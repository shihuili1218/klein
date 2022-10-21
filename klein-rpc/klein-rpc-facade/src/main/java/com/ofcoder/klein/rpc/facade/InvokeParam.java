package com.ofcoder.klein.rpc.facade;

import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * @author far.liu
 */
public class InvokeParam {
    private String service;
    private String method;
    private ByteBuffer data;

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public ByteBuffer getData() {
        return data;
    }

    public void setData(ByteBuffer data) {
        this.data = data;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InvokeParam that = (InvokeParam) o;
        return Objects.equals(service, that.service) && Objects.equals(method, that.method) && Objects.equals(data, that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(service, method, data);
    }

    @Override
    public String toString() {
        return "InvokeParam{" +
                "service='" + service + '\'' +
                ", method='" + method + '\'' +
                ", data='" + data + '\'' +
                '}';
    }
}
