package com.spotify.netty.handler.codec.zmtp;

/**
 * ZMTP protocol revision number.
 * 
 * @author gciuloaica
 * 
 */
public enum ZMTPRevision {
	ZMTP_10((byte) 0x00), ZMTP_20((byte) 0x01), ZMTP_30((byte) 0x02);

	private final byte revision;

	private ZMTPRevision(final byte revision) {
		this.revision = revision;
	}

	public byte getRevision() {
		return revision;
	}
}
