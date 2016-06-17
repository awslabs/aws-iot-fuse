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

package com.amazonaws.services.iot.client.fuse.node.rules.actions;

import com.amazonaws.services.iot.client.fuse.node.InfoNode;
import com.amazonaws.services.iot.client.fuse.node.Node;
import com.amazonaws.services.iot.client.fuse.node.rules.TopicRuleActionNode;
import com.amazonaws.services.iot.model.ElasticsearchAction;

public class ElasticsearchActionNode extends TopicRuleActionNode {

    public ElasticsearchActionNode(Node parent, String name, ElasticsearchAction action) {
        super(parent, name);

        addChildren(new InfoNode(this, "endpoint", action.getEndpoint()));
        addChildren(new InfoNode(this, "id", action.getId()));
        addChildren(new InfoNode(this, "index", action.getIndex()));
        addChildren(new InfoNode(this, "role-arn", action.getRoleArn()));
        addChildren(new InfoNode(this, "type", action.getType()));
    }

}
