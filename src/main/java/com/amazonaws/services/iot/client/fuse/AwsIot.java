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

package com.amazonaws.services.iot.client.fuse;

import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import net.fusejna.ErrorCodes;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.iot.AWSIotClient;
import com.amazonaws.services.iot.client.AWSIotDevice;
import com.amazonaws.services.iot.client.AWSIotException;
import com.amazonaws.services.iot.client.AWSIotMqttClient;
import com.amazonaws.services.iot.client.fuse.node.LinkNode;
import com.amazonaws.services.iot.client.fuse.node.Node;
import com.amazonaws.services.iot.client.fuse.node.certificates.CertificateNode;
import com.amazonaws.services.iot.client.fuse.node.policies.PolicyDocumentNode;
import com.amazonaws.services.iot.client.fuse.node.policies.PolicyNode;
import com.amazonaws.services.iot.client.fuse.node.rules.TopicRuleActionNode;
import com.amazonaws.services.iot.client.fuse.node.rules.TopicRuleNode;
import com.amazonaws.services.iot.client.fuse.node.things.ThingNode;
import com.amazonaws.services.iot.model.Action;
import com.amazonaws.services.iot.model.AttachPrincipalPolicyRequest;
import com.amazonaws.services.iot.model.AttachThingPrincipalRequest;
import com.amazonaws.services.iot.model.Certificate;
import com.amazonaws.services.iot.model.CertificateStatus;
import com.amazonaws.services.iot.model.CreateKeysAndCertificateRequest;
import com.amazonaws.services.iot.model.CreateKeysAndCertificateResult;
import com.amazonaws.services.iot.model.CreatePolicyRequest;
import com.amazonaws.services.iot.model.CreatePolicyResult;
import com.amazonaws.services.iot.model.CreatePolicyVersionRequest;
import com.amazonaws.services.iot.model.CreatePolicyVersionResult;
import com.amazonaws.services.iot.model.CreateThingRequest;
import com.amazonaws.services.iot.model.DeleteCertificateRequest;
import com.amazonaws.services.iot.model.DeleteConflictException;
import com.amazonaws.services.iot.model.DeletePolicyRequest;
import com.amazonaws.services.iot.model.DeletePolicyVersionRequest;
import com.amazonaws.services.iot.model.DeleteThingRequest;
import com.amazonaws.services.iot.model.DeleteTopicRuleRequest;
import com.amazonaws.services.iot.model.DescribeEndpointRequest;
import com.amazonaws.services.iot.model.DescribeEndpointResult;
import com.amazonaws.services.iot.model.DetachPrincipalPolicyRequest;
import com.amazonaws.services.iot.model.DetachThingPrincipalRequest;
import com.amazonaws.services.iot.model.GetPolicyRequest;
import com.amazonaws.services.iot.model.GetPolicyResult;
import com.amazonaws.services.iot.model.GetPolicyVersionRequest;
import com.amazonaws.services.iot.model.GetPolicyVersionResult;
import com.amazonaws.services.iot.model.GetTopicRuleRequest;
import com.amazonaws.services.iot.model.GetTopicRuleResult;
import com.amazonaws.services.iot.model.InvalidRequestException;
import com.amazonaws.services.iot.model.ListCertificatesRequest;
import com.amazonaws.services.iot.model.ListCertificatesResult;
import com.amazonaws.services.iot.model.ListPoliciesRequest;
import com.amazonaws.services.iot.model.ListPoliciesResult;
import com.amazonaws.services.iot.model.ListPolicyVersionsRequest;
import com.amazonaws.services.iot.model.ListPolicyVersionsResult;
import com.amazonaws.services.iot.model.ListPrincipalPoliciesRequest;
import com.amazonaws.services.iot.model.ListPrincipalPoliciesResult;
import com.amazonaws.services.iot.model.ListThingPrincipalsRequest;
import com.amazonaws.services.iot.model.ListThingPrincipalsResult;
import com.amazonaws.services.iot.model.ListThingsRequest;
import com.amazonaws.services.iot.model.ListThingsResult;
import com.amazonaws.services.iot.model.ListTopicRulesRequest;
import com.amazonaws.services.iot.model.ListTopicRulesResult;
import com.amazonaws.services.iot.model.Policy;
import com.amazonaws.services.iot.model.PolicyVersion;
import com.amazonaws.services.iot.model.ResourceNotFoundException;
import com.amazonaws.services.iot.model.SetDefaultPolicyVersionRequest;
import com.amazonaws.services.iot.model.ThingAttribute;
import com.amazonaws.services.iot.model.TopicRule;
import com.amazonaws.services.iot.model.TopicRuleListItem;
import com.amazonaws.services.iot.model.UnauthorizedException;
import com.amazonaws.services.iot.model.UpdateCertificateRequest;

