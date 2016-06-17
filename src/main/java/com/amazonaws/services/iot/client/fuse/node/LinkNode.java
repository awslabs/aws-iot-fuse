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

import net.fusejna.DirectoryFiller;
import net.fusejna.ErrorCodes;
import net.fusejna.StructFuseFileInfo.FileInfoWrapper;
import net.fusejna.StructStat.StatWrapper;
import net.fusejna.types.TypeMode.ModeWrapper;
import net.fusejna.types.TypeMode.NodeType;

public class LinkNode extends Node {

	private final Node source;
	
	public LinkNode(Node parent, String name, Node target) {
		super(parent, name, false);
		
		this.source = target;
	}

	public Node getSource() {
		return source;
	}

	@Override
	public int getAttr(StatWrapper stat) {
		int r = super.getAttr(stat);
		stat.setMode(NodeType.SYMBOLIC_LINK);
		return r;
	}

    @Override
	public int readlink(ByteBuffer buffer, long size) {
        String path;
        if (this.parent == source.parent) {
            path = source.name;
        } else {
            path = this.getRelativePathToRoot() + source.getAbsolutePath();
        }
        byte[] bytes = path.getBytes();
        if (size < bytes.length) {
            return -ErrorCodes.ENOSPC();
        }
        
        buffer.put(bytes);
        return 0;
    }
    
    @Override
    public int unlink(String path) {
        int r = parent.symunlink(name);
        if (r < 0) {
            return r;
        }
        return super.unlink(path);
    }
    
	@Override
	public Node find(String path) {
		return source.find(path);
	}
	
	@Override
	public int readdir(DirectoryFiller filler) {
		return source.readdir(filler);
	}
	
	@Override
	public int read(String path, ByteBuffer buffer, long size, long offset, FileInfoWrapper info) {
		return source.read(path, buffer, size, offset, info);
	}

	@Override
	public int open(String path, FileInfoWrapper info) {
		return source.open(path, info);
	}

	@Override
	public int create(String name, ModeWrapper mode, FileInfoWrapper info) {
		return source.create(name, mode, info);
	}

	public int write(String path, ByteBuffer buf, long bufSize, long offset, FileInfoWrapper info) {
		return source.write(path, buf, bufSize, offset, info);
	}
	
	public int release(String path, FileInfoWrapper info) {
		return source.release(path, info);
	}
	
}