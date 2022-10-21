package com.ofcoder.klein.consensus.facade;

import com.ofcoder.klein.common.exception.NoImplementationException;

/**
 * @author: 释慧利
 */
public abstract class Node {

    @Override
    public int hashCode() {
        throw new NoImplementationException("Subclasses must implement the hashCode method");
    }

    @Override
    public boolean equals(Object obj) {
        throw new NoImplementationException("Subclasses must implement the equals method");
    }

    @Override
    public String toString() {
        throw new NoImplementationException("Subclasses must implement the toString method");
    }
}
