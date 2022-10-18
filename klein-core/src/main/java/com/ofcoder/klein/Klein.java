package com.ofcoder.klein;

import com.google.errorprone.annotations.CompatibleWith;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

/**
 * @author: 释慧利
 */
public interface Klein {

    <D extends Serializable> boolean put(String key, D data);

    <D extends Serializable> boolean put(String key, D data, Long ttl, TimeUnit unit);

    <D extends Serializable> boolean putIfPresent(String key, D data);

    <D extends Serializable> D get(String key);

    void invalidate(String key);

    void invalidateAll();

}