public class AwsIot {

    private static final int BATCH_SIZE_LIST_THINGS = 50;
    private static final int BATCH_SIZE_LIST_CERTIFICATES = 50;
    private static final int BATCH_SIZE_LIST_POLICIES = 50;
    private static final int BATCH_SIZE_LIST_TOPIC_RULES = 50;
    private static final int BATCH_SIZE_LIST_PRINCIPAL_POLICIES = 50;

    private final AWSIotClient client;
    private final AWSIotMqttClient mqttClient;
    private final Node root;

    public AwsIot(Node root, String region, String accessKeyId, String secretAccessKey, KeyStore keyStore,
            String keyPassword) {
        AWSCredentials awsCredentials = new BasicAWSCredentials(accessKeyId, secretAccessKey);

        this.client = new AWSIotClient(awsCredentials);
        this.client.withRegion(Regions.fromName(region));

        String endpoint = getEndpoint();
        String clientId = UUID.randomUUID().toString();
        if (keyStore != null && keyPassword != null) {
            this.mqttClient = new AWSIotMqttClient(endpoint, clientId, keyStore, keyPassword);
        } else {
            this.mqttClient = new AWSIotMqttClient(endpoint, clientId, accessKeyId, secretAccessKey);
        }

        try {
            this.mqttClient.connect();
        } catch (AWSIotException e) {
            throw new RuntimeException("Failed to connect to AWS IoT service", e);
        }

        this.root = root;
    }

    public Node getRoot() {
        return root;
    }

    public List<ThingNode> getThings(Node parent) {
        List<ThingNode> thingNodes = new ArrayList<>();

        String nextToken = null;
        while (true) {
            ListThingsRequest req = new ListThingsRequest().withMaxResults(BATCH_SIZE_LIST_THINGS);
            if (nextToken != null) {
                req.setNextToken(nextToken);
            }

            ListThingsResult res = client.listThings(req);

            for (ThingAttribute thing : res.getThings()) {
                ThingNode thingNode = new ThingNode(parent, thing.getThingName());
                thingNodes.add(thingNode);
            }

            nextToken = res.getNextToken();
            if (nextToken == null || res.getThings().size() < BATCH_SIZE_LIST_THINGS) {
                break;
            }
        }

        return thingNodes;
    }

    public List<String> getPrincipals(Node parent) {
        List<String> principals = new ArrayList<>();
        ListThingPrincipalsRequest req = new ListThingPrincipalsRequest().withThingName(parent.getParent().getName());

        ListThingPrincipalsResult res = client.listThingPrincipals(req);
        principals.addAll(res.getPrincipals());

        return principals;
    }

    public String getEndpoint() {
        DescribeEndpointRequest req = new DescribeEndpointRequest();

        DescribeEndpointResult res = client.describeEndpoint(req);

        return res.getEndpointAddress();
    }

    public ThingNode createThing(Node parent, String name) {
        CreateThingRequest req = new CreateThingRequest().withThingName(name);

        client.createThing(req);

        return new ThingNode(parent, name);
    }

