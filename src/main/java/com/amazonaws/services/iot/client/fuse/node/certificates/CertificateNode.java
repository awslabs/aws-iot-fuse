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

import java.util.Date;

import net.fusejna.ErrorCodes;

import com.amazonaws.services.iot.client.fuse.node.InfoNode;
import com.amazonaws.services.iot.client.fuse.node.Node;
import com.amazonaws.services.iot.model.KeyPair;

public class CertificateNode extends Node {

    private final String certificateId;
    private final String certificateArn;
    private String status;

    public CertificateNode(Node parent, String name, String certificateId, String certificateArn, String certPem,
            KeyPair keyPair, Date creationDate, String status) {
        super(parent, name, true);

        this.certificateId = certificateId;
        this.certificateArn = certificateArn;
        this.creationDate = creationDate;
        this.status = status;

        addChildren(new InfoNode(this, "id", this.certificateId));
        addChildren(new InfoNode(this, "arn", this.certificateArn));
        addChildren(new InfoNode(this, "status", this.status));
        addChildren(new CertificatePoliciesNode(this, this.certificateArn));

        String prefix = certificateId.substring(0, 10);
        if (certPem != null) {
            addChildren(new InfoNode(this, prefix + "-certificate.pem.crt", certPem));
        }
        if (keyPair != null) {
            addChildren(new InfoNode(this, prefix + "-private.pem.key", keyPair.getPrivateKey()));
            addChildren(new InfoNode(this, prefix + "-public.pem.key", keyPair.getPublicKey()));
        }
    }

    public String getCertificateId() {
        return certificateId;
    }

    public String getCertificateArn() {
        return certificateArn;
    }

    @Override
    public int rmdir(String path) {
        if (!children.isEmpty()) {
            return -ErrorCodes.ENOTEMPTY();
        }

        int r = iotClient.updateCertificate(certificateId, false);
        if (r != 0) {
            return r;
        }

        r = iotClient.deleteCertificate(certificateId);
        if (r != 0) {
            return r;
        }

        return super.rmdir(path);
    }

}
