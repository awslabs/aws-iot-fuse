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

import net.fusejna.ErrorCodes;
import net.fusejna.StructFuseFileInfo.FileInfoWrapper;

public class PublishNode extends Node {

    private static final String NODE_NAME = "publish";
    private static final int DEFAULT_BUFFER_SIZE = 1024;
    private static final int MAX_BUFFER_SIZE = 128 * 1024;

    private ByteBuffer buffer;
    private byte[] payload;

    public PublishNode(Node parent) {
        super(parent, NODE_NAME, false);

        buffer = ByteBuffer.allocate(DEFAULT_BUFFER_SIZE);
    }

    @Override
    public synchronized int read(String path, ByteBuffer buf, long bufSize, long offset, FileInfoWrapper info) {
        if (offset < 0) {
            offset = 0;
        }
        if (payload == null || size <= 0 || offset >= size) {
            return 0;
        }
        if (bufSize < 0) {
            bufSize = size;
        }
        if (offset + bufSize > size) {
            bufSize = size - offset;
        }

        buf.put(payload, (int) offset, (int) bufSize);
        return (int) bufSize;
    }

    @Override
    public synchronized int truncate(String path, long offset) {
        if (payload == null || size <= 0 || offset >= size) {
            return 0;
        }

        size = offset;
        return 0;
    }

    @Override
    public synchronized int write(String path, ByteBuffer buf, long bufSize, long offset, FileInfoWrapper info) {
        if (bufSize <= 0) {
            return 0;
        }
        if (offset < 0) {
            return 0;
        }
        if (offset + bufSize > MAX_BUFFER_SIZE) {
            return -ErrorCodes.ENOSPC();
        }

        if (offset + bufSize > buffer.capacity()) {
            long newSize = offset + bufSize + DEFAULT_BUFFER_SIZE;
            ByteBuffer newBuffer = ByteBuffer.allocate((int) newSize);
            buffer.flip();
            newBuffer.put(buffer);
            buffer = newBuffer;
        }

        byte[] data = new byte[(int) bufSize];
        buf.get(data);

        int pos = buffer.position();
        buffer.position((int) offset);
        buffer.put(data);

        if (pos > buffer.position()) {
            buffer.position(pos);
        }

        return (int) bufSize;
    }

    @Override
    public synchronized int release(String path, FileInfoWrapper info) {
        publishData();
        return 0;
    }

    private void publishData() {
        TopicNode topicNode = (TopicNode) parent;

        buffer.flip();
        if (buffer.hasRemaining()) {
            size = buffer.remaining();
            payload = new byte[(int) size];

            buffer.get(payload);
            iotClient.publish(topicNode.getTopic(), payload);
        }
        buffer.clear();
    }
}