    public List<CertificateNode> getCertificates(Node parent) {
        List<CertificateNode> certifcateNodes = new ArrayList<>();

        String nextToken = null;
        while (true) {
            ListCertificatesRequest req = new ListCertificatesRequest().withPageSize(BATCH_SIZE_LIST_CERTIFICATES);
            if (nextToken != null) {
                req.setMarker(nextToken);
            }

            ListCertificatesResult res = client.listCertificates(req);

            for (Certificate cert : res.getCertificates()) {
                CertificateNode certifcateNode = new CertificateNode(parent, cert.getCertificateId(),
                        cert.getCertificateId(), cert.getCertificateArn(), null, null, cert.getCreationDate(),
                        cert.getStatus());
                certifcateNodes.add(certifcateNode);
            }

            nextToken = res.getNextMarker();
            if (nextToken == null || res.getCertificates().size() < BATCH_SIZE_LIST_CERTIFICATES) {
                break;
            }
        }

        return certifcateNodes;
    }

    public List<PolicyNode> getPolicies(Node parent) {
        List<PolicyNode> policyNodes = new ArrayList<>();

        String nextToken = null;
        while (true) {
            ListPoliciesRequest req = new ListPoliciesRequest().withPageSize(BATCH_SIZE_LIST_POLICIES);
            if (nextToken != null) {
                req.setMarker(nextToken);
            }

            ListPoliciesResult res = client.listPolicies(req);

            for (Policy policy : res.getPolicies()) {
                PolicyNode policyNode = new PolicyNode(parent, policy.getPolicyName(), policy.getPolicyArn());
                policyNodes.add(policyNode);
            }

            nextToken = res.getNextMarker();
            if (nextToken == null || res.getPolicies().size() < BATCH_SIZE_LIST_POLICIES) {
                break;
            }
        }

        return policyNodes;
    }

    public List<TopicRuleNode> getTopicRules(Node parent) {
        List<TopicRuleNode> topicRulesNodes = new ArrayList<>();

        String nextToken = null;
        while (true) {
            ListTopicRulesRequest req = new ListTopicRulesRequest().withMaxResults(BATCH_SIZE_LIST_TOPIC_RULES);
            if (nextToken != null) {
                req.setNextToken(nextToken);
            }

            ListTopicRulesResult res = client.listTopicRules(req);

            for (TopicRuleListItem rule : res.getRules()) {
                TopicRuleNode ruleNode = new TopicRuleNode(parent, rule);
                topicRulesNodes.add(ruleNode);
            }

            nextToken = res.getNextToken();
            if (nextToken == null || res.getRules().size() < BATCH_SIZE_LIST_TOPIC_RULES) {
                break;
            }
        }

        return topicRulesNodes;
    }

    public void subscribe(Node parent, String topic) {
        MessageListener listener = new MessageListener(topic, parent);

        try {
            mqttClient.subscribe(listener, true);
        } catch (AWSIotException e) {
            // TODO: log error message;
        }
    }

    public void publish(String topic, byte[] data) {
        try {
            mqttClient.publish(topic, data);
        } catch (AWSIotException e) {
            // TODO: log error message;
        }
    }

    public int attachDevice(AWSIotDevice device) {
        try {
            mqttClient.attach(device);
        } catch (Exception e) {
            return translateException(e);
        }
        return 0;
    }

    public int detachDevice(AWSIotDevice device) {
        try {
            mqttClient.detach(device);
        } catch (Exception e) {
            return translateException(e);
        }
        return 0;
    }

    public PolicyNode createPolicy(Node parent, String policyName, String policyDocument) {
        CreatePolicyRequest req = new CreatePolicyRequest().withPolicyName(policyName).withPolicyDocument(
                policyDocument);
        CreatePolicyResult res = client.createPolicy(req);

        return new PolicyNode(parent, policyName, res.getPolicyArn());
    }

    public PolicyDocumentNode createPolicyVersion(Node parent, String policyName, String policyDocument) {
        CreatePolicyVersionRequest req = new CreatePolicyVersionRequest().withPolicyName(policyName)
                .withPolicyDocument(policyDocument);
        CreatePolicyVersionResult res = client.createPolicyVersion(req);
        return new PolicyDocumentNode(parent, res.getPolicyVersionId(), policyName, res.getPolicyVersionId());
    }

