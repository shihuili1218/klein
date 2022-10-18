package com.ofcoder.klein.consensus.facade;

import com.ofcoder.klein.consensus.facade.config.ConsensusProp;
import com.ofcoder.klein.spi.SPI;

/**
 * @author far.liu
 */
@SPI
public interface ConsensusBootstrap {
    void startup(ConsensusProp prop);
}
