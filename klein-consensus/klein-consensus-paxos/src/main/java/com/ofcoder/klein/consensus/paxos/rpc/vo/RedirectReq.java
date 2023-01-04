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

import com.ofcoder.klein.rpc.facade.Endpoint;

/**
 * @author 释慧利
 */
public class RedirectReq implements Serializable {
    public static final byte CHANGE_MEMBER = 0x00;
    public static final byte TRANSACTION_REQUEST = 0x01;

    private byte redirect;
    private byte changeOp;
    private Endpoint changeTarget;

    public byte getRedirect() {
        return redirect;
    }

    public byte getChangeOp() {
        return changeOp;
    }

    public Endpoint getChangeTarget() {
        return changeTarget;
    }

    public static final class Builder {
        private byte redirect;
        private byte changeOp;
        private Endpoint changeTarget;

        private Builder() {
        }

        public static Builder aRedirectReq() {
            return new Builder();
        }

        public Builder redirect(byte redirect) {
            this.redirect = redirect;
            return this;
        }

        public Builder changeOp(byte changeOp) {
            this.changeOp = changeOp;
            return this;
        }

        public Builder changeTarget(Endpoint changeTarget) {
            this.changeTarget = changeTarget;
            return this;
        }

        public RedirectReq build() {
            RedirectReq redirectReq = new RedirectReq();
            redirectReq.changeOp = this.changeOp;
            redirectReq.changeTarget = this.changeTarget;
            redirectReq.redirect = this.redirect;
            return redirectReq;
        }
    }
}
