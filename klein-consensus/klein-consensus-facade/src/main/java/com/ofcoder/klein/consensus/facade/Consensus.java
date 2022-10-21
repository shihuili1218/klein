package com.ofcoder.klein.consensus.facade;

import com.ofcoder.klein.common.Lifecycle;
import com.ofcoder.klein.consensus.facade.config.ConsensusProp;
import com.ofcoder.klein.spi.SPI;

import java.nio.ByteBuffer;

/**
 * @author: 释慧利
 */
@SPI
public interface Consensus extends Lifecycle<ConsensusProp> {

    Result propose(final ByteBuffer data);

    Result read(final ByteBuffer data);

    void loadSM(SM sm);

}
