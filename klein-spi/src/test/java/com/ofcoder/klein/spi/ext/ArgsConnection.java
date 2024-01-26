package com.ofcoder.klein.spi.ext;

import com.ofcoder.klein.spi.Join;

/**
 * @author 释慧利
 */
@Join
public class ArgsConnection implements DBConnection {

    public ArgsConnection(String hi) {
        System.out.printf(hi);
    }
}
