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

import net.fusejna.ErrorCodes;
import net.fusejna.StructFuseFileInfo.FileInfoWrapper;

public class DocumentNode extends Node {

    private static final int MAX_DOCUMENT_SIZE = 64 * 1024;

    private int maxDocumentSize = MAX_DOCUMENT_SIZE;
    private String document = "";
    private boolean isDirty;

    public DocumentNode(Node parent, String name) {
        super(parent, name, false);
    }

    public void setMaxDocumentSize(int maxSize) {
        maxDocumentSize = maxSize;
    }

    public String getDocument() throws Exception {
        return document;
    }

    public void setDocument(String document) throws Exception {
        this.document = document;
    }

    @Override
    public synchronized void init() {
        try {
            document = getDocument();
        } catch (Exception e) {
        }
        if (document == null) {
            document = "";
        }
        size = document.length();

        super.init();
    }

    @Override
    public synchronized int read(String path, ByteBuffer buf, long bufSize, long offset, FileInfoWrapper info) {
        try {
            document = getDocument();
        } catch (Exception e) {
            return -ErrorCodes.EIO();
        }
        size = document.length();

        if (offset < 0) {
            offset = 0;
        }
        if (offset >= size) {
            return 0;
        }
        if (bufSize < 0) {
            bufSize = size;
        }
        if (offset + bufSize > size) {
            bufSize = size - offset;
        }

        buf.put(document.getBytes(), (int) offset, (int) bufSize);
        return (int) bufSize;
    }

    @Override
    public synchronized int write(String path, ByteBuffer buf, long bufSize, long offset, FileInfoWrapper info) {
        if (bufSize <= 0) {
            return 0;
        }
        if (offset < 0) {
            return 0;
        }
        if (offset + bufSize > maxDocumentSize) {
            return -ErrorCodes.ENOSPC();
        }

        StringBuffer strBuffer = new StringBuffer();
        if (offset > 0) {
            if (offset <= document.length()) {
                strBuffer.append(document.substring(0, (int) offset));
            } else {
                strBuffer.append(document);
                long len = document.length();
                while (len < offset) {
                    strBuffer.append(" ");
                    len++;
                }
            }
        }

        byte[] data = new byte[(int) bufSize];
        buf.get(data);
        strBuffer.append(new String(data));

        if (offset + bufSize < document.length()) {
            strBuffer.append(document.substring((int) (offset + bufSize)));
        }

        document = strBuffer.toString();
        size = document.length();
        isDirty = true;

        return (int) bufSize;
    }

    @Override
    public synchronized int release(String path, FileInfoWrapper info) {
        try {
            if (isDirty) {
                try {
                    setDocument(document);
                } catch (Exception e) {
                    return -ErrorCodes.EIO();
                }
            }
        } finally {
            isDirty = false;
        }
        return 0;
    }

    @Override
    public synchronized int truncate(String path, long offset) {
        if (size <= 0 || offset >= size) {
            return 0;
        }

        document = document.substring(0, (int) offset);
        size = offset;
        isDirty = true;

        return 0;
    }

}
