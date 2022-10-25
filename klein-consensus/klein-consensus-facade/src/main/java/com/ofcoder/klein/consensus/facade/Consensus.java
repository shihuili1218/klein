package com.ofcoder.klein.consensus.facade;

import java.io.Serializable;
import java.nio.ByteBuffer;

import com.ofcoder.klein.common.Lifecycle;
import com.ofcoder.klein.consensus.facade.config.ConsensusProp;
import com.ofcoder.klein.rpc.facade.serialization.Hessian2Util;
import com.ofcoder.klein.spi.SPI;

/**
 * @author 释慧利
 */
@SPI
public interface Consensus extends Lifecycle<ConsensusProp> {

    <E extends Serializable> Result propose(final E data);

    Result read(final ByteBuffer data);

    void loadSM(SM sm);

}
