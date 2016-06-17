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

import java.io.File;
import java.nio.ByteBuffer;
import java.security.KeyStore;
import java.util.Set;

import com.amazonaws.services.iot.client.fuse.node.EndpointNode;
import com.amazonaws.services.iot.client.fuse.node.Node;
import com.amazonaws.services.iot.client.fuse.node.certificates.CertificatesNode;
import com.amazonaws.services.iot.client.fuse.node.policies.PoliciesNode;
import com.amazonaws.services.iot.client.fuse.node.rules.TopicRulesNode;
import com.amazonaws.services.iot.client.fuse.node.things.ThingsNode;
import com.amazonaws.services.iot.client.fuse.node.topics.TopicsNode;

import net.fusejna.DirectoryFiller;
import net.fusejna.ErrorCodes;
import net.fusejna.FlockCommand;
import net.fusejna.FuseFilesystem;
import net.fusejna.StructFlock.FlockWrapper;
import net.fusejna.StructFuseFileInfo.FileInfoWrapper;
import net.fusejna.StructStat.StatWrapper;
import net.fusejna.StructStatvfs.StatvfsWrapper;
import net.fusejna.StructTimeBuffer.TimeBufferWrapper;
import net.fusejna.XattrFiller;
import net.fusejna.XattrListFiller;
import net.fusejna.types.TypeMode.ModeWrapper;

public class FuseIotFS extends FuseFilesystem {

    private final Node root;

    public FuseIotFS(String region, String accessKeyId, String secretAccessKey, KeyStore keyStore, String keyPassword,
            Set<String> topics) {
        log(true);

        root = new Node();
        AwsIot iotClient = new AwsIot(root, region, accessKeyId, secretAccessKey, keyStore, keyPassword);

        root.setIotClient(iotClient);
        root.addChildren(new EndpointNode(root));
        root.addChildren(new ThingsNode(root));
        root.addChildren(new PoliciesNode(root));
        root.addChildren(new CertificatesNode(root));
        root.addChildren(new TopicRulesNode(root));
        root.addChildren(new TopicsNode(root, topics));
    }

    public FuseIotFS(String region, String accessKeyId, String secretAccessKey, Set<String> topics) {
        this(region, accessKeyId, secretAccessKey, null, null, topics);
    }

    @Override
    public int access(String path, int access) {
        return -ErrorCodes.ENOSYS();
    }

    @Override
    public void afterUnmount(File mountPoint) {
        // TODO Auto-generated method stub

    }

    @Override
    public void beforeMount(File mountPoint) {
        // TODO Auto-generated method stub

    }

    @Override
    public int bmap(String path, FileInfoWrapper info) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int chmod(String path, ModeWrapper mode) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int chown(String path, long uid, long gid) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int create(String path, ModeWrapper mode, FileInfoWrapper info) {
        Node node = root.find(path);
        if (node != null) {
            return -ErrorCodes.EEXIST();
        }

        int pos = path.lastIndexOf('/');
        if (pos < 0) {
            return -ErrorCodes.ENOENT();
        }
        String name = path.substring(pos + 1);
        if (name.length() < 1) {
            return -ErrorCodes.ENOENT();
        }
        path = path.substring(0, pos);
        if (path.length() < 1) {
            path = "/";
        }

        node = root.find(path);
        if (node == null || !node.isDir()) {
            return -ErrorCodes.ENOENT();
        }

        return node.create(name, mode, info);
    }

    @Override
    public void destroy() {
        // TODO Auto-generated method stub

    }

    @Override
    public int fgetattr(String path, StatWrapper stat, FileInfoWrapper info) {
        return getattr(path, stat);
    }

    @Override
    public int flush(String path, FileInfoWrapper info) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int fsync(String path, int datasync, FileInfoWrapper info) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int fsyncdir(String path, int datasync, FileInfoWrapper info) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int ftruncate(String path, long offset, FileInfoWrapper info) {
        return truncate(path, offset);
    }

    @Override
    public int getattr(String path, StatWrapper stat) {
        Node node = root.find(path, false);
        if (node == null) {
            return -ErrorCodes.ENOENT();
        }

        if (!node.isInitDone()) {
            node.init();
        }

        return node.getAttr(stat);
    }

    @Override
    protected String getName() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected String[] getOptions() {
        return new String[] { "-o", "direct_io,nosuid,nodev,noexec" };
    }

    @Override
    public int getxattr(String path, String xattr, XattrFiller filler, long size, long position) {
        return -ErrorCodes.ENOSYS();
    }

    @Override
    public void init() {
    }

    @Override
    public int link(String path, String target) {
        Node targetNode = root.find(target);
        if (targetNode == null) {
            return -ErrorCodes.ENOENT();
        }

        Node node = root.find(path);
        if (node != null) {
            return -ErrorCodes.EEXIST();
        }

        int pos = path.lastIndexOf('/');
        if (pos < 0) {
            return -ErrorCodes.ENOENT();
        }
        String name = path.substring(pos + 1);
        if (name.length() < 1) {
            return -ErrorCodes.ENOENT();
        }
        path = path.substring(0, pos);
        if (path.length() < 1) {
            path = "/";
        }

        node = root.find(path);
        if (node == null || !node.isDir()) {
            return -ErrorCodes.ENOENT();
        }

        return node.link(name, targetNode);
    }

