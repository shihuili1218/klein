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
package com.ofcoder.klein.consensus.paxos.rpc.vo;

import java.io.Serializable;

import com.ofcoder.klein.consensus.facade.config.ConsensusProp;
import com.ofcoder.klein.rpc.facade.Endpoint;

/**
 * if {@link ConsensusProp#elastic} is true, then the member will automatically join the cluster when it starts up,
 * and automatically exit the cluster when shutdown.
 */
public class ElasticReq implements Serializable {
    public static final byte SHUTDOWN = 0x00;
    public static final byte LAUNCH = 0x01;

    private Endpoint endpoint;
    private byte op;

    public Endpoint getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(final Endpoint endpoint) {
        this.endpoint = endpoint;
    }

    public byte getOp() {
        return op;
    }

    public void setOp(final byte op) {
        this.op = op;
    }

    @Override
    public String toString() {
        if (op == SHUTDOWN) {
            return "ElasticReq{" + "endpoint=" + endpoint + ", op=SHUTDOWN" + '}';
        } else if (op == LAUNCH) {
            return "ElasticReq{" + "endpoint=" + endpoint + ", op=LAUNCH" + '}';
        } else {
            return "ElasticReq{" + "endpoint=" + endpoint + ", op=UNKNOWN" + '}';
        }
    }
}
