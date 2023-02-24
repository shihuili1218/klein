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
 * pre elect response.
 *
 * @author 释慧利
 */
public class PreElectRes implements Serializable {
    private Endpoint master;

    public Endpoint getMaster() {
        return master;
    }

    public static final class Builder {
        private Endpoint master;

        private Builder() {
        }

        /**
         * aPreElectRes.
         *
         * @return Builder
         */
        public static Builder aPreElectRes() {
            return new Builder();
        }

        /**
         * master.
         *
         * @param master master
         * @return Builder
         */
        public Builder master(final Endpoint master) {
            this.master = master;
            return this;
        }

        /**
         * build.
         *
         * @return PreElectRes
         */
        public PreElectRes build() {
            PreElectRes preElectRes = new PreElectRes();
            preElectRes.master = this.master;
            return preElectRes;
        }
    }
}
