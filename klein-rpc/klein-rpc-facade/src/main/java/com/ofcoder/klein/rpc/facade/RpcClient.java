package com.ofcoder.klein.rpc.facade;

import java.io.Serializable;
import java.nio.ByteBuffer;

import com.ofcoder.klein.common.Lifecycle;
import com.ofcoder.klein.common.serialization.Hessian2Util;
import com.ofcoder.klein.rpc.facade.config.RpcProp;
import com.ofcoder.klein.spi.SPI;

/**
 * grpc client for send request.
 *
 * @author 释慧利
 */
@SPI
public interface RpcClient extends Lifecycle<RpcProp> {

    void createConnection(final Endpoint endpoint);

    boolean checkConnection(final Endpoint endpoint);

    void closeConnection(final Endpoint endpoint);

    void closeAll();

    default void sendRequestAsync(final Endpoint target, final Serializable request, InvokeCallback callback, long timeoutMs) {
        InvokeParam param = InvokeParam.Builder.anInvokeParam()
                .service(request.getClass().getSimpleName())
                .method(RpcProcessor.KLEIN)
                .data(ByteBuffer.wrap(Hessian2Util.serialize(request))).build();
        sendRequestAsync(target, param, callback, timeoutMs);
    }

    void sendRequestAsync(final Endpoint target, final InvokeParam request, InvokeCallback callback, long timeoutMs);

    Object sendRequestSync(final Endpoint target, final InvokeParam request, long timeoutMs);

}
