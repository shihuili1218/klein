package com.ofcoder.klein.storage.facade;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.ofcoder.klein.common.Lifecycle;
import com.ofcoder.klein.spi.SPI;
import com.ofcoder.klein.storage.facade.config.StorageProp;

/**
 * @author 释慧利
 */
@SPI
public interface LogManager<P extends Serializable> extends Lifecycle<StorageProp> {

    ReentrantReadWriteLock getLock();

    /**
     * Get the instance by id.
     *
     * @param id the index of instance
     * @return the instance with {@code id}
     */
    Instance<P> getInstance(final long id);

    /**
     * Get instance without consensus.
     *
     * @return all instance for no confirm, state in (PREPARED, ACCEPTED)
     */
    List<Instance<P>> getInstanceNoConfirm();

    /**
     * Persisting the Instance.
     * <p>
     * NOTICE: It needs to be called in a synchronous method.
     *
     * @param instance data
     */
    void updateInstance(final Instance<P> instance);

    long maxInstanceId();
    long maxProposalNo();

    long maxAppliedInstanceId();

    void saveSnap(String group, Snap snap);

    Snap getLastSnap(String group);


}
