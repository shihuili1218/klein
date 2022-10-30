package com.ofcoder.klein.core.cache;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

/**
 * @author 释慧利
 */
public interface KleinCache {
    boolean exist(String key);

    <D extends Serializable> boolean put(String key, D data);

    <D extends Serializable> boolean put(String key, D data, Long ttl, TimeUnit unit);

    <D extends Serializable> D putIfPresent(String key, D data);

    <D extends Serializable> D putIfPresent(String key, D data, Long ttl, TimeUnit unit);

    <D extends Serializable> D get(String key);

    void invalidate(String key);

    void invalidateAll();


}
