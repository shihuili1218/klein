package com.ofcoder.klein.consensus.facade;

import java.io.Serializable;

/**
 * @author 释慧利
 */
public interface SM {

    <E extends Serializable> E apply(Object data);

    void makeImage();

    void loadImage();


}
