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

import clojure.lang.RT;
import com.ofcoder.klein.jepsen.control.jepsen.JepsenMain;
import com.ofcoder.klein.jepsen.control.jepsen.core.impl.KleinClient;
import com.ofcoder.klein.jepsen.control.jepsen.core.impl.NoopDatabase;
import org.eclipse.jetty.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * JepsenExecutable.
 */
public class JepsenExecutable {
    private static final Logger LOG = LoggerFactory.getLogger(JepsenMain.class);

    private final String timeLimit;
    private final String nodes;
    private final String username;
    private final String password;
    private final long nemesisOpGapTime;
    private final long clientOpGapTime;
    private Client client;
    private Database database;
    private Map<String, CheckerCallback> checkerCallbacks;
    private final String testName;
    private final String nemesis;
    private ArrayList<NemesisCallback> nemesisCallbacks;
    private List<String> nemesisOps;

    public JepsenExecutable(final String nodes, final String username, final String password, final long timeLimit, final Client client) {
        this.nodes = nodes;
        this.username = username;
        this.password = password;
        this.timeLimit = Long.toString(timeLimit);
        this.client = client;
        this.checkerCallbacks = null;
        this.testName = "test";
        this.nemesis = "partition-random-halves";
        this.database = new NoopDatabase();
        this.nemesisCallbacks = null;
        this.nemesisOpGapTime = 30;
        this.clientOpGapTime = 1;
    }

    public JepsenExecutable(final JepsenConfig config) {
        final Map<String, String> properties = config.getConfig();
        this.nodes = properties.get(JepsenConfig.NODES);
        this.username = properties.get(JepsenConfig.USERNAME);
        this.password = properties.get(JepsenConfig.PASSWORD);
        this.timeLimit = properties.get(JepsenConfig.TIME_LIMIT);
        this.testName = properties.get(JepsenConfig.TEST_NAME);
        this.checkerCallbacks = null;
        this.client = new KleinClient();
        this.database = new NoopDatabase();
        this.nemesis = properties.get(JepsenConfig.NEMESIS);
        this.nemesisCallbacks = null;
        this.nemesisOpGapTime = Long.parseLong(properties.get(JepsenConfig.NEMESIS_OP_WAIT_TIME));
        this.clientOpGapTime = Long.parseLong(properties.get(JepsenConfig.CLIENT_OP_WAIT_TIME));
    }

    public JepsenExecutable setClient(final Client client) {
        this.client = client;
        return this;
    }

    public JepsenExecutable setDatabase(final Database database) {
        this.database = database;
        return this;
    }

    /**
     * add checker.
     *
     * @param checkerName key
     * @param callback    check callback
     * @return this object
     */
    public JepsenExecutable addChecker(final String checkerName, final CheckerCallback callback) {
        if (checkerCallbacks == null) {
            checkerCallbacks = new HashMap<>();
        }
        checkerCallbacks.put(checkerName, callback);
        return this;
    }

    /**
     * batch add checker.
     *
     * @param callbacks all checker
     * @return this object
     */
    public JepsenExecutable addCheckers(final Map<String, CheckerCallback> callbacks) {
        if (checkerCallbacks == null) {
            checkerCallbacks = new HashMap<>();
        }
        for (final Map.Entry<String, CheckerCallback> entry : callbacks.entrySet()) {
            checkerCallbacks.put(entry.getKey(), entry.getValue());
        }
        return this;
    }

    /**
     * add nemesis.
     *
     * @param callback nemesis callback
     * @return this object
     */
    public JepsenExecutable addNemesis(final NemesisCallback callback) {
        if (callback == null) {
            return this;
        }
        if (nemesisCallbacks == null) {
            nemesisCallbacks = new ArrayList<>();
        }
        nemesisCallbacks.add(callback);
        return this;
    }

    public JepsenExecutable setNemesisOps(final List<String> nemesisOps) {
        this.nemesisOps = nemesisOps;
        return this;
    }

    /**
     * start jepsen test.
     */
    public void launchTest() {
        try {
            RT.loadResourceScript("jepsen/interfaces/main.clj", true);
            String[] args;
            if (StringUtil.isNotBlank(nodes)) {
                args = new String[]{"test", "--nodes", nodes, "--username", username, "--password", password, "--time-limit", timeLimit};
            } else {
                args = new String[]{"test", "--username", username, "--password", password, "--time-limit", timeLimit};
            }
            RT.var("jepsen.interfaces", "setTestName").invoke(testName);
            RT.var("jepsen.interfaces", "setClientOpWaitTime").invoke(clientOpGapTime);
            RT.var("jepsen.interfaces", "setClient").invoke(client);
            RT.var("jepsen.interfaces", "setDatabase").invoke(database);
            RT.var("jepsen.interfaces", "setNemesis").invoke(nemesis);
            RT.var("jepsen.interfaces", "setCheckerCallbacks").invoke(checkerCallbacks);
            RT.var("jepsen.interfaces", "setNemesisOps").invoke(nemesisOps, nemesisOpGapTime);
            RT.var("jepsen.interfaces", "setNemesisCallbacks").invoke(nemesisCallbacks);
            RT.var("jepsen.interfaces", "main").invoke(args);
        } catch (IOException exc) {
            LOG.info("Found exception " + exc);
        }
    }
}
