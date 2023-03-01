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

import java.nio.ByteBuffer;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ofcoder.klein.KleinProp;
import com.ofcoder.klein.common.serialization.Hessian2Util;
import com.ofcoder.klein.common.util.SumcheckUtil;
import com.ofcoder.klein.jepsen.server.rpc.GetReq;
import com.ofcoder.klein.jepsen.server.rpc.PutReq;
import com.ofcoder.klein.rpc.facade.Endpoint;
import com.ofcoder.klein.rpc.facade.InvokeParam;
import com.ofcoder.klein.rpc.facade.RpcProcessor;
import com.ofcoder.klein.rpc.grpc.GrpcClient;

/**
 * jepsen‘s client.
 *
 * @author 释慧利
 */
public class JepsenClient {

    static final Logger LOG = LoggerFactory.getLogger(KleinServer.class);

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
    private final Endpoint endpoint;
    private final GrpcClient client;

    public JepsenClient(final String node) {
        String id = StringUtils.remove(node, "n");

        endpoint = new Endpoint(id, node, 1218);
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
        req.setSeq(SumcheckUtil.md5(RandomStringUtils.random(32)));
        req.setKey(key);

        InvokeParam param = InvokeParam.Builder.anInvokeParam()
                .service(req.getClass().getSimpleName())
                .method(RpcProcessor.KLEIN)
                .data(ByteBuffer.wrap(Hessian2Util.serialize(req))).build();

        boolean o = client.sendRequestSync(endpoint, param, 1000);
        if (!o) {
            throw new IllegalArgumentException("seq: " + req.getSeq() + "wirte: " + value + " on node: " + endpoint.getId() + ", occur proposal conflict");
        }
        return true;
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
        req.setSeq(SumcheckUtil.md5(RandomStringUtils.random(32)));

        InvokeParam param = InvokeParam.Builder.anInvokeParam()
                .service(req.getClass().getSimpleName())
                .method(RpcProcessor.KLEIN)
                .data(ByteBuffer.wrap(Hessian2Util.serialize(req))).build();

        Object o = client.sendRequestSync(endpoint, param, 1000);
        if (o == null) {
            throw new IllegalArgumentException("seq: " + req.getSeq() + "get: " + key + " on node: " + endpoint.getId() + ", result is null");
        }
        return o;
    }

}
