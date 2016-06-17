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

package com.amazonaws.services.iot.client.fuse.node.things;

import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import net.fusejna.StructFuseFileInfo.FileInfoWrapper;

import com.amazonaws.services.iot.client.AWSIotDevice;
import com.amazonaws.services.iot.client.AWSIotDeviceErrorCode;
import com.amazonaws.services.iot.client.AWSIotException;
import com.amazonaws.services.iot.client.AWSIotTimeoutException;
import com.amazonaws.services.iot.client.fuse.node.DocumentNode;
import com.amazonaws.services.iot.client.fuse.node.Node;

public class StateNode extends DocumentNode {

    private static final Logger LOGGER = Logger.getLogger(StateNode.class.getName());

    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private static final String NODE_NAME = "state";
    private static final int DEFAULT_BUFFER_SIZE = 1024;
    private static final int MAX_BUFFER_SIZE = 8 * 1024;

    private final AWSIotDevice iotDevice;

    private Future<?> timeoutTask;
    private boolean deviceAttached;

    public StateNode(Node parent) {
        super(parent, NODE_NAME);
        setMaxDocumentSize(MAX_BUFFER_SIZE);

        this.iotDevice = new AWSIotDevice(parent.getName());
    }

    @Override
    public synchronized void init() {
        // size is unknown, set an arbitrary size so 'cat' doesn't exist
        size = DEFAULT_BUFFER_SIZE;
        initDone = true;
    }

    @Override
    public synchronized int open(String path, FileInfoWrapper info) {
        if (!deviceAttached) {
            LOGGER.info("Attaching device " + path);
            int r = iotClient.attachDevice(iotDevice);
            if (r != 0) {
                return r;
            }
            deviceAttached = true;
        }
        return 0;
    }

    private void scheduleDeviceDetach(final String path) {
        if (timeoutTask != null) {
            timeoutTask.cancel(false);
        }

        timeoutTask = scheduler.schedule(new Runnable() {
            @Override
            public void run() {
                LOGGER.info("Detaching device " + path);
                int r = iotClient.detachDevice(iotDevice);
                if (r != 0) {
                    LOGGER.warning("Close: detach device failure");
                }
                deviceAttached = false;
            }
        }, 30, TimeUnit.SECONDS);
    }

    @Override
    public synchronized int release(String path, FileInfoWrapper info) {
        try {
            return super.release(path, info);
        } finally {
            scheduleDeviceDetach(path);
        }
    }

    @Override
    public String getDocument() throws Exception {
        try {
            return iotDevice.get(5000);
        } catch (AWSIotException e) {
            if (AWSIotDeviceErrorCode.NOT_FOUND == e.getErrorCode()) {
                return "";
            }
            
            LOGGER.warning("Read: read device failure");
            throw new Exception(e);
        } catch (AWSIotTimeoutException e) {
            LOGGER.warning("Read: read device timeout");
            throw new Exception(e);
        }
    }

    @Override
    public void setDocument(String document) throws Exception {
        try {
            iotDevice.update(document, 5000);
        } catch (AWSIotException e) {
            LOGGER.warning("Close: update device failure");
            throw new Exception(e);
        } catch (AWSIotTimeoutException e) {
            LOGGER.warning("Close: update device timeout");
            throw new Exception(e);
        }
    }

}
