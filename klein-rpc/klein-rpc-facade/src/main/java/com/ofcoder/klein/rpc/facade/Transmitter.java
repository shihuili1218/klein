package com.ofcoder.klein.rpc.facade;

/**
 * @author: 释慧利
 */
public interface Transmitter {

    void createConnection(final Endpoint endpoint);

    boolean checkConnection(final Endpoint endpoint);

    void closeConnection(final Endpoint endpoint);

    void closeAll(final Endpoint endpoint);

    void sendRequest(final Endpoint target, final Object request, InvokeCallback callback, long timeoutMs);

    Object sendRequestSync(final Endpoint target, final Object request, long timeoutMs);

}
