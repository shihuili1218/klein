package com.ofcoder.klein;

import com.google.common.cache.Cache;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

/**
 * @author: 释慧利
 */
public abstract class AbstractKlein implements Klein {
    @Override
    public <D extends Serializable> boolean put(String key, D data) {

        return false;
    }

    @Override
    public <D extends Serializable> boolean put(String key, D data, Long ttl, TimeUnit unit) {
        return false;
    }

    @Override
    public <D extends Serializable> D get(String key) {
        return null;
    }
}
