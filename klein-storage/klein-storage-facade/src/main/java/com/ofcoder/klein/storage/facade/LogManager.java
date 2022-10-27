package com.ofcoder.klein.storage.facade;

import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.ofcoder.klein.common.Lifecycle;
import com.ofcoder.klein.spi.SPI;
import com.ofcoder.klein.storage.facade.config.StorageProp;

/**
 * @author 释慧利
 */
@SPI
public interface LogManager extends Lifecycle<StorageProp> {

    ReentrantReadWriteLock getLock();

    /**
     * Get the instance by id.
     *
     * @param id the index of instance
     * @return the instance with {@code id}
     */
    Instance getInstance(final long id);

    /**
     * Get instance without consensus.
     *
     * @return all instance for no confirm, state in (PREPARED, ACCEPTED)
     */
    List<Instance> getInstanceNoConfirm();

    /**
     * Persisting the Instance.
     * <p>
     * NOTICE: It needs to be called in a synchronous method.
     *
     * @param instance data
     */
    void updateInstance(final Instance instance);

    long maxInstanceId();

    long maxAppliedInstanceId();

}
