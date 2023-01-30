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
import com.google.common.collect.ImmutableMap;
import com.ofcoder.klein.common.serialization.Hessian2Util;
import com.ofcoder.klein.core.config.KleinProp;
import com.ofcoder.klein.jepsen.server.rpc.GetReq;
import com.ofcoder.klein.jepsen.server.rpc.PutReq;
import com.ofcoder.klein.rpc.facade.Endpoint;
import com.ofcoder.klein.rpc.facade.InvokeParam;
import com.ofcoder.klein.rpc.facade.RpcProcessor;
import com.ofcoder.klein.rpc.facade.util.RpcUtil;
import com.ofcoder.klein.rpc.grpc.GrpcClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Map;

/**
 * jepsen‘s client.
 *
 * @author 释慧利
 */
public class JepsenClient {

    static final Logger LOG = LoggerFactory.getLogger(KleinServer.class);
    private static final Map<String, String> ROUTER = ImmutableMap.of(
            "n1", "1:172.22.0.102:1218",
            "n2", "2:172.22.0.103:1218",
            "n3", "3:172.22.0.104:1218",
            "n4", "4:172.22.0.106:1218",
            "n5", "5:172.22.0.113:1218"
    );
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
    private final Endpoint endpoint;
    private final GrpcClient client;

    public JepsenClient(final String node) {
        endpoint = RpcUtil.parseEndpoint(ROUTER.get(node));
        LOG.info("node: {}", endpoint);
        KleinProp kleinProp = KleinProp.loadIfPresent();

        client = new GrpcClient();
        client.init(kleinProp.getRpcProp());
        client.createConnection(endpoint);
    }

    /**
     * put.
     *
     * @param value value
     * @return result
     */
    public boolean put(final Integer value) {
        final String key = "def";
        PutReq req = new PutReq();
        req.setData(value);
        req.setKey(key);

        InvokeParam param = InvokeParam.Builder.anInvokeParam()
                .service(req.getClass().getSimpleName())
                .method(RpcProcessor.KLEIN)
                .data(ByteBuffer.wrap(Hessian2Util.serialize(req))).build();

        boolean o = client.sendRequestSync(endpoint, param, 1000);
        LOG.info("call klein-server, op: put, key: {}, val: {}, result: {}", key, value, o);
        return o;
    }


    /**
     * get.
     *
     * @return result
     */
    public Object get() {
        final String key = "def";
        GetReq req = new GetReq();
        req.setKey(key);

        InvokeParam param = InvokeParam.Builder.anInvokeParam()
                .service(req.getClass().getSimpleName())
                .method(RpcProcessor.KLEIN)
                .data(ByteBuffer.wrap(Hessian2Util.serialize(req))).build();

        Integer o = client.sendRequestSync(endpoint, param, 1000);
        LOG.info("get result: {}", o);
        return o;
    }

}
