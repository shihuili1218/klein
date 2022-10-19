package com.ofcoder.klein.rpc.facade.config;

/**
 * @author far.liu
 */
public class RpcProp {
    private int port = 1218;
    private int maxInboundMsgSize = 4 * 1024 * 1024;

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getMaxInboundMsgSize() {
        return maxInboundMsgSize;
    }

    public void setMaxInboundMsgSize(int maxInboundMsgSize) {
        this.maxInboundMsgSize = maxInboundMsgSize;
    }
}
