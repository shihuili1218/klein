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

import com.ofcoder.klein.rpc.facade.Endpoint;

/**
 * ChangeMemberReq.
 *
 * @author 释慧利
 */
public class ChangeMemberReq implements Serializable {

    private byte op;
    private List<Endpoint> changeTarget;

    public byte getOp() {
        return op;
    }

    public List<Endpoint> getChangeTarget() {
        return changeTarget;
    }

    public static final class Builder {
        private byte op;
        private List<Endpoint> changeTarget;

        private Builder() {
        }

        /**
         * aRedirectReq.
         *
         * @return Builder
         */
        public static Builder aRedirectReq() {
            return new Builder();
        }

        /**
         * op.
         *
         * @param op op
         * @return Builder
         */
        public Builder changeOp(final byte op) {
            this.op = op;
            return this;
        }

        /**
         * changeTarget.
         *
         * @param changeTarget changeTarget
         * @return Builder
         */
        public Builder changeTarget(final List<Endpoint> changeTarget) {
            this.changeTarget = changeTarget;
            return this;
        }

        /**
         * build.
         *
         * @return RedirectReq
         */
        public ChangeMemberReq build() {
            ChangeMemberReq redirectReq = new ChangeMemberReq();
            redirectReq.changeTarget = this.changeTarget;
            return redirectReq;
        }
    }
}
