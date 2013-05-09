package com.spotify.netty.handler.codec.zmtp;

import org.jetbrains.annotations.Nullable;

/**
 * ZMTP/2.0 session.
 * @author gciuloaica
 *
 */
public class ZMTPSession2 extends AbstractZMTPSession {

	private final ZMTPSocketType type;

	public ZMTPSession2(final ZMTPSocketType type) {
		this(type, null);
	}

	public ZMTPSession2(final ZMTPSocketType type, @Nullable final byte[] localIdent) {
		super(localIdent);
		this.type = type;

	}

	/**
	 * Type of connection dictates if a identity frame is needed
	 * 
	 * @return Returns the type of connection
	 */
	public ZMTPSocketType getConnectionType() {
		return type;
	}

	/**
	 * Helper to determine if messages in this session are enveloped
	 */
	public boolean isEnveloped() {
		return (type == ZMTPSocketType.REQ);
	}

	@Override
	public ZMTPRevision getRevision() {
		return ZMTPRevision.ZMTP_20;
	}

}
