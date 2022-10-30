package com.ofcoder.klein.consensus.facade;

import java.io.Serializable;

/**
 * @author 释慧利
 */
public interface SM {

    Object apply(Object data);

    void makeImage();

    void loadImage();


}
