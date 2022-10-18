package com.ofcoder.klein.core.cache;

import java.io.Serializable;

/**
 * @author far.liu
 */
public class Message implements Serializable {

    public static final byte PUT = 0x01;
    public static final byte PUTIFPRESENT = 0x02;
    public static final byte GET = 0x03;
    public static final byte EXIST = 0x04;
    public static final byte INVALIDATE = 0x05;
    public static final byte INVALIDATEALL = 0x06;

    private byte op;
    private String key;
    private Object data;
    private long ttl;

    public byte getOp() {
        return op;
    }

    public void setOp(byte op) {
        this.op = op;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public long getTtl() {
        return ttl;
    }

    public void setTtl(long ttl) {
        this.ttl = ttl;
    }

    @Override
    public String toString() {
        return "Message{" +
                "op=" + op +
                ", key='" + key + '\'' +
                ", data=" + data +
                ", ttl=" + ttl +
                '}';
    }
}
