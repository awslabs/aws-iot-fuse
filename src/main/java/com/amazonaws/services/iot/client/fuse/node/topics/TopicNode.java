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

import com.amazonaws.services.iot.client.fuse.node.Node;

public class TopicNode extends Node {

    private final String topic;

    public TopicNode(Node parent, String name) {
        super(parent, sanitize(name), true);

        this.topic = name;

        addChildren(new PublishNode(this));
        addChildren(new MessagesNode(this));
    }

    public String getTopic() {
        return topic;
    }

    private static String sanitize(String name) {
        return name.replace('/', '_');
    }

}
