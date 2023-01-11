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

import com.ofcoder.klein.consensus.facade.Result;

/**
 * Redirect Resp.
 *
 * @author 释慧利
 */
public class RedirectRes implements Serializable {
    private boolean changeResult;
    private Result<Serializable> proposeResult;

    public boolean isChangeResult() {
        return changeResult;
    }

    public Result<Serializable> getProposeResult() {
        return proposeResult;
    }

    @Override
    public String toString() {
        return "RedirectRes{"
                + "changeResult=" + changeResult
                + ", proposeResult=" + proposeResult
                + '}';
    }

    public static final class Builder {
        private boolean changeResult;
        private Result<Serializable> proposeResult;

        private Builder() {
        }

        /**
         * aRedirectResp.
         *
         * @return Builder
         */
        public static Builder aRedirectResp() {
            return new Builder();
        }

        /**
         * changeResult.
         *
         * @param changeResult changeResult
         * @return Builder
         */
        public Builder changeResult(final boolean changeResult) {
            this.changeResult = changeResult;
            return this;
        }

        /**
         * proposeResult.
         *
         * @param proposeResult proposeResult
         * @return Builder
         */
        public Builder proposeResult(final Result<Serializable> proposeResult) {
            this.proposeResult = proposeResult;
            return this;
        }

        /**
         * build.
         *
         * @return RedirectResp
         */
        public RedirectRes build() {
            RedirectRes redirectRes = new RedirectRes();
            redirectRes.changeResult = this.changeResult;
            redirectRes.proposeResult = this.proposeResult;
            return redirectRes;
        }
    }
}
