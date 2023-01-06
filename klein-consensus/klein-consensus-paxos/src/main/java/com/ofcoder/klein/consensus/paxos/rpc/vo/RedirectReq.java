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
import java.util.Set;

import com.ofcoder.klein.rpc.facade.Endpoint;

/**
 * RedirectReq.
 *
 * @author 释慧利
 */
public class RedirectReq implements Serializable {
    public static final byte CHANGE_MEMBER = 0x00;
    public static final byte TRANSACTION_REQUEST = 0x01;

    private byte redirect;
    private byte changeOp;
    private Set<Endpoint> changeTarget;

    public byte getRedirect() {
        return redirect;
    }

    public byte getChangeOp() {
        return changeOp;
    }

    public Set<Endpoint> getChangeTarget() {
        return changeTarget;
    }

    public static final class Builder {
        private byte redirect;
        private byte changeOp;
        private Set<Endpoint> changeTarget;

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
         * redirect.
         *
         * @param redirect redirect
         * @return Builder
         */
        public Builder redirect(final byte redirect) {
            this.redirect = redirect;
            return this;
        }

        /**
         * changeOp.
         *
         * @param changeOp changeOp
         * @return Builder
         */
        public Builder changeOp(final byte changeOp) {
            this.changeOp = changeOp;
            return this;
        }

        /**
         * changeTarget.
         *
         * @param changeTarget changeTarget
         * @return Builder
         */
        public Builder changeTarget(final Set<Endpoint> changeTarget) {
            this.changeTarget = changeTarget;
            return this;
        }

        /**
         * build.
         *
         * @return RedirectReq
         */
        public RedirectReq build() {
            RedirectReq redirectReq = new RedirectReq();
            redirectReq.changeOp = this.changeOp;
            redirectReq.changeTarget = this.changeTarget;
            redirectReq.redirect = this.redirect;
            return redirectReq;
        }
    }
}
