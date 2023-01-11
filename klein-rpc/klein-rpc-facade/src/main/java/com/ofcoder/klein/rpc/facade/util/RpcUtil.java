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
package com.ofcoder.klein.rpc.facade.util;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.commons.lang3.StringUtils;

import com.ofcoder.klein.rpc.facade.Endpoint;

/**
 * Rpc Util.
 *
 * @author 释慧利
 */
public class RpcUtil {
    public static final String IP_ANY = "0.0.0.0";

    /**
     * get local ip.
     *
     * @return self ip
     */
    public static String getLocalIp() {
        try {
            InetAddress address = InetAddress.getLocalHost();
            return address.getHostAddress();
        } catch (UnknownHostException e) {
            return IP_ANY;
        }
    }

    /**
     * parse endpoint by <code>s</code>.
     *
     * @param s e.g. 127.0.0.1:1218
     * @return endpoint
     */
    public static Endpoint parseEndpoint(final String s) {
        if (StringUtils.isEmpty(s)) {
            throw new IllegalArgumentException("parse Endpoint, but address is empty.");
        }
        final String[] tmps = StringUtils.split(s, ":");
        if (tmps.length != 2) {
            throw new IllegalArgumentException(String.format("parse Endpoint, but address: %s is invalid, e.g. ip:port", s));
        }
        try {
            final int port = Integer.parseInt(tmps[1]);
            return new Endpoint(null, tmps[0], port);
        } catch (final Exception e) {
            throw new IllegalArgumentException(String.format("parse Endpoint, address: %s, error: %s.", s, e.getMessage()), e);
        }
    }

}
