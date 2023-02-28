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
package com.ofcoder.klein.rpc.facade.config;

import com.ofcoder.klein.common.util.SystemPropertyUtil;

/**
 * rpc property.
 *
 * @author far.liu
 */
public class RpcProp {
    private int port = SystemPropertyUtil.getInt("klein.port", 1218);
    private int maxInboundMsgSize = SystemPropertyUtil.getInt("klein.rpc.max-inbound-size", 4 * 1024 * 1024);
    private int requestTimeout = SystemPropertyUtil.getInt("klein.rpc.request-timeout", 200);

    /**
     * get port.
     *
     * @return port
     */
    public int getPort() {
        return port;
    }

    /**
     * set port.
     *
     * @param port port
     */
    public void setPort(final int port) {
        this.port = port;
    }

    /**
     * get max inbound msg size.
     *
     * @return inbound msg size
     */
    public int getMaxInboundMsgSize() {
        return maxInboundMsgSize;
    }

    /**
     * set max inbound msg size.
     *
     * @param maxInboundMsgSize inbound msg size
     */
    public void setMaxInboundMsgSize(final int maxInboundMsgSize) {
        this.maxInboundMsgSize = maxInboundMsgSize;
    }

    public int getRequestTimeout() {
        return requestTimeout;
    }

    public void setRequestTimeout(final int requestTimeout) {
        this.requestTimeout = requestTimeout;
    }
}
