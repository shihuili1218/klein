package com.ofcoder.klein.spi;

import com.ofcoder.klein.spi.ext.DBConnection;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author 释慧利
 */
public class ExtensionLoaderTest {

    @Test
    public void testSameJoin() {
        DBConnection zero = ExtensionLoader.getExtensionLoader(DBConnection.class).register("mysql");
        DBConnection first = ExtensionLoader.getExtensionLoader(DBConnection.class).getJoin("mysql");
        DBConnection second = ExtensionLoader.getExtensionLoader(DBConnection.class).getJoin("mysql");
        Assert.assertEquals(first, zero);
        Assert.assertEquals(first, second);
    }


    @Test
    public void testArgsJoin() {
        DBConnection zero = ExtensionLoader.getExtensionLoader(DBConnection.class).register("args", "guys");
        DBConnection first = ExtensionLoader.getExtensionLoader(DBConnection.class).getJoin("args");
        DBConnection second = ExtensionLoader.getExtensionLoader(DBConnection.class).getJoin("args");
        Assert.assertEquals(first, zero);
        Assert.assertEquals(first, second);
    }
}
