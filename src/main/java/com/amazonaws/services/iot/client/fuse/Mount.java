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

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.Set;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.iot.client.fuse.CommandArguments;

import net.fusejna.FuseException;

public class Mount {

    public static void main(String[] args) throws FuseException {
        CommandArguments commandArgs = CommandArguments.parse(args);

        String mountPoint = commandArgs.get("dest");
        String region = commandArgs.get("region");
        String accessKeyId = commandArgs.get("access-key-id");
        String secretAccessKey = commandArgs.get("secret-access-key");
        String certificateFile = commandArgs.get("certificate");
        String privateKeyFile = commandArgs.get("private-key");
        Set<String> topics = commandArgs.getAll("topic");

        if (commandArgs.getAll("help") != null || mountPoint == null || region == null) {
            usageAndExit();
        }

        if (accessKeyId == null && secretAccessKey == null) {
            DefaultAWSCredentialsProviderChain chain = new DefaultAWSCredentialsProviderChain();
            AWSCredentials credentials = chain.getCredentials();
            
            if (credentials != null) {
                accessKeyId = credentials.getAWSAccessKeyId();
                secretAccessKey = credentials.getAWSSecretKey();
            }
        }
        if (accessKeyId == null || secretAccessKey == null) {
            usageAndExit();
        }

        FuseIotFS fs;
        if (certificateFile != null && privateKeyFile != null) {
            KeyStorePasswordPair pair = getKeyStorePasswordPair(certificateFile, privateKeyFile);
            fs = new FuseIotFS(region, accessKeyId, secretAccessKey, pair.keyStore, pair.keyPassword, topics);
        } else {
            fs = new FuseIotFS(region, accessKeyId, secretAccessKey, topics);
        }

        fs.mount(mountPoint);
    }

    private static void usageAndExit() {
        System.err
                .println("Usage: mount-iot-fuse [-help] -dest <mount-point> -region <aws-region> [-access-key-id <access-key-id>] [-secret-access-key <secret-access-key>] [-certificate <certificate-file>] [-private-key <private-key-file>] [-topic <topic>]...");
        System.exit(1);
    }

    static class KeyStorePasswordPair {
        public KeyStore keyStore;
        public String keyPassword;

        public KeyStorePasswordPair(KeyStore keyStore, String keyPassword) {
            this.keyStore = keyStore;
            this.keyPassword = keyPassword;
        }
    }

    private static KeyStorePasswordPair getKeyStorePasswordPair(String certificateFile, String privateKeyFile) {
        if (certificateFile == null || privateKeyFile == null) {
            System.out.println("Certificate or private key file missing");
            return null;
        }

        Certificate certificate = loadCertificateFromFile(certificateFile);
        PrivateKey privateKey = loadPrivateKeyFromFile(privateKeyFile);
        if (certificate == null || privateKey == null) {
            return null;
        }

        return getKeyStorePasswordPair(certificate, privateKey);
    }

    private static KeyStorePasswordPair getKeyStorePasswordPair(Certificate certificate, PrivateKey privateKey) {
        KeyStore keyStore = null;
        String keyPassword = null;
        try {
            keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null);
            keyStore.setCertificateEntry("alias", certificate);

            // randomly generated key password for the key in the KeyStore
            keyPassword = new BigInteger(128, new SecureRandom()).toString(32);
            keyStore.setKeyEntry("alias", privateKey, keyPassword.toCharArray(), new Certificate[] { certificate });
        } catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException e) {
            System.out.println("Failed to create key store");
            return null;
        }

        return new KeyStorePasswordPair(keyStore, keyPassword);
    }

    private static Certificate loadCertificateFromFile(String filename) {
        Certificate certificate = null;

        File file = new File(filename);
        if (!file.exists()) {
            System.out.println("Certificate file not found: " + filename);
            return null;
        }
        try (BufferedInputStream stream = new BufferedInputStream(new FileInputStream(file))) {
            CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
            certificate = certFactory.generateCertificate(stream);
        } catch (IOException | CertificateException e) {
            System.out.println("Failed to load certificate file " + filename);
        }

        return certificate;
    }

    private static PrivateKey loadPrivateKeyFromFile(String filename) {
        PrivateKey privateKey = null;

        File file = new File(filename);
        if (!file.exists()) {
            System.out.println("Certificate file not found: " + filename);
            return null;
        }
        try (DataInputStream stream = new DataInputStream(new FileInputStream(file))) {
            privateKey = PrivateKeyReader.getPrivateKey(stream);
        } catch (IOException | GeneralSecurityException e) {
            System.out.println("Failed to load private key from file " + filename);
        }

        return privateKey;
    }

}
