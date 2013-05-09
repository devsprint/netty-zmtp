/*
 * Copyright (c) 2012-2013 Spotify AB
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.spotify.netty.handler.codec.zmtp;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.handler.codec.frame.FrameDecoder;

import static com.spotify.netty.handler.codec.zmtp.ZMTPUtils.FINAL_FLAG;

/**
 * Netty FrameDecoder for zmtp protocol
 * 
 * Decodes ZMTP frames into a ZMTPMessage - will return a ZMTPMessage as a
 * message event
 */
public class ZMTPFramingDecoder extends FrameDecoder {

	private final ZMTPMessageParser parser;
	private final AbstractZMTPSession session;
	private ChannelFuture handshakeFuture;

	/**
	 * Creates a new decoder
	 */
	public ZMTPFramingDecoder(final AbstractZMTPSession session) {
		this.session = session;
		this.parser = new ZMTPMessageParser(session.isEnveloped());
	}

	/**
	 * Sends my local identity
	 */
	private void sendIdentity(final Channel channel) {

		if (session.getRevision() == ZMTPRevision.ZMTP_10) {
			final ChannelBuffer msg;
			if (session.useLocalIdentity()) {
				// send session current identity
				msg = ChannelBuffers.dynamicBuffer(2 + session.getLocalIdentity().length);

				ZMTPUtils.encodeLength(1 + session.getLocalIdentity().length, msg);
				msg.writeByte(FINAL_FLAG);
				msg.writeBytes(session.getLocalIdentity());
			} else {
				msg = ChannelBuffers.dynamicBuffer(2);
				// Anonymous identity
				msg.writeByte(1);
				msg.writeByte(FINAL_FLAG);
			}
			// Send identity message
			channel.write(msg);
		}

		if (session.getRevision() == ZMTPRevision.ZMTP_20) {
			sendGreeting(channel);
		}

	}

	/**
	 * Parses the remote zmtp identity received
	 */
	private boolean handleRemoteIdentity(final ChannelBuffer buffer) {
		if (session.getRevision() == ZMTPRevision.ZMTP_10) {

			buffer.markReaderIndex();

			final long len = ZMTPUtils.decodeLength(buffer);

			// Bail out if there's not enough data
			if (len == -1 || buffer.readableBytes() < len) {
				buffer.resetReaderIndex();
				return false;
			}

			final int flags = buffer.readByte();

			// More flag should not be set (TODO: is this true?)
			if ((flags & ZMTPUtils.MORE_FLAG) == ZMTPUtils.MORE_FLAG) {
				handshakeFuture.setFailure(new ZMTPException(
						"Expected identity from remote side but got a frame with MORE flag set."));
			}

			if (len == 1) {
				// Anonymous identity
				session.setRemoteIdentity(null);
			} else {
				// Read identity from remote
				final byte[] identity = new byte[(int) len - 1];
				buffer.readBytes(identity);

				// Anonymous identity
				session.setRemoteIdentity(identity);
			}

			handshakeFuture.setSuccess();
		}

		if (session.getRevision() == ZMTPRevision.ZMTP_20) {
			// check signature, revision, socket-type
			buffer.markReaderIndex();
			// Bail out if there's not enough data
			if (buffer.readableBytes() < 1) {
				buffer.resetReaderIndex();
				return false;
			}

			if (isSignatureValid(buffer)) {
				final byte revision = buffer.readByte();
				if (revision != ZMTPRevision.ZMTP_20.getRevision()) {
					// TODO: revision is not the same - should we handle it?
					buffer.resetReaderIndex();
					return false;
				}
				ZMTPSession2 zmtp2 = (ZMTPSession2) session;
				if (canAcceptConnection(buffer, zmtp2.getConnectionType())) {
					final long len = ZMTPUtils.decodeLength(buffer);
					// Read identity from remote
					final byte[] identity = new byte[(int) len - 1];
					buffer.readBytes(identity);

					// Anonymous identity
					session.setRemoteIdentity(identity);
					handshakeFuture.setSuccess();
				} else {
					buffer.resetReaderIndex();
					return false;
				}
			}

		}
		return true;
	}

	/**
	 * Resposible for decoding incomming data to zmtp frames
	 */
	@Override
	protected Object decode(final ChannelHandlerContext ctx, final Channel channel, final ChannelBuffer buffer)
			throws Exception {
		if (buffer.readableBytes() < 2) {
			return null;
		}

		if (session.getRemoteIdentity() == null) {
			// Should be first packet received from host
			if (!handleRemoteIdentity(buffer)) {
				return null;
			}
		}

		// Parse incoming frames
		final ZMTPMessage message = parser.parse(buffer);
		if (message == null) {
			return null;
		}

		return new ZMTPIncomingMessage(session, message);
	}

	@Override
	public void channelConnected(final ChannelHandlerContext ctx, final ChannelStateEvent e) throws Exception {
		// Store channel in the session
		this.session.setChannel(e.getChannel());

		handshake(ctx.getChannel()).addListener(new ChannelFutureListener() {
			@Override
			public void operationComplete(final ChannelFuture future) throws Exception {
				ctx.sendUpstream(e);
			}
		});
	}

