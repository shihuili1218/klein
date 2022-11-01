package com.ofcoder.klein.consensus.facade;

import java.io.Serializable;

import com.ofcoder.klein.common.Lifecycle;
import com.ofcoder.klein.consensus.facade.config.ConsensusProp;
import com.ofcoder.klein.consensus.facade.sm.SM;
import com.ofcoder.klein.spi.SPI;

/**
 * @author 释慧利
 */
@SPI
public interface Consensus extends Lifecycle<ConsensusProp> {

    /**
     * @param data  Client data, type is <E>
     *              e.g. The input value of the state machine
     * @param apply Whether you need to wait until the state machine is applied
     *              If true, wait until the state machine is applied before returning
     * @return whether success
     */
    <E extends Serializable, D extends Serializable> Result<D> propose(final E data, final boolean apply);

    default <E extends Serializable> Result propose(final E data) {
        return propose(data, false);
    }

    /**
     * W + R > N
     * read from local.sm and only check self.lastApplyInstance if W = 1
     * read from W and only check self.lastApplyInstance if W > 1
     *
     * @param data message
     * @return whether success
     */
    <E extends Serializable, D extends Serializable> Result<D> read(final E data);

    void loadSM(SM sm);

}
