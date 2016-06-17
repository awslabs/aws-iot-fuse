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

package com.amazonaws.services.iot.client.fuse.node.policies;

import java.util.List;

import net.fusejna.ErrorCodes;

import com.amazonaws.services.iot.client.fuse.node.Node;

public class PolicyVersionsNode extends Node {

    private static final String DEFAULT_NODE_NAME = "default";

    public PolicyVersionsNode(Node parent) {
        super(parent, "versions", true);
    }

    @Override
    public void init() {
        List<Node> versionNodes = iotClient.getPolicyVersions(this, parent.getName());
        addChildren(versionNodes);

        super.init();
    }

    @Override
    public int symlink(String name, String path) {
        Node sourceNode = this.find(path);
        if (sourceNode instanceof PolicyDocumentNode && name.equals(DEFAULT_NODE_NAME)
                && sourceNode.getParent() == this) {
            int r = iotClient.setDefaultPolicyVersion(parent.getName(), sourceNode.getName());
            if (r != 0) {
                return r;
            }
        } else {
            return -ErrorCodes.ENODEV();
        }

        return link(name, sourceNode);
    }

}
