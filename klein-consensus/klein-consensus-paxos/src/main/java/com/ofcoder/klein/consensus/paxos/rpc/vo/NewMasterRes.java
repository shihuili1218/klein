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
package com.ofcoder.klein.consensus.paxos.rpc.vo;

import java.io.Serializable;

/**
 * NewMaster Request Data.
 *
 * @author 释慧利
 */
public class NewMasterRes implements Serializable {
    private long curInstanceId;
    private long lastAppliedId;
    private long checkpoint;
    private boolean granted;

    public long getCurInstanceId() {
        return curInstanceId;
    }

    public void setCurInstanceId(final long curInstanceId) {
        this.curInstanceId = curInstanceId;
    }

    public long getLastAppliedId() {
        return lastAppliedId;
    }

    public void setLastAppliedId(final long lastAppliedId) {
        this.lastAppliedId = lastAppliedId;
    }

    public long getCheckpoint() {
        return checkpoint;
    }

    public void setCheckpoint(final long checkpoint) {
        this.checkpoint = checkpoint;
    }

    public boolean isGranted() {
        return granted;
    }

    @Override
    public String toString() {
        return "NewMasterRes{"
                + "curInstanceId=" + curInstanceId
                + ", lastAppliedId=" + lastAppliedId
                + ", checkpoint=" + checkpoint
                + ", granted=" + granted
                + '}';
    }

    public static final class Builder {
        private long curInstanceId;
        private long lastAppliedId;
        private long checkpoint;
        private boolean granted;

        private Builder() {
        }

        /**
         * aNewMasterRes.
         *
         * @return Builder
         */
        public static Builder aNewMasterRes() {
            return new Builder();
        }

        /**
         * curInstanceId.
         *
         * @param curInstanceId curInstanceId
         * @return Builder
         */
        public Builder curInstanceId(final long curInstanceId) {
            this.curInstanceId = curInstanceId;
            return this;
        }

        /**
         * lastAppliedId.
         *
         * @param lastAppliedId lastAppliedId
         * @return Builder
         */
        public Builder lastAppliedId(final long lastAppliedId) {
            this.lastAppliedId = lastAppliedId;
            return this;
        }

        /**
         * checkpoint.
         *
         * @param checkpoint checkpoint
         * @return Builder
         */
        public Builder checkpoint(final long checkpoint) {
            this.checkpoint = checkpoint;
            return this;
        }

        /**
         * granted.
         *
         * @param granted granted
         * @return Builder
         */
        public Builder granted(final boolean granted) {
            this.granted = granted;
            return this;
        }

        /**
         * build.
         *
         * @return NewMasterRes
         */
        public NewMasterRes build() {
            NewMasterRes newMasterRes = new NewMasterRes();
            newMasterRes.lastAppliedId = this.lastAppliedId;
            newMasterRes.curInstanceId = this.curInstanceId;
            newMasterRes.checkpoint = this.checkpoint;
            newMasterRes.granted = this.granted;
            return newMasterRes;
        }
    }
}
