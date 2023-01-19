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
package com.ofcoder.klein.jepsen.control.jepsen.core.impl;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ofcoder.klein.core.config.KleinProp;
import com.ofcoder.klein.jepsen.control.jepsen.core.Client;
import com.ofcoder.klein.jepsen.server.rpc.PutReq;
import com.ofcoder.klein.rpc.facade.Endpoint;
import com.ofcoder.klein.rpc.grpc.GrpcClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

/**
 * Klein Client For Jepsen Test.
 */
public class KleinClient implements Client {
    private static final Logger LOG = LoggerFactory.getLogger(KleinClient.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
    private final GrpcClient client;
    private Endpoint endpoint;

    public KleinClient() {
        KleinProp kleinProp = KleinProp.loadIfPresent();

        client = new GrpcClient();
        client.init(kleinProp.getRpcProp());
    }

    @Override
    public void teardownClient(final Object args) {
        client.closeConnection(this.endpoint);
        LOG.info("Torndown client " + args);
    }

    @Override
    public Object invokeClient(final Object args, final String opName, final Object inputValue) {
        LOG.info("Invoked client op " + opName + " with input value " + inputValue);
        PutReq req = new PutReq();
        req.setData(String.valueOf(inputValue));
        req.setKey(opName);
        boolean o = client.sendRequestSync(endpoint, req, 1000);
        LOG.info("Invoked client op " + opName + " with input value " + inputValue + ", result: " + o);
        return o;
    }

    @Override
    public Object openClient(final String node) {
        String id = node.replace("n", "");
        this.endpoint = new Endpoint(id, node, 1218);
        this.client.createConnection(this.endpoint);
        return client;
    }

    @Override
    public String generateOp() {
        return "dummyOp";
    }

    @Override
    public Object getValue(final String opName) {
        return (new Random()).nextInt(100);
    }
}
