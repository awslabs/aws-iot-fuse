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

public class InfoNode extends Node {

    private String info;

    public InfoNode(Node parent, String name) {
        this(parent, name, null);
    }

    public InfoNode(Node parent, String name, String info) {
        super(parent, name, false);
        setInfo(info);
    }

    public void setInfo(String info) {
        this.info = (info == null) ? "" : info;
        this.size = this.info.length();
    }

    @Override
    public int read(final String path, final ByteBuffer buf, final long bufSize, final long offset,
            final FileInfoWrapper infoWrapper) {
        final String str = info.substring((int) offset,
                (int) Math.max(offset, Math.min(info.length() - offset, offset + size)));
        buf.put(str.getBytes());
        return str.getBytes().length;
    }

}
