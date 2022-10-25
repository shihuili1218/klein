package com.ofcoder.klein.common;

/**
 * @author 释慧利
 */
public interface Lifecycle<O> {

    void init(final O op);

    void shutdown();
}
