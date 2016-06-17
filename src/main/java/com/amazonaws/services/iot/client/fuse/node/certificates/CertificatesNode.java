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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import net.fusejna.types.TypeMode.ModeWrapper;

import com.amazonaws.services.iot.client.fuse.node.Node;

public class CertificatesNode extends Node {

    private static final String NODE_NAME = "certificates";

    public CertificatesNode(Node parent) {
        super(parent, NODE_NAME, true);
        setReInitDelay(30000);
    }

    public void updateCertificateChildren(List<CertificateNode> nodes) {
        HashMap<String, String> aliases = new HashMap<>();
        for (Entry<String, Node> entry : children.entrySet()) {
            String alias = entry.getKey();
            CertificateNode node = (CertificateNode) entry.getValue();
            if (!alias.equals(node.getCertificateId())) {
                aliases.put(node.getCertificateId(), alias);
            }
        }

        Set<String> namesSet = new HashSet<>();
        for (Node node : nodes) {
            String nodeName = node.getName();
            if (aliases.containsKey(nodeName)) {
                nodeName = aliases.get(nodeName);
            }
            children.putIfAbsent(nodeName, node);
            namesSet.add(nodeName);
        }
        children.keySet().retainAll(namesSet);
    }

    @Override
    public void init() {
        synchronized (this) {
            List<CertificateNode> certificateNodes = iotClient.getCertificates(this);
            updateCertificateChildren(certificateNodes);

            super.init();
        }
    }

    @Override
    public int mkdir(String name, ModeWrapper mode) {
        Node node = iotClient.createCertificate(this, name);
        addChildren(node);
        return 0;
    }

}
