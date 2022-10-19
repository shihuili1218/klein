package com.ofcoder.klein.consensus.facade.manager;

import java.nio.ByteBuffer;

/**
 * @author: 释慧利
 */
public interface SM {

    void apply(ByteBuffer data);

    void makeImage();

}
