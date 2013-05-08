/**
 * 
 */
package com.spotify.netty.handler.codec.zmtp;

/**
 * Zeromq Socket type.
 * 
 * @author gciuloaica
 * 
 */
public enum ZMTPSocketType {
	PAIR((byte) 0x00), PUB((byte) 0x01), SUB((byte) 0x02), REQ((byte) 0x03), REP((byte) 0x04), DEALER((byte) 0x05), ROUTER(
			(byte) 0x06), PULL((byte) 0x07), PUSH((byte) 0x08);

	private final byte encoding;

	private ZMTPSocketType(byte encoding) {
		this.encoding = encoding;
	}

	public byte getEncodedValue() {
		return this.encoding;
	}
}
