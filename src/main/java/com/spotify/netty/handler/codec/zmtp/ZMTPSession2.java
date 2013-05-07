package com.spotify.netty.handler.codec.zmtp;

import java.net.SocketAddress;
import java.util.UUID;

import org.jboss.netty.channel.Channel;
import org.jetbrains.annotations.Nullable;

public class ZMTPSession2 {
	  private final boolean useLocalIdentity;
	  private final byte[] localIdent;

	  private ZMTPSocketType type;
	  private Channel channel;
	  private byte[] remoteIdent;

	  public ZMTPSession2(final ZMTPSocketType type) {
	    this(type, null);
	  }

	  public ZMTPSession2(final ZMTPSocketType type, @Nullable final byte[] localIdent) {
	    this.type = type;
	    this.useLocalIdentity = (localIdent != null);
	    if (localIdent == null) {
	      this.localIdent = ZMTPUtils.getBytesFromUUID(UUID.randomUUID());
	    } else {
	      this.localIdent = localIdent;
	    }
	  }

	  /**
	   * @return The local address of the session
	   */
	  public SocketAddress getLocalAddress() {
	    return channel.getLocalAddress();
	  }

	  /**
	   * @return The remote address of the session
	   */
	  public SocketAddress getRemoteAddress() {
	    return channel.getRemoteAddress();
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
	   * Changes the type of connection of the session
	   */
	  void setConnectionType(final ZMTPSocketType type) {
	    this.type = type;
	  }

	  /**
	   * Helper to determine if messages in this session are enveloped
	   */
	  public boolean isEnveloped() {
	    return (type == ZMTPSocketType.REQ);
	  }

	  /**
	   * Get the remote session id (can be used for persistent queuing)
	   */
	  public byte[] getRemoteIdentity() {
	    return remoteIdent;
	  }

	  /**
	   * Return the local identity
	   */
	  public byte[] getLocalIdentity() {
	    return localIdent;
	  }

	  /**
	   * Do we have a local identity or does the system create one
	   */
	  public boolean useLocalIdentity() {
	    return useLocalIdentity;
	  }

	  /**
	   * Set the remote identity
	   *
	   * @param remoteIdent Remote identity, if null an identity will be created
	   */
	  public void setRemoteIdentity(@Nullable final byte[] remoteIdent) throws ZMTPException {
	    if (this.remoteIdent != null) {
	      throw new ZMTPException("Remote identity already set");
	    }

	    this.remoteIdent = remoteIdent;
	    if (this.remoteIdent == null) {
	      // Create a new remote identity
	      this.remoteIdent = ZMTPUtils.getBytesFromUUID(UUID.randomUUID());
	    }
	  }

	  public Channel getChannel() {
	    return channel;
	  }

	  public void setChannel(final Channel channel) {
	    this.channel = channel;
	  }

}
