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
package com.ofcoder.klein.jepsen.control.jepsen.core;

import java.util.HashMap;
import java.util.Map;

/**
 * JepsenConfig.
 */
public class JepsenConfig {
    public static final String TEST_NAME = "test.name";
    public static final String TIME_LIMIT = "time.limit";
    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";
    public static final String NODES = "nodes";
    public static final String NEMESIS = "nemesis";
    public static final String CLIENT_OP_WAIT_TIME = "client.op.wait.time";
    public static final String NEMESIS_OP_WAIT_TIME = "nemesis.op.wait.time";

    private final Map<String, String> properties;

    public JepsenConfig() {
        properties = new HashMap<>();
        properties.put(JepsenConfig.NODES, "localhost:9092");
        properties.put(JepsenConfig.USERNAME, "root");
        properties.put(JepsenConfig.NEMESIS, "noop");
        properties.put(JepsenConfig.TIME_LIMIT, "90");
        properties.put(JepsenConfig.TEST_NAME, "random-test");
        properties.put(JepsenConfig.PASSWORD, "root");
        properties.put(JepsenConfig.CLIENT_OP_WAIT_TIME, "1");
        properties.put(JepsenConfig.NEMESIS_OP_WAIT_TIME, "30");
    }

    /**
     * add config.
     *
     * @param property key
     * @param value    v
     * @return this object
     */
    public JepsenConfig add(final String property, final String value) {
        properties.put(property, value);
        return this;
    }

    /**
     * get all config.
     *
     * @return all config
     */
    public Map<String, String> getConfig() {
        return properties;
    }
}
