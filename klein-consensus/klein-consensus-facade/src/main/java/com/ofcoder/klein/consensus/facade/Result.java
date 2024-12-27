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
package com.ofcoder.klein.consensus.facade;

import java.io.Serializable;

/**
 * propose Result.
 *
 * @author 释慧利
 */
public class Result implements Serializable {

    private State state;
    private byte[] data;

    public enum State {
        /**
         * negotiation done, and consensus data eq client’s data.
         */
        SUCCESS,
        /**
         * negotiation done, and consensus data not eq client’s data.
         */
        FAILURE,
        /**
         * negotiation unknown.
         */
        UNKNOWN;
    }

    public State getState() {
        return state;
    }

    public void setState(final State state) {
        this.state = state;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(final byte[] data) {
        this.data = data;
    }

    public static final class Builder {
        private State state;
        private byte[] data;

        private Builder() {
        }

        /**
         * aResult.
         *
         * @return Builder
         */
        public static Builder aResult() {
            return new Builder();
        }

        /**
         * state.
         *
         * @param state state
         * @return Builder
         */
        public Builder state(final State state) {
            this.state = state;
            return this;
        }

        /**
         * data.
         *
         * @param data data
         * @return Builder
         */
        public Builder data(final byte[] data) {
            this.data = data;
            return this;
        }

        /**
         * build a result object.
         *
         * @return Result object
         */
        public Result build() {
            Result result = new Result();
            result.setState(state);
            result.setData(data);
            return result;
        }
    }
}
