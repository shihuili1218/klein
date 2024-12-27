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
package com.ofcoder.klein.rpc.facade;

import java.util.Arrays;
import java.util.Objects;

/**
 * Invoke Param.
 *
 * @author far.liu
 */
public class InvokeParam {
    private String service;
    private String method;
    private byte[] data;

    /**
     * get service name.
     *
     * @return service name
     */
    public String getService() {
        return service;
    }

    /**
     * set service name.
     *
     * @param service service name
     */
    public void setService(final String service) {
        this.service = service;
    }

    /**
     * get method name.
     *
     * @return method name
     */
    public String getMethod() {
        return method;
    }

    /**
     * set method name.
     *
     * @param method method name
     */
    public void setMethod(final String method) {
        this.method = method;
    }

    /**
     * get invoke data.
     *
     * @return invoke data
     */
    public byte[] getData() {
        return data;
    }

    /**
     * set data.
     *
     * @param data invoke data
     */
    public void setData(final byte[] data) {
        this.data = data;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        InvokeParam that = (InvokeParam) o;
        return Objects.equals(service, that.service) && Objects.equals(method, that.method) && Arrays.equals(data, that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(service, method, Arrays.hashCode(data));
    }

    @Override
    public String toString() {
        return "InvokeParam{"
            + "service='" + service + '\''
            + ", method='" + method + '\''
            + ", data=" + data.length
            + '}';
    }

    public static final class Builder {
        private String service;
        private String method;
        private byte[] data;

        private Builder() {
        }

        /**
         * anInvokeParam.
         *
         * @return Builder
         */
        public static Builder anInvokeParam() {
            return new Builder();
        }

        /**
         * service.
         *
         * @param service service
         * @return Builder
         */
        public Builder service(final String service) {
            this.service = service;
            return this;
        }

        /**
         * method.
         *
         * @param method method
         * @return Builder
         */
        public Builder method(final String method) {
            this.method = method;
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
         * build object.
         *
         * @return InvokeParam object
         */
        public InvokeParam build() {
            InvokeParam invokeParam = new InvokeParam();
            invokeParam.setService(service);
            invokeParam.setMethod(method);
            invokeParam.setData(data);
            return invokeParam;
        }
    }
}
