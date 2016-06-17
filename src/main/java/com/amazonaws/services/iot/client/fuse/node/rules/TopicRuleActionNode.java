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

import com.amazonaws.services.iot.client.fuse.node.Node;
import com.amazonaws.services.iot.client.fuse.node.rules.actions.CloudwatchAlarmActionNode;
import com.amazonaws.services.iot.client.fuse.node.rules.actions.CloudwatchMetricActionNode;
import com.amazonaws.services.iot.client.fuse.node.rules.actions.DynamoDBActionNode;
import com.amazonaws.services.iot.client.fuse.node.rules.actions.ElasticsearchActionNode;
import com.amazonaws.services.iot.client.fuse.node.rules.actions.FirehoseActionNode;
import com.amazonaws.services.iot.client.fuse.node.rules.actions.KinesisActionNode;
import com.amazonaws.services.iot.client.fuse.node.rules.actions.LambdaActionNode;
import com.amazonaws.services.iot.client.fuse.node.rules.actions.RepublishActionNode;
import com.amazonaws.services.iot.client.fuse.node.rules.actions.S3ActionNode;
import com.amazonaws.services.iot.client.fuse.node.rules.actions.SnsActionNode;
import com.amazonaws.services.iot.client.fuse.node.rules.actions.SqsActionNode;
import com.amazonaws.services.iot.model.Action;

public class TopicRuleActionNode extends Node {

    private static final int MAX_NUM_ACTIONS = 128;
    
    protected TopicRuleActionNode(Node parent, String name) {
        super(parent, name, true);
    }

    public static TopicRuleActionNode getActionNode(Node parent, Action action) {
        String nodeName;
        TopicRuleActionNode node;

        if (action.getCloudwatchAlarm() != null) {
            nodeName = getNodeName(parent, "cloudwatch-alarm");
            node = new CloudwatchAlarmActionNode(parent, nodeName, action.getCloudwatchAlarm());
        } else if (action.getCloudwatchMetric() != null) {
            nodeName = getNodeName(parent, "cloudwatch-metric");
            node = new CloudwatchMetricActionNode(parent, nodeName, action.getCloudwatchMetric());
        } else if (action.getDynamoDB() != null) {
            nodeName = getNodeName(parent, "dynamodb");
            node = new DynamoDBActionNode(parent, nodeName, action.getDynamoDB());
        } else if (action.getElasticsearch() != null) {
            nodeName = getNodeName(parent, "elasticsearch");
            node = new ElasticsearchActionNode(parent, nodeName, action.getElasticsearch());
        } else if (action.getFirehose() != null) {
            nodeName = getNodeName(parent, "firehose");
            node = new FirehoseActionNode(parent, nodeName, action.getFirehose());
        } else if (action.getKinesis() != null) {
            nodeName = getNodeName(parent, "kinesis");
            node = new KinesisActionNode(parent, nodeName, action.getKinesis());
        } else if (action.getLambda() != null) {
            nodeName = getNodeName(parent, "lambda");
            node = new LambdaActionNode(parent, nodeName, action.getLambda());
        } else if (action.getCloudwatchMetric() != null) {
            nodeName = getNodeName(parent, "republish");
            node = new RepublishActionNode(parent, nodeName, action.getRepublish());
        } else if (action.getS3() != null) {
            nodeName = getNodeName(parent, "s3");
            node = new S3ActionNode(parent, nodeName, action.getS3());
        } else if (action.getSns() != null) {
            nodeName = getNodeName(parent, "sns");
            node = new SnsActionNode(parent, nodeName, action.getSns());
        } else if (action.getSqs() != null) {
            nodeName = getNodeName(parent, "sqs");
            node = new SqsActionNode(parent, nodeName, action.getSqs());
        } else {
            throw new RuntimeException("Unknown action node type");
        }

        return node;
    }
    
    private static String getNodeName(Node parent, String prefix) {
        if (parent.getChild(prefix) == null) {
            return prefix;
        }
        
        for (int i = 2; i < MAX_NUM_ACTIONS; i++) {
            String name = prefix + "-" + i;
            if (parent.getChild(name) == null) {
                return name;
            }            
        }
        
        throw new RuntimeException("Too many actions");
    }

}
