package com.ofcoder.klein.storage.h2;

import com.ofcoder.klein.spi.Join;
import com.ofcoder.klein.storage.facade.StorageEngine;
import com.ofcoder.klein.storage.facade.config.StorageProp;

/**
 * @author: 释慧利
 */
@Join
public class H2Engine implements StorageEngine {
    @Override
    public void init(StorageProp op) {

    }

    @Override
    public void shutdown() {

    }
}
