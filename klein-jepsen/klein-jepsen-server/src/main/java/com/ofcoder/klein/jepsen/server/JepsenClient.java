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
package com.ofcoder.klein.jepsen.server;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ofcoder.klein.core.config.KleinProp;
import com.ofcoder.klein.jepsen.server.rpc.GetReq;
import com.ofcoder.klein.jepsen.server.rpc.PutReq;
import com.ofcoder.klein.rpc.facade.Endpoint;
import com.ofcoder.klein.rpc.facade.util.RpcUtil;
import com.ofcoder.klein.rpc.grpc.GrpcClient;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * jepsen‘s client.
 *
 * @author 释慧利
 */
public class JepsenClient {

    static final Logger LOG = LoggerFactory.getLogger(KleinServer.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
    private final List<Endpoint> endpoints;
    private final GrpcClient client;

    public JepsenClient(final String clusterInfo) {
        LOG.info("clusterInfo: {}", clusterInfo);
        KleinProp kleinProp = KleinProp.loadIfPresent();

        endpoints = parseMember(clusterInfo);
        client = new GrpcClient();
        client.init(kleinProp.getRpcProp());
        for (Endpoint endpoint : endpoints) {
            client.createConnection(endpoint);
        }
    }

    /**
     * put.
     *
     * @param value value
     * @return result
     */
    public boolean put(final Integer value) {
        final String key = "def";
        LOG.info("call klein-server, op: put, key: {}, val: {}", key, value);
        PutReq req = new PutReq();
        req.setData(value);
        req.setKey(key);
        boolean o = client.sendRequestSync(endpoints.get(0), req, 1000);
        LOG.info("put result: {}", o);
        return o;
    }


    /**
     * get.
     *
     * @return result
     */
    public boolean get() {
        final String key = "def";
        GetReq req = new GetReq();
        req.setKey(key);
        boolean o = client.sendRequestSync(endpoints.get(0), req, 1000);
        LOG.info("get result: {}", o);
        return o;
    }

    private List<Endpoint> parseMember(final String members) {
        List<Endpoint> endpoints = new ArrayList<>();
        if (StringUtils.isEmpty(members)) {
            return endpoints;
        }
        for (String item : StringUtils.split(members, ";")) {
            if (StringUtils.isEmpty(item)) {
                continue;
            }
            endpoints.add(RpcUtil.parseEndpoint(item));
        }
        return endpoints;
    }
}
