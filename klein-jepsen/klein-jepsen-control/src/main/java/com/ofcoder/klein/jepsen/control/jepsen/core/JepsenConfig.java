package com.ofcoder.klein.jepsen.control.jepsen.core;

import java.util.HashMap;
import java.util.Map;

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

	public JepsenConfig add(String property, String value) {
		properties.put(property, value);
		return this;
	}

	public Map<String, String> getConfig() {
		return properties;
	}
}
