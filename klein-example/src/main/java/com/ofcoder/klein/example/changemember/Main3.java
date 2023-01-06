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
package com.ofcoder.klein.example.changemember;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.ofcoder.klein.Klein;
import com.ofcoder.klein.core.config.KleinProp;
import com.ofcoder.klein.rpc.facade.Endpoint;

/**
 * Main3: cluster member.
 *
 * @author 释慧利
 */
public class Main3 {
    private static final Logger LOG = LoggerFactory.getLogger(Main3.class);

    public static void main(final String[] args) throws IOException {
        System.setProperty("klein.id", "3");
        System.setProperty("klein.port", "1220");
        System.setProperty("klein.ip", "127.0.0.1");

        KleinProp prop3 = KleinProp.loadIfPresent();

        prop3.getConsensusProp().setMembers(
                Lists.newArrayList(
                        new Endpoint("1", "127.0.0.1", 1218),
                        new Endpoint("2", "127.0.0.1", 1219),
                        new Endpoint("3", "127.0.0.1", 1220)
                )
        );

        Klein instance = Klein.startup();
        System.in.read();
    }
}
