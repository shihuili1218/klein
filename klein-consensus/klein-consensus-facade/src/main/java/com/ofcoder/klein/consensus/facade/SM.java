package com.ofcoder.klein.consensus.facade;

import java.io.Serializable;
import java.nio.ByteBuffer;

/**
 * @author 释慧利
 */
public interface SM {

   <E extends Serializable> E apply(Object data);

    void makeImage();

    void loadImage();


}
