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

package com.amazonaws.services.iot.client.fuse.node.topics;

import java.util.Set;

import com.amazonaws.services.iot.client.fuse.node.Node;

public class TopicsNode extends Node {

    private static final String NODE_NAME = "topics";
    private static final String MESSAGES_NODE_NAME = "messages";

    public TopicsNode(Node parent, Set<String> topics) {
        super(parent, NODE_NAME, true);

        if (topics != null) {
            for (String topic : topics) {
                TopicNode node = new TopicNode(this, topic);

                iotClient.subscribe(node.getChild(MESSAGES_NODE_NAME), topic);

                addChildren(node);
            }
        }
    }

}
