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

package com.amazonaws.services.iot.client.fuse.node.certificates;

import java.util.List;

import net.fusejna.ErrorCodes;

import com.amazonaws.services.iot.client.fuse.node.Node;
import com.amazonaws.services.iot.client.fuse.node.policies.PolicyNode;

public class CertificatePoliciesNode extends Node {

    private static final String NODE_NAME = "policies";
    private static final String POLICIES_DIR = "/policies/";

    private final String certificateArn;

    public CertificatePoliciesNode(Node parent, String certificateArn) {
        super(parent, NODE_NAME, true);

        this.certificateArn = certificateArn;
    }

    @Override
    public void init() {
        synchronized (this) {
            List<String> policies = iotClient.getCertificatePolicies(certificateArn);
            for (String policy : policies) {
                Node node = root.find(POLICIES_DIR + policy);
                if (node == null) {
                    continue;
                }
                link(policy, node);
            }

            super.init();
        }
    }

    @Override
    public int symlink(String name, String path) {
        Node sourceNode = this.find(path);
        if (sourceNode instanceof PolicyNode) {
            int r = iotClient.attachPolicy(certificateArn, sourceNode.getName());
            if (r != 0) {
                return r;
            }
        } else {
            return -ErrorCodes.ENODEV();
        }

        return link(name, sourceNode);
    }

    @Override
    public int symunlink(String name) {
        Node node = getChild(name);
        if (node == null) {
            return -ErrorCodes.ENODEV();
        }

        Node sourceNode = this.find(name);
        if (sourceNode instanceof PolicyNode) {
            int r = iotClient.detachPolicy(certificateArn, sourceNode.getName());
            if (r != 0) {
                return r;
            }
        } else {
            return -ErrorCodes.ENODEV();
        }

        return 0;
    }

}