    public String getPolicy(String policyName) {
        GetPolicyRequest req = new GetPolicyRequest().withPolicyName(policyName);

        GetPolicyResult res = client.getPolicy(req);

        return res.getPolicyDocument();
    }

    public String getPolicy(String policyName, String policyVersion) {
        if (policyVersion == null) {
            return getPolicy(policyName);
        }

        GetPolicyVersionRequest req = new GetPolicyVersionRequest().withPolicyName(policyName).withPolicyVersionId(
                policyVersion);

        GetPolicyVersionResult res = client.getPolicyVersion(req);

        return res.getPolicyDocument();
    }

    public List<Node> getPolicyVersions(Node parent, String policyName) {
        List<Node> policyVersionNodes = new ArrayList<>();

        ListPolicyVersionsRequest req = new ListPolicyVersionsRequest().withPolicyName(policyName);
        ListPolicyVersionsResult res = client.listPolicyVersions(req);
        for (PolicyVersion policy : res.getPolicyVersions()) {
            PolicyDocumentNode node = new PolicyDocumentNode(parent, policy.getVersionId(), policyName,
                    policy.getVersionId());
            if (policy.isDefaultVersion()) {
                policyVersionNodes.add(new LinkNode(parent, "default", node));
            }
            policyVersionNodes.add(node);
        }

        return policyVersionNodes;
    }

    public int deletePolicyVersion(String policyName, String policyVersion) {
        DeletePolicyVersionRequest req = new DeletePolicyVersionRequest().withPolicyName(policyName)
                .withPolicyVersionId(policyVersion);

        try {
            client.deletePolicyVersion(req);
        } catch (Exception e) {
            return translateException(e);
        }
        return 0;
    }

    public int deletePolicy(String policyName) {
        DeletePolicyRequest req = new DeletePolicyRequest().withPolicyName(policyName);

        try {
            client.deletePolicy(req);
        } catch (Exception e) {
            return translateException(e);
        }
        return 0;
    }

    public String getTopicRuleSql(String ruleName) {
        GetTopicRuleRequest req = new GetTopicRuleRequest().withRuleName(ruleName);

        GetTopicRuleResult res = client.getTopicRule(req);
        TopicRule rule = res.getRule();
        if (rule != null) {
            return rule.getSql();
        }
        return null;
    }

    public Node createCertificate(Node parent, String name) {
        Date creationDate = new Date();
        boolean isActive = true;

        CreateKeysAndCertificateRequest req = new CreateKeysAndCertificateRequest();
        req.setSetAsActive(isActive);

        CreateKeysAndCertificateResult res = client.createKeysAndCertificate(req);

        return new CertificateNode(parent, name, res.getCertificateId(), res.getCertificateArn(),
                res.getCertificatePem(), res.getKeyPair(), creationDate, isActive ? "ACTIVE" : "INACTIVE");
    }

    public int attachPolicy(String certificateArn, String policyName) {
        AttachPrincipalPolicyRequest req = new AttachPrincipalPolicyRequest().withPrincipal(certificateArn)
                .withPolicyName(policyName);

        try {
            client.attachPrincipalPolicy(req);
        } catch (Exception e) {
            return translateException(e);
        }
        return 0;
    }

    public int detachPolicy(String certificateArn, String policyName) {
        DetachPrincipalPolicyRequest req = new DetachPrincipalPolicyRequest().withPrincipal(certificateArn)
                .withPolicyName(policyName);

        try {
            client.detachPrincipalPolicy(req);
        } catch (Exception e) {
            return translateException(e);
        }
        return 0;
    }

