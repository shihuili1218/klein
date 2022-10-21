package com.ofcoder.klein.consensus.facade;

import java.nio.ByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ofcoder.klein.rpc.facade.InvokeCallback;
import com.ofcoder.klein.rpc.facade.serialization.Hessian2Util;

/**
 * @author far.liu
 */
public abstract class AbstractInvokeCallback<RES> implements InvokeCallback {
    public abstract void complete(RES result);

    @Override
    public void complete(ByteBuffer result) {
        RES deserialize = Hessian2Util.deserialize(result.array());
        complete(deserialize);
    }
}
