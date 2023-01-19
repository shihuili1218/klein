package com.ofcoder.klein.jepsen.control.jepsen.core;

public class NoopDatabase implements Database {
    public NoopDatabase() {}

    public Object setUpDatabase(String node) {
        System.out.println("Setup DB");
        return "NOOP_DB";
    }

    public void teardownDatabase(String node) {
        System.out.println("Torndown DB");
    }
}
