package com.ofcoder.klein.jepsen.control.jepsen.core;

import clojure.lang.RT;
import org.eclipse.jetty.util.StringUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JepsenExecutable {
    private final String timeLimit;
    private final String nodes;
    private final String username, password;
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
        this.client = new NoopClient();
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

    public JepsenExecutable addChecker(final String checkerName, final CheckerCallback callback) {
        if (checkerCallbacks == null)
            checkerCallbacks = new HashMap<>();
        checkerCallbacks.put(checkerName, callback);
        return this;
    }

    public JepsenExecutable addCheckers(final Map<String, CheckerCallback> callbacks) {
        if (checkerCallbacks == null)
            checkerCallbacks = new HashMap<>();
        for (final Map.Entry<String, CheckerCallback> entry : callbacks.entrySet())
            checkerCallbacks.put(entry.getKey(), entry.getValue());
        return this;
    }

    public JepsenExecutable addNemesis(final NemesisCallback callback) {
        if (callback == null) return this;
        if (nemesisCallbacks == null)
            nemesisCallbacks = new ArrayList<>();
        nemesisCallbacks.add(callback);
        return this;
    }

    public JepsenExecutable setNemesisOps(final List<String> nemesisOps) {
        this.nemesisOps = nemesisOps;
        return this;
    }

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
            System.out.println("Found exception " + exc);
        }
    }
}
