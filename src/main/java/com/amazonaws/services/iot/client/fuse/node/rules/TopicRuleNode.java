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

package com.amazonaws.services.iot.client.fuse.node.rules;

import net.fusejna.ErrorCodes;

import com.amazonaws.services.iot.client.fuse.node.InfoNode;
import com.amazonaws.services.iot.client.fuse.node.Node;
import com.amazonaws.services.iot.model.TopicRuleListItem;

public class TopicRuleNode extends Node {

    private final String ruleArn;
    private String rulePattern;
    private String status;

    public TopicRuleNode(Node parent, TopicRuleListItem rule) {
        super(parent, rule.getRuleName(), true);

        this.ruleArn = rule.getRuleArn();
        this.rulePattern = rule.getTopicPattern();
        this.creationDate = rule.getCreatedAt();
        this.status = rule.isRuleDisabled() ? "INACTIVE" : "ACTIVE";

        addChildren(new InfoNode(this, "arn", ruleArn));
        addChildren(new InfoNode(this, "status", status));
        addChildren(new InfoNode(this, "rule-pattern", rulePattern));
        addChildren(new TopicRuleSqlNode(this));
        addChildren(new TopicRuleActionsNode(this));
    }

    @Override
    public int rmdir(String path) {
        if (!children.isEmpty()) {
            return -ErrorCodes.ENOTEMPTY();
        }

        int r = iotClient.deleteTopicRule(name);
        if (r != 0) {
            return r;
        }

        return super.rmdir(path);
    }

}
