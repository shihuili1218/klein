package com.ofcoder.klein.spi;

import org.junit.Assert;
import org.junit.Test;

import com.ofcoder.klein.spi.ExtensionLoader;
import com.ofcoder.klein.spi.ext.DBConnection;

/**
 * @author 释慧利
 */
public class ArgsExtensionLoaderTest {

    @Test
    public void testArgsJoin() {
        DBConnection zero = ExtensionLoader.getExtensionLoader(DBConnection.class).register("args", "guys");
        DBConnection first = ExtensionLoader.getExtensionLoader(DBConnection.class).getJoin("args");
        DBConnection second = ExtensionLoader.getExtensionLoader(DBConnection.class).getJoin("args");
        Assert.assertEquals(first, zero);
        Assert.assertEquals(first, second);
    }
}
