package com.ofcoder.klein.consensus.facade;

import java.io.Serializable;

import com.ofcoder.klein.common.Lifecycle;
import com.ofcoder.klein.consensus.facade.config.ConsensusProp;
import com.ofcoder.klein.spi.SPI;

/**
 * @author 释慧利
 */
@SPI
public interface Consensus extends Lifecycle<ConsensusProp> {

    <E extends Serializable> Result propose(final E data);

    /**
     * W + R > N
     * read from local.sm and only check self.lastApplyInstance if W = 1
     * read from W and only check self.lastApplyInstance if W > 1
     *
     * @param data message
     * @return whether success
     */
    <E extends Serializable> Result read(final E data);

    void loadSM(SM sm);

}