	private ChannelFuture handshake(final Channel channel) {
		handshakeFuture = Channels.future(channel);

		// Send our identity
		sendIdentity(channel);

		return handshakeFuture;
	}

	/**
	 * Sends the greeting message.
	 * 
	 * @param channel
	 *            - current channel
	 */
	private void sendGreeting(Channel channel) {
		final ChannelBuffer msg;
		// TODO: it is still possible to use anonymous?
		if (session.useLocalIdentity()) {
			// send session current greeting
			msg = ChannelBuffers.dynamicBuffer(14 + session.getLocalIdentity().length);

			msg.writeByte(0xFF);
			msg.writeBytes(ZMTPUtils.ZMTP_20_SIGNATURE);
			msg.writeByte(session.getRevision().getRevision());
			ZMTPSession2 session2 = (ZMTPSession2) session;
			msg.writeByte(session2.getConnectionType().getEncodedValue());
			ZMTPUtils.encodeLength(1 + session.getLocalIdentity().length, msg);
			msg.writeByte(FINAL_FLAG);
			msg.writeBytes(session.getLocalIdentity());
		} else {
			msg = ChannelBuffers.dynamicBuffer(2);
			// Anonymous identity
			msg.writeByte(1);
			msg.writeByte(FINAL_FLAG);
		}
		// Send identity message
		channel.write(msg);

	}

	/**
	 * Is signature of incoming message valid?
	 * 
	 * @param buffer
	 *            - received buffer
	 * @return true if signature is valid, false otherwise
	 */
	private boolean isSignatureValid(final ChannelBuffer buffer) {
		final int firstByteOfSignature = buffer.readByte();
		if (firstByteOfSignature != 0xFF) {
			buffer.resetReaderIndex();
			return false;
		}
		final long zeros = buffer.readLong();
		if (zeros != 0L) {
			buffer.resetReaderIndex();
			return false;
		}
		final byte signatureLastByte = buffer.readByte();
		if (signatureLastByte != 0x7F) {
			buffer.resetReaderIndex();
			return false;
		}
		return true;
	}

	/**
	 * Check the socket type for the remote host.
	 * 
	 * @param buffer
	 *            - received buffer
	 * @param connectionType
	 *            - local socket type
	 * @return true if the remote socket type can work with local socket type,
	 *         false otherwise.
	 * 
	 * 
	 *         NOTE: XPUB, XSUB is implemented as PUB,SUB
	 */
	private boolean canAcceptConnection(ChannelBuffer buffer, ZMTPSocketType connectionType) {
		final byte remoteSocketType = buffer.readByte();
		ZMTPSocketType remoteSocket = ZMTPSocketType.fromByte(remoteSocketType);
		// PAIR socket accept connections only from PAIR sockets
		if (connectionType == ZMTPSocketType.PAIR && remoteSocket == ZMTPSocketType.PAIR) {
			return true;
		}
		// PUB socket accepts connection only from a SUB socket
		if (connectionType == ZMTPSocketType.PUB && remoteSocket == ZMTPSocketType.SUB) {
			return true;
		}
		// SUB sockets accept connections only from a PUB socket
		if (connectionType == ZMTPSocketType.SUB && remoteSocket == ZMTPSocketType.PUB) {
			return true;
		}
		// REQ sockets accept connections from a REP or ROUTER socket
		if (connectionType == ZMTPSocketType.REQ
				&& (remoteSocket == ZMTPSocketType.REP || remoteSocket == ZMTPSocketType.ROUTER)) {
			return true;
		}
		// REP sockets accept connections from a REQ or DEALER socket
		if (connectionType == ZMTPSocketType.REP
				&& (remoteSocket == ZMTPSocketType.REQ || remoteSocket == ZMTPSocketType.DEALER)) {
			return true;
		}

		// DEALER sockets accept connections from a REP, DEALER or ROUTER socket
		if (connectionType == ZMTPSocketType.DEALER
				&& (remoteSocket == ZMTPSocketType.REP || remoteSocket == ZMTPSocketType.DEALER || remoteSocket == ZMTPSocketType.ROUTER)) {
			return true;
		}

		// ROUTER sockets accept connections from a REQ, DEALER or ROUTER socket
		if (connectionType == ZMTPSocketType.ROUTER
				&& (remoteSocket == ZMTPSocketType.REQ || remoteSocket == ZMTPSocketType.DEALER || remoteSocket == ZMTPSocketType.ROUTER)) {
			return true;
		}
		// PUSH sockets accept connections only from a PULL socket
		if (connectionType == ZMTPSocketType.PUSH && remoteSocket == ZMTPSocketType.PULL) {
			return true;
		}

		// PULL sockets accept connections only from a PUSH socket
		if (connectionType == ZMTPSocketType.PULL && remoteSocket == ZMTPSocketType.PUSH) {
			return true;
		}

		return false;
	}
}