    @Override
    public int listxattr(String path, XattrListFiller filler) {
        return -ErrorCodes.ENOSYS();
    }

    @Override
    public int lock(String path, FileInfoWrapper info, FlockCommand command, FlockWrapper flock) {
        return -ErrorCodes.ENOSYS();
    }

    @Override
    public int mkdir(String path, ModeWrapper mode) {
        Node node = root.find(path);
        if (node != null) {
            return -ErrorCodes.EEXIST();
        }

        int pos = path.lastIndexOf('/');
        if (pos < 0) {
            return -ErrorCodes.ENOENT();
        }
        String name = path.substring(pos + 1);
        if (name.length() < 1) {
            return -ErrorCodes.ENOENT();
        }
        path = path.substring(0, pos);
        if (path.length() < 1) {
            path = "/";
        }

        node = root.find(path);
        if (node == null || !node.isDir()) {
            return -ErrorCodes.ENOENT();
        }

        return node.mkdir(name, mode);
    }

    @Override
    public int mknod(String path, ModeWrapper mode, long dev) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int open(String path, FileInfoWrapper info) {
        Node node = root.find(path);
        if (node == null) {
            return -ErrorCodes.ENOENT();
        }

        if (!node.isInitDone()) {
            node.init();
        }
        return node.open(path, info);
    }

    @Override
    public int opendir(String path, FileInfoWrapper info) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int read(String path, ByteBuffer buffer, long size, long offset, FileInfoWrapper info) {
        Node node = root.find(path);
        if (node == null) {
            return -ErrorCodes.ENOENT();
        }

        return node.read(path, buffer, size, offset, info);
    }

    @Override
    public int readdir(String path, DirectoryFiller filler) {
        Node node = root.find(path);
        if (node == null) {
            return -ErrorCodes.ENOENT();
        }

        if (!node.isDir()) {
            return -ErrorCodes.ENOTDIR();
        }

        if (!node.isInitDone()) {
            node.init();
        }
        return node.readdir(filler);
    }

    @Override
    public int readlink(String path, ByteBuffer buffer, long size) {
        Node node = root.find(path, false);
        if (node == null) {
            return -ErrorCodes.ENOENT();
        }

        if (!node.isInitDone()) {
            node.init();
        }

        return node.readlink(buffer, size);
    }

    @Override
    public int release(String path, FileInfoWrapper info) {
        Node node = root.find(path);
        if (node == null) {
            return -ErrorCodes.ENOENT();
        }

        return node.release(path, info);
    }

    @Override
    public int releasedir(String path, FileInfoWrapper info) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int removexattr(String path, String xattr) {
        return -ErrorCodes.ENOSYS();
    }

    @Override
    public int rename(String path, String newName) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int rmdir(String path) {
        Node node = root.find(path, false);
        if (node == null) {
            return -ErrorCodes.ENOENT();
        }

        return node.rmdir(path);
    }

    @Override
    public int setxattr(String path, String xattr, ByteBuffer value, long size, int flags, int position) {
        return -ErrorCodes.ENOSYS();
    }

    @Override
    public int statfs(String path, StatvfsWrapper wrapper) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int symlink(String path, String target) {
        Node node = root.find(target);
        if (node != null) {
            return -ErrorCodes.EEXIST();
        }

        int pos = target.lastIndexOf('/');
        if (pos < 0) {
            return -ErrorCodes.ENOENT();
        }
        String name = target.substring(pos + 1);
        if (name.length() < 1) {
            return -ErrorCodes.ENOENT();
        }
        String targetPath = target.substring(0, pos);
        if (targetPath.length() < 1) {
            targetPath = "/";
        }

        node = root.find(targetPath);
        if (node == null || !node.isDir()) {
            return -ErrorCodes.ENOENT();
        }

        return node.symlink(name, path);
    }

    @Override
    public int truncate(String path, long offset) {
        Node node = root.find(path);
        if (node == null) {
            return -ErrorCodes.ENOENT();
        }

        return node.truncate(path, offset);
    }

    @Override
    public int unlink(String path) {
        Node node = root.find(path, false);
        if (node == null) {
            return -ErrorCodes.ENOENT();
        }

        return node.unlink(path);
    }

    @Override
    public int utimens(String path, TimeBufferWrapper wrapper) {
        return -ErrorCodes.ENOSYS();
    }

    @Override
    public int write(String path, ByteBuffer buf, long bufSize, long offset, FileInfoWrapper info) {
        Node node = root.find(path);
        if (node == null) {
            return -ErrorCodes.ENOENT();
        }

        return node.write(path, buf, bufSize, offset, info);
    }

}
