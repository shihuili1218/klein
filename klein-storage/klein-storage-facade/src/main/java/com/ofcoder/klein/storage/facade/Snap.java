/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ofcoder.klein.storage.facade;

import java.io.Serializable;

/**
 * snapshot information.
 *
 * @author 释慧利
 */
public class Snap implements Serializable {
    private long checkpoint;
    private byte[] snap;

    public Snap() {
    }

    public Snap(final long checkpoint, final byte[] snap) {
        this.checkpoint = checkpoint;
        this.snap = snap;
    }

    /**
     * get checkpoint.
     * Checkpoint is the snapshot last applied instanceId
     *
     * @return checkpoint
     */
    public long getCheckpoint() {
        return checkpoint;
    }

    /**
     * set checkpoint.
     *
     * @param checkpoint checkpoint
     */
    public void setCheckpoint(final long checkpoint) {
        this.checkpoint = checkpoint;
    }

    /**
     * get snap.
     *
     * @return snapshot
     */
    public byte[] getSnap() {
        return snap.clone();
    }

    /**
     * set snapshot.
     *
     * @param snap snapshot
     */
    public void setSnap(final byte[] snap) {
        this.snap = snap.clone();
    }
}
