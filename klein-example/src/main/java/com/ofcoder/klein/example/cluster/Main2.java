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
package com.ofcoder.klein.example.cluster;

import java.io.IOException;

import com.google.common.collect.Lists;
import com.ofcoder.klein.Klein;
import com.ofcoder.klein.core.config.KleinProp;
import com.ofcoder.klein.rpc.facade.Endpoint;
import com.ofcoder.klein.rpc.facade.util.RpcUtil;

/**
 * @author 释慧利
 */
public class Main2 {
    public static void main(String[] args) throws IOException {
        System.setProperty("klein.id", "2");
        System.setProperty("klein.port", "1219");

        KleinProp prop2 = KleinProp.loadIfPresent();

        prop2.getConsensusProp().setMembers(
                Lists.newArrayList(
                        new Endpoint("1", RpcUtil.getLocalIp(), 1218),
                        new Endpoint("2", RpcUtil.getLocalIp(), 1219),
                        new Endpoint("3", RpcUtil.getLocalIp(), 1220)
                )
        );
//        prop2.getConsensusProp().setSelf(new Endpoint("2", "127.0.0.1", 1219));
//        prop2.getRpcProp().setPort(1219);

        Klein instance2 = Klein.getInstance();

        System.in.read();
    }
}
