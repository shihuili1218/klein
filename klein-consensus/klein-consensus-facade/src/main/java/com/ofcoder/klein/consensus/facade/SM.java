package com.ofcoder.klein.consensus.facade;

import java.nio.ByteBuffer;

/**
 * @author: 释慧利
 */
public interface SM {

    void apply(Object data);

    void makeImage();

    void loadImage();

    long lastApplyInstance();

}
