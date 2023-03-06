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
package com.ofcoder.klein.jepsen.server.rpc;

import java.io.Serializable;

/**
 * GetResp.
 *
 * @author 释慧利
 */
public class Resp implements Serializable {
    private boolean s;
    private Object v;

    public Resp() {
    }

    public Resp(final boolean s, final Object v) {
        this.s = s;
        this.v = v;
    }

    public boolean isS() {
        return s;
    }

    public void setS(final boolean s) {
        this.s = s;
    }

    public Object getV() {
        return v;
    }

    public void setV(final Object v) {
        this.v = v;
    }

    @Override
    public String toString() {
        return "Resp{"
                + "s=" + s
                + ", v=" + v
                + '}';
    }
}
