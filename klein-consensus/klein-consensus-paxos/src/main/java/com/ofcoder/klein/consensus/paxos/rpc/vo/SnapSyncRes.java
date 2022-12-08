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
import java.util.Map;

import com.ofcoder.klein.storage.facade.Snap;

/**
 * @author 释慧利
 */
public class SnapSyncRes implements Serializable {
    private long checkpoint;
    private Map<String, Snap> images;

    public long getCheckpoint() {
        return checkpoint;
    }

    public void setCheckpoint(long checkpoint) {
        this.checkpoint = checkpoint;
    }

    public Map<String, Snap> getImages() {
        return images;
    }

    public void setImages(Map<String, Snap> images) {
        this.images = images;
    }

    public static final class Builder {
        private long checkpoint;
        private Map<String, Snap> images;

        private Builder() {
        }

        public static Builder aSnapSyncRes() {
            return new Builder();
        }

        public Builder checkpoint(long checkpoint) {
            this.checkpoint = checkpoint;
            return this;
        }

        public Builder images(Map<String, Snap> images) {
            this.images = images;
            return this;
        }

        public SnapSyncRes build() {
            SnapSyncRes snapSyncRes = new SnapSyncRes();
            snapSyncRes.images = this.images;
            snapSyncRes.checkpoint = this.checkpoint;
            return snapSyncRes;
        }
    }
}
