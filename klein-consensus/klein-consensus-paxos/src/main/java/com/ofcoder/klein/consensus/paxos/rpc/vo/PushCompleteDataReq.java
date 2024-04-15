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
import java.util.List;
import java.util.Map;

import com.ofcoder.klein.consensus.paxos.Proposal;
import com.ofcoder.klein.storage.facade.Instance;
import com.ofcoder.klein.storage.facade.Snap;

/**
 * PushCompleteDataReq.
 *
 * @author 释慧利
 */
@Deprecated
public class PushCompleteDataReq implements Serializable {

    private Map<String, Snap> snaps;
    private List<Instance<Proposal>> confirmedInstances;

    public Map<String, Snap> getSnaps() {
        return snaps;
    }

    public List<Instance<Proposal>> getConfirmedInstances() {
        return confirmedInstances;
    }

    public static final class Builder {
        private Map<String, Snap> snaps;
        private List<Instance<Proposal>> confirmedInstances;

        private Builder() {
        }

        /**
         * aPushCompleteDataReq.
         *
         * @return Builder
         */
        public static Builder aPushCompleteDataReq() {
            return new Builder();
        }

        /**
         * set snaps.
         *
         * @param snaps snaps
         * @return Builder
         */
        public Builder snaps(final Map<String, Snap> snaps) {
            this.snaps = snaps;
            return this;
        }

        /**
         * set confirmedInstances.
         *
         * @param confirmedInstances confirmedInstances
         * @return Builder
         */
        public Builder confirmedInstances(final List<Instance<Proposal>> confirmedInstances) {
            this.confirmedInstances = confirmedInstances;
            return this;
        }

        /**
         * build.
         *
         * @return Builder
         */
        public PushCompleteDataReq build() {
            PushCompleteDataReq pushCompleteDataReq = new PushCompleteDataReq();
            pushCompleteDataReq.confirmedInstances = this.confirmedInstances;
            pushCompleteDataReq.snaps = this.snaps;
            return pushCompleteDataReq;
        }
    }
}
