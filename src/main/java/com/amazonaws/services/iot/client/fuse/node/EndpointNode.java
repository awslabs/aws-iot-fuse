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

package com.amazonaws.services.iot.client.fuse.node;

import java.nio.ByteBuffer;

import net.fusejna.StructFuseFileInfo.FileInfoWrapper;

public class EndpointNode extends Node {

    private String endpoint;

    public EndpointNode(Node parent) {
        super(parent, "endpoint", false);
    }

    @Override
    public void init() {
        synchronized (this) {
            endpoint = iotClient.getEndpoint();
            size = endpoint.length();

            super.init();
        }
    }

    @Override
    public int read(String path, ByteBuffer buf, long bufSize, long offset, FileInfoWrapper info) {
        final String str = endpoint.substring((int) offset,
                (int) Math.max(offset, Math.min(endpoint.length() - offset, offset + bufSize)));
        buf.put(str.getBytes());
        return str.getBytes().length;
    }

}
