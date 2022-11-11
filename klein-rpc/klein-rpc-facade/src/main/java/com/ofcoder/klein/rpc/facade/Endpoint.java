package com.ofcoder.klein.rpc.facade;

import java.io.Serializable;
import java.util.Objects;

/**
 * @author 释慧利
 */
public class Endpoint implements Serializable {
    private String id;
    private String ip;
    private int port;

    public Endpoint(String id, String ip, int port) {
        this.id = id;
        this.ip = ip;
        this.port = port;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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
        return "id: " + id + ", endpoint: " + ip + ":" + port;
    }
}
