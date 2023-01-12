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
package com.ofcoder.klein.example.nwr;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ofcoder.klein.Klein;
import com.ofcoder.klein.core.config.KleinProp;

/**
 * Main2: cluster member.
 *
 * @author 释慧利
 */
public class Main2 {
    private static final Logger LOG = LoggerFactory.getLogger(Main2.class);

    public static void main(final String[] args) throws IOException {
        System.setProperty("klein.id", "2");
        System.setProperty("klein.port", "1219");
        System.setProperty("klein.ip", "127.0.0.1");
        System.setProperty("klein.members", "1:127.0.0.1:1218;2:127.0.0.1:1219;3:127.0.0.1:1220");
        System.setProperty("klein.consensus.nwr", "fastWrite");

        KleinProp prop2 = KleinProp.loadIfPresent();

//        prop2.getConsensusProp().setSelf(new Endpoint("2", "127.0.0.1", 1219));
//        prop2.getRpcProp().setPort(1219);

        Klein instance2 = Klein.startup();

        System.in.read();
    }
}
