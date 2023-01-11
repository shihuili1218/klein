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
package com.ofcoder.klein.rpc.facade;

import java.io.Serializable;
import java.util.Objects;

/**
 * desc ip and port.
 *
 * @author 释慧利
 */
public class Endpoint implements Serializable {
    private String id;
    private String ip;
    private int port;

    public Endpoint(final String id, final String ip, final int port) {
        this.id = id;
        this.ip = ip;
        this.port = port;
    }

    /**
     * get id.
     *
     * @return id
     */
    public String getId() {
        return id;
    }

    /**
     * set id.
     *
     * @param id id
     */
    public void setId(final String id) {
        this.id = id;
    }

    /**
     * get ip.
     *
     * @return ip
     */
    public String getIp() {
        return ip;
    }

    /**
     * get port.
     *
     * @return port
     */
    public int getPort() {
        return port;
    }

    /**
     * set ip.
     *
     * @param ip ip
     */
    public void setIp(final String ip) {
        this.ip = ip;
    }

    /**
     * set port.
     *
     * @param port port
     */
    public void setPort(final int port) {
        this.port = port;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Endpoint endpoint = (Endpoint) o;
        return port == endpoint.port && Objects.equals(ip, endpoint.ip);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ip, port);
    }

    @Override
    public String toString() {
        return "id: " + id + ", endpoint: " + ip + ":" + port;
    }
}
