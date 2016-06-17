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

import java.util.List;

import net.fusejna.types.TypeMode.ModeWrapper;

import com.amazonaws.services.iot.client.fuse.node.Node;

public class ThingsNode extends Node {

    private static final String NODE_NAME = "things";

    public ThingsNode(Node parent) {
        super(parent, NODE_NAME, true);
        setReInitDelay(30000);
    }

    @Override
    public void init() {
        synchronized (this) {
            List<ThingNode> thingNodes = iotClient.getThings(this);
            updateChildren(thingNodes);

            super.init();
        }
    }

    @Override
    public int mkdir(String name, ModeWrapper mode) {
        ThingNode node = iotClient.createThing(this, name);
        addChildren(node);

        return 0;
    }

}
