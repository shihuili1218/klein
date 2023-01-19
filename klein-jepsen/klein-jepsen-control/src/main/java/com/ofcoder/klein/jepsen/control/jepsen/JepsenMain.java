package com.ofcoder.klein.jepsen.control.jepsen;

import com.ofcoder.klein.jepsen.control.jepsen.core.*;

import java.util.ArrayList;
import java.util.List;

public class JepsenMain {
    public static class NoopChecker implements CheckerCallback {
        public NoopChecker() {
        }

        public void check(Object test, Object history) {
            System.out.println("Checking stuff");
        }
    }

    public static class Nemesis implements NemesisCallback {
        public Nemesis() {
        }

        public void setup() {
            System.out.println("Nemesis setup here.");
        }

        public void invokeOp(String op) {
            System.out.println("Executing " + op + ".");
        }

        public void teardown() {
            System.out.println("Nemesis was teardown here.");
        }

        public List<String> getPossibleOps() {
            final List<String> res = new ArrayList<>();
            res.add("Noop start");
            res.add("Noop end");
            return res;
        }
    }

    public static void main(String[] args) {
        JepsenConfig config = (new JepsenConfig()).add(JepsenConfig.NODES, "")
                .add(JepsenConfig.USERNAME, "root")
                .add(JepsenConfig.PASSWORD, "root")
                .add(JepsenConfig.NEMESIS, "partition-majorities-ring")
                .add(JepsenConfig.TEST_NAME, "sample_test")
                .add(JepsenConfig.TIME_LIMIT, "13")
                .add(JepsenConfig.CLIENT_OP_WAIT_TIME, "1")
                .add(JepsenConfig.NEMESIS_OP_WAIT_TIME, "3");

        final List<String> nemesisOps = new ArrayList<>();
        nemesisOps.add("Noop start");
        nemesisOps.add("start");
        nemesisOps.add("Noop end");
        nemesisOps.add("stop");

        (new JepsenExecutable(config)).setClient(new NoopClient())
                .setDatabase(new NoopDatabase())
                .addChecker("perf", null)
                .addChecker("noop", new NoopChecker())
                .addNemesis(new Nemesis())
                .setNemesisOps(nemesisOps)
                .launchTest();
    }
}
