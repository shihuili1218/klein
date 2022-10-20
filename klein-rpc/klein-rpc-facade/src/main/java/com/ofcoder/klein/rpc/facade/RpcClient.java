package com.ofcoder.klein.rpc.facade;

import com.ofcoder.klein.common.Lifecycle;
import com.ofcoder.klein.rpc.facade.config.RpcProp;
import com.ofcoder.klein.spi.SPI;

/**
 * grpc client for send request.
 *
 * @author: 释慧利
 */
@SPI
public interface RpcClient extends Lifecycle<RpcProp> {

    void createConnection(final Endpoint endpoint);

    boolean checkConnection(final Endpoint endpoint);

    void closeConnection(final Endpoint endpoint);

    void closeAll();

    void sendRequest(final Endpoint target, final InvokeParam request, InvokeCallback callback, long timeoutMs);

    Object sendRequestSync(final Endpoint target, final InvokeParam request, long timeoutMs);

}
