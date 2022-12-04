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
    public static final long TTL_PERPETUITY = -1;

    private byte op;
    private String key;
    private Serializable data;
    private long expire = TTL_PERPETUITY;

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

    public Serializable getData() {
        return data;
    }

    public void setData(Serializable data) {
        this.data = data;
    }

    public long getExpire() {
        return expire;
    }

    public void setExpire(long expire) {
        this.expire = expire;
    }

    @Override
    public String toString() {
        return "Message{" +
                "op=" + op +
                ", key='" + key + '\'' +
                ", data=" + data +
                ", ttl=" + expire +
                '}';
    }
}