    public List<String> getCertificatePolicies(String certificateArn) {
        List<String> policyNames = new ArrayList<>();

        String nextToken = null;
        while (true) {
            ListPrincipalPoliciesRequest req = new ListPrincipalPoliciesRequest().withPrincipal(certificateArn)
                    .withPageSize(BATCH_SIZE_LIST_PRINCIPAL_POLICIES);

            if (nextToken != null) {
                req.setMarker(nextToken);
            }

            ListPrincipalPoliciesResult res = client.listPrincipalPolicies(req);

            for (Policy policy : res.getPolicies()) {
                policyNames.add(policy.getPolicyName());
            }

            nextToken = res.getNextMarker();
            if (nextToken == null || res.getPolicies().size() < BATCH_SIZE_LIST_PRINCIPAL_POLICIES) {
                break;
            }
        }

        return policyNames;
    }

    public int attachCertificate(String certificateArn, String thingName) {
        AttachThingPrincipalRequest req = new AttachThingPrincipalRequest().withPrincipal(certificateArn)
                .withThingName(thingName);

        try {
            client.attachThingPrincipal(req);
        } catch (Exception e) {
            return translateException(e);
        }
        return 0;
    }

    public int detachCertificate(String certificateArn, String thingName) {
        DetachThingPrincipalRequest req = new DetachThingPrincipalRequest().withPrincipal(certificateArn)
                .withThingName(thingName);

        try {
            client.detachThingPrincipal(req);
        } catch (Exception e) {
            return translateException(e);
        }
        return 0;
    }

    public int deleteCertificate(String certificateId) {
        DeleteCertificateRequest req = new DeleteCertificateRequest().withCertificateId(certificateId);

        try {
            client.deleteCertificate(req);
        } catch (Exception e) {
            return translateException(e);
        }
        return 0;
    }

    public int updateCertificate(String certificateId, boolean activate) {
        UpdateCertificateRequest req = new UpdateCertificateRequest().withCertificateId(certificateId).withNewStatus(
                activate ? CertificateStatus.ACTIVE : CertificateStatus.INACTIVE);

        try {
            client.updateCertificate(req);
        } catch (Exception e) {
            return translateException(e);
        }
        return 0;
    }

    public int setDefaultPolicyVersion(String policyName, String policyVersion) {
        SetDefaultPolicyVersionRequest req = new SetDefaultPolicyVersionRequest().withPolicyName(policyName)
                .withPolicyVersionId(policyVersion);

        try {
            client.setDefaultPolicyVersion(req);
        } catch (Exception e) {
            return translateException(e);
        }
        return 0;
    }

    public int deleteThing(String thingName) {
        DeleteThingRequest req = new DeleteThingRequest().withThingName(thingName);

        try {
            client.deleteThing(req);
        } catch (Exception e) {
            return translateException(e);
        }
        return 0;
    }

    public List<TopicRuleActionNode> getTopicRuleActions(Node parent, String ruleName) {
        List<TopicRuleActionNode> actionList = new ArrayList<>();

        GetTopicRuleRequest req = new GetTopicRuleRequest().withRuleName(ruleName);

        GetTopicRuleResult res = client.getTopicRule(req);
        TopicRule rule = res.getRule();

        for (Action action : rule.getActions()) {
            actionList.add(TopicRuleActionNode.getActionNode(parent, action));
        }

        return actionList;
    }

    public int deleteTopicRule(String ruleName) {
        DeleteTopicRuleRequest req = new DeleteTopicRuleRequest().withRuleName(ruleName);

        try {
            client.deleteTopicRule(req);
        } catch (Exception e) {
            return translateException(e);
        }
        return 0;
    }

    private int translateException(Exception e) {
        if (e instanceof ResourceNotFoundException) {
            return -ErrorCodes.ENODEV();
        } else if (e instanceof UnauthorizedException) {
            return -ErrorCodes.EACCES();
        } else if (e instanceof InvalidRequestException) {
            return -ErrorCodes.EINVAL();
        } else if (e instanceof DeleteConflictException) {
            return -ErrorCodes.EBUSY();
        }

        return -ErrorCodes.EIO();
    }

}
