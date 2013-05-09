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
			(byte) 0x06), PULL((byte) 0x07), PUSH((byte) 0x08), UNKNOWN((byte) (0xFF));

	private final byte encoding;

	private ZMTPSocketType(byte encoding) {
		this.encoding = encoding;
	}

	public byte getEncodedValue() {
		return this.encoding;
	}

	public static ZMTPSocketType fromByte(final byte value) {
		switch (value) {
		case 0x00:
			return PAIR;
		case 0x01:
			return PUB;
		case 0x02:
			return SUB;
		case 0x03:
			return REQ;
		case 0x04:
			return REP;
		case 0x05:
			return DEALER;
		case 0x06:
			return ROUTER;
		case 0x07:
			return PULL;
		case 0x08:
			return PUSH;
		default:
			return UNKNOWN;
		}
	}
}
