package com.ofcoder.klein.rpc.facade;

import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

/**
 * @author: 释慧利
 */
public class Endpoint {
    private String ip;
    private int port;

    public Endpoint(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Endpoint endpoint = (Endpoint) o;
        return port == endpoint.port && Objects.equals(ip, endpoint.ip);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ip, port);
    }

    @Override
    public String toString() {
        return "endpoint: " + ip + ":" + port;
    }
}
