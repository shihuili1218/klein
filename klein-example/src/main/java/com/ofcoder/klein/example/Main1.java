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
package com.ofcoder.klein.example;

import java.io.IOException;
import java.io.Serializable;

import com.google.common.collect.Lists;
import com.ofcoder.klein.Klein;
import com.ofcoder.klein.core.config.KleinProp;
import com.ofcoder.klein.rpc.facade.Endpoint;

/**
 * @author 释慧利
 */
public class Main1 {
    public static void main(String[] args) throws IOException {
        KleinProp prop1 = KleinProp.loadIfPresent();
        prop1.getConsensusProp().setMembers(Lists.newArrayList(new Endpoint("1", "127.0.0.1", 1218), new Endpoint("2", "127.0.0.1", 1219), new Endpoint("3", "127.0.0.1", 1220)));
        prop1.getConsensusProp().setSelf(new Endpoint("1", "127.0.0.1", 1218));
        prop1.getRpcProp().setPort(1218);

        Klein instance1 = Klein.getInstance();
        String key = "hello";
        String value = "klein";
        instance1.getCache().put(key, value);
        Serializable hello = instance1.getCache().get(key);
        assert value == hello;
        System.in.read();
    }
}
