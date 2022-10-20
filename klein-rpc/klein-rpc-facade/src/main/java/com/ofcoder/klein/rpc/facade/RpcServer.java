package com.ofcoder.klein.rpc.facade;

import com.ofcoder.klein.common.Lifecycle;
import com.ofcoder.klein.rpc.facade.config.RpcProp;
import com.ofcoder.klein.spi.SPI;

/**
 * @author far.liu
 */
@SPI
public interface RpcServer extends Lifecycle<RpcProp> {

    /**
     * Register user processor.
     *
     * @param processor the user processor which has a interest
     */
    void registerProcessor(final RpcProcessor processor);
}
