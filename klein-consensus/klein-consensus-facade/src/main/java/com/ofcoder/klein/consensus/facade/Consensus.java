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
     * @param group group name
     * @param data  Client data, type is Serializable
     *              e.g. The input value of the state machine
     * @param apply Whether you need to wait until the state machine is applied
     *              If true, wait until the state machine is applied before returning
     * @return whether success
     */
    <E extends Serializable, D extends Serializable> Result<D> propose(final String group, final E data, final boolean apply);

    default <E extends Serializable> Result propose(final String group, final E data) {
        return propose(group, data, false);
    }

    /**
     * W + R > N
     * read from local.sm and only check self.lastApplyInstance if W = 1
     * read from W and only check self.lastApplyInstance if W > 1
     *
     * @param group group name
     * @param data  message
     * @return whether success
     */
    default <E extends Serializable, D extends Serializable> Result<D> read(final String group, final E data) {
        return propose(group, data, true);
    }

    void loadSM(final String group, final SM sm);

    void setListener(LifecycleListener listener);

    interface LifecycleListener {

        /**
         * initialized, ready to initiate a proposal
         */
        void prepared();
    }
}
