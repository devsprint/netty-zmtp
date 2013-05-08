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

import org.jetbrains.annotations.Nullable;

/**
 * Represents a ongoing zmtp session
 */
public class ZMTPSession extends AbstractZMTPSession {

	private final ZMTPConnectionType type;

	public ZMTPSession(final ZMTPConnectionType type) {
		this(type, null);
	}

	public ZMTPSession(final ZMTPConnectionType type, @Nullable final byte[] localIdent) {
		super(localIdent);
		this.type = type;
	}

	/**
	 * Type of connection dictates if a identity frame is needed
	 * 
	 * @return Returns the type of connection
	 */
	public ZMTPConnectionType getConnectionType() {
		return type;
	}

	@Override
	public boolean isEnveloped() {
		return (type == ZMTPConnectionType.Addressed);

	}

	@Override
	public ZMTPRevision getRevision() {
		return ZMTPRevision.ZMTP_10;
	}
}
