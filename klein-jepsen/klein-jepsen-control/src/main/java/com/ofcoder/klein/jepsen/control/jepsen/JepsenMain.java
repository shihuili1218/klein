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
package com.ofcoder.klein.jepsen.control.jepsen;

import com.ofcoder.klein.jepsen.control.jepsen.core.JepsenConfig;
import com.ofcoder.klein.jepsen.control.jepsen.core.JepsenExecutable;
import com.ofcoder.klein.jepsen.control.jepsen.core.NemesisCallback;
import com.ofcoder.klein.jepsen.control.jepsen.core.impl.KleinClient;
import com.ofcoder.klein.jepsen.control.jepsen.core.impl.NoopChecker;
import com.ofcoder.klein.jepsen.control.jepsen.core.impl.NoopDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * jepsen starter.
 */
public class JepsenMain {
    private static final Logger LOG = LoggerFactory.getLogger(JepsenMain.class);

    public static void main(final String[] args) {
        final JepsenConfig config = (new JepsenConfig()).add(JepsenConfig.NODES, "1:172.22.0.76:1218,2:172.22.0.76:1219,3:172.22.0.76:1220,4:172.22.0.76:1221,5:172.22.0.76:1222")
                .add(JepsenConfig.USERNAME, "root")
                .add(JepsenConfig.PASSWORD, "123456")
                .add(JepsenConfig.NEMESIS, "partition-majorities-ring")
                .add(JepsenConfig.TEST_NAME, "klein_test")
                .add(JepsenConfig.TIME_LIMIT, "13")
                .add(JepsenConfig.CLIENT_OP_WAIT_TIME, "1")
                .add(JepsenConfig.NEMESIS_OP_WAIT_TIME, "3");

        final List<String> nemesisOps = new ArrayList<>();
        nemesisOps.add("Noop start");
        nemesisOps.add("start");
        nemesisOps.add("Noop end");
        nemesisOps.add("stop");

        (new JepsenExecutable(config)).setClient(new KleinClient())
                .setDatabase(new NoopDatabase())
                .addChecker("perf", null)
                .addChecker("noop", new NoopChecker())
                .addNemesis(new Nemesis())
                .setNemesisOps(nemesisOps)
                .launchTest();
    }

    public static class Nemesis implements NemesisCallback {
        public Nemesis() {
        }

        @Override
        public void setup() {
            LOG.info("Nemesis setup here.");
        }

        @Override
        public void invokeOp(final String op) {
            LOG.info("Executing " + op + ".");
        }

        @Override
        public void teardown() {
            LOG.info("Nemesis was teardown here.");
        }

        @Override
        public List<String> getPossibleOps() {
            final List<String> res = new ArrayList<>();
            res.add("Noop start");
            res.add("Noop end");
            return res;
        }
    }

}
