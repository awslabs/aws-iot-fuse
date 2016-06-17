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

package com.amazonaws.services.iot.client.fuse.node;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.amazonaws.services.iot.client.fuse.AwsIot;

import net.fusejna.DirectoryFiller;
import net.fusejna.ErrorCodes;
import net.fusejna.StructFuseFileInfo.FileInfoWrapper;
import net.fusejna.StructStat.StatWrapper;
import net.fusejna.types.TypeMode.ModeWrapper;
import net.fusejna.types.TypeMode.NodeType;

public class Node {

    private static final int MAX_DIR_LEVEL = 64;
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    protected final String name;
    protected final Node root;
    protected final Node parent;
    protected final Map<String, Node> children;
    protected final boolean isDir;
    protected long reInitDelay;
    protected Future<?> reInitTask;
    protected boolean initDone;
    protected long size;
    protected Date creationDate;
    protected AwsIot iotClient;

    public Node() {
        this(null, "", true);
    }

    public Node(Node parent, String name, boolean isDir) {
        if (parent != null) {
            this.parent = parent;
            this.root = parent.getRoot();
            this.iotClient = parent.iotClient;
        } else {
            this.parent = null;
            this.root = this;
            this.iotClient = null;
        }
        this.name = name;
        this.isDir = isDir;
        this.children = new HashMap<>();
        this.creationDate = new Date();
    }

    public Node getRoot() {
        return root;
    }

    public Node getParent() {
        return parent;
    }

    public String getName() {
        return name;
    }

    public boolean isDir() {
        return isDir;
    }

    public boolean isInitDone() {
        return initDone;
    }

    public AwsIot getIotClient() {
        return iotClient;
    }

    public void setIotClient(AwsIot iotClient) {
        this.iotClient = iotClient;
    }

    public long getReInitDelay() {
        return this.reInitDelay;
    }

    public void setReInitDelay(int delay) {
        if (reInitTask != null) {
            reInitTask.cancel(false);
            reInitTask = null;
        }

        if (delay <= 0) {
            return;
        }

        reInitDelay = delay;
        reInitTask = scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                initDone = false;
            }
        }, 0, reInitDelay, TimeUnit.MILLISECONDS);
    }

    public void init() {
        initDone = true;
    }

    public Node find(String path) {
        return find(path, true);
    }

    public Node find(String path, boolean follow) {
        if (path == null) {
            return null;
        }

        Node node = null;
        if (path.charAt(0) == '/') {
            node = root;
            if (path.length() == 1) {
                return node;
            }
            path = path.substring(1);
        } else {
            node = this;
        }

        List<String> dirs = Arrays.asList(path.split("/"));
        synchronized (this) {
            int levels = 0;
            for (Iterator<String> it = dirs.iterator(); it.hasNext();) {
                if (levels++ > MAX_DIR_LEVEL) {
                    return null;
                }

                String dir = it.next();
                if (dir.equals("..")) {
                    node = node.parent;
                    continue;
                } else if (dir.equals(".")) {
                    continue;
                }

                if (!node.initDone) {
                    node.init();
                }

                node = node.children.get(dir);
                if (node == null) {
                    return null;
                }

                if (follow && node instanceof LinkNode) {
                    node = ((LinkNode) node).getSource();
                }

                if (!node.isDir && it.hasNext()) {
                    return null;
                }
            }

            return node;
        }
    }

    public String getAbsolutePath() {
        StringBuffer buffer = new StringBuffer();
        buffer.insert(0, name);

        Node currentParent = this.parent;
        while (currentParent != null) {
            buffer.insert(0, currentParent.name + "/");
            currentParent = currentParent.parent;
        }

        return buffer.toString();
    }

    public String getRelativePathToRoot() {
        StringBuffer buffer = new StringBuffer();

        Node currentParent = this.parent;
        while (currentParent != null) {
            if (currentParent.parent == null) {
                break;
            }
            buffer.insert(0, (buffer.length() > 0) ? "../" : "..");
            currentParent = currentParent.parent;
        }

        return buffer.toString();
    }

    public int getAttr(StatWrapper stat) {
        if (isDir) {
            stat.setMode(NodeType.DIRECTORY);
        } else {
            stat.setMode(NodeType.FILE).size(size);
        }
        stat.setAllTimesMillis(creationDate.getTime());
        return 0;
    }

    public void addChildren(List<? extends Node> nodes) {
        for (Node node : nodes) {
            children.put(node.name, node);
        }
    }

    public void updateChildren(List<? extends Node> nodes) {
        Set<String> namesSet = new HashSet<>();
        for (Node node : nodes) {
            children.putIfAbsent(node.name, node);
            namesSet.add(node.name);
        }
        children.keySet().retainAll(namesSet);
    }

    public void addChildren(Node node) {
        children.put(node.name, node);
    }

    public Node getChild(String name) {
        return children.get(name);
    }

    public int readdir(DirectoryFiller filler) {
        filler.add(children.keySet());
        return 0;
    }

    public int link(String name, Node sourceNode) {
        LinkNode node = new LinkNode(this, name, sourceNode);
        int r = sourceNode.accept(node);
        if (r == 0) {
            addChildren(node);
        }
        return r;
    }

    public int unlink(String path) {
        if (parent == null) {
            return -ErrorCodes.EACCES();
        }

        if (isDir && !children.isEmpty()) {
            return -ErrorCodes.ENOTEMPTY();
        }

        parent.children.remove(name);
        return 0;
    }

    public int rmdir(String path) {
        if (parent == null) {
            return -ErrorCodes.EACCES();
        }

        if (isDir && !children.isEmpty()) {
            return -ErrorCodes.ENOTEMPTY();
        }

        parent.children.remove(name);
        return 0;
    }

    public int read(String path, ByteBuffer buf, long bufSize, long offset, FileInfoWrapper info) {
        return 0;
    }

    public int open(String path, FileInfoWrapper info) {
        return 0;
    }

    public int create(String name, ModeWrapper mode, FileInfoWrapper info) {
        return 0;
    }

    public int accept(LinkNode node) {
        return 0;
    }

    public int write(String path, ByteBuffer buf, long bufSize, long offset, FileInfoWrapper info) {
        return 0;
    }

    public int release(String path, FileInfoWrapper info) {
        return 0;
    }

    public int truncate(String path, long offset) {
        return 0;
    }

    public int readlink(ByteBuffer buffer, long size2) {
        return 0;
    }

    public int mkdir(String name, ModeWrapper mode) {
        return 0;
    }

    public int symlink(String name, String path) {
        return 0;
    }

    public int symunlink(String name) {
        return 0;
    }

}
