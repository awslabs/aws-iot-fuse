/*
 * Copyright 2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.amazonaws.services.iot.client.fuse.node.topics;

import java.nio.ByteBuffer;

import com.amazonaws.services.iot.client.fuse.node.Node;

import net.fusejna.StructFuseFileInfo.FileInfoWrapper;

public class MessageNode extends Node {

	private final byte[] payload;

	public MessageNode(Node parent, String name, byte[] payload) {
		super(parent, name, false);

		this.payload = payload;
		this.size = payload.length;
	}

	@Override
	public int read(String path, ByteBuffer buf, long bufSize, long offset, FileInfoWrapper info) {
		if (offset < 0) {
			offset = 0;
		}
		if (offset >= payload.length) {
			return 0;
		}
		if (bufSize < 0) {
			bufSize = payload.length;
		}
		if (offset + bufSize > payload.length) {
			bufSize = payload.length - offset;
		}

		buf.put(payload, (int) offset, (int) bufSize);
		return (int) bufSize;
	}

}
