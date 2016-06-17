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

import net.fusejna.ErrorCodes;

import com.amazonaws.services.iot.client.fuse.node.Node;
import com.amazonaws.services.iot.client.fuse.node.certificates.CertificateNode;

public class PrincipalsNode extends Node {

    private static final String NODE_NAME = "principals";
    private static final String CERTIFICATES_PATH = "/certificates";

    private final String thingName;

    public PrincipalsNode(Node parent, String thingName) {
        super(parent, NODE_NAME, true);

        this.thingName = thingName;
    }

    @Override
    public void init() {
        synchronized (this) {
            List<String> principals = iotClient.getPrincipals(this);
            for (String principal : principals) {
                // extract Id from ARN
                principal = principal.substring(principal.lastIndexOf("/") + 1);

                Node node = root.find(CERTIFICATES_PATH + "/" + principal);
                if (node == null) {
                    continue;
                }
                link(principal, node);
            }

            super.init();
        }
    }

    @Override
    public int symlink(String name, String path) {
        Node sourceNode = this.find(path);
        if (sourceNode instanceof CertificateNode) {
            CertificateNode certificateNode = (CertificateNode) sourceNode;
            int r = iotClient.attachCertificate(certificateNode.getCertificateArn(), thingName);
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
        if (sourceNode instanceof CertificateNode) {
            CertificateNode certificateNode = (CertificateNode) sourceNode;
            int r = iotClient.detachCertificate(certificateNode.getCertificateArn(), thingName);
            if (r != 0) {
                return r;
            }
        } else {
            return -ErrorCodes.ENODEV();
        }

        return 0;
    }

}
