package com.ofcoder.klein.spi;

import com.ofcoder.klein.spi.ext.DBConnection;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author: 释慧利
 */
public class ExtensionLoaderTest {

    @Test
    public void testSameJoin() {
        DBConnection first = ExtensionLoader.getExtensionLoader(DBConnection.class).getJoin("mysql");
        DBConnection second = ExtensionLoader.getExtensionLoader(DBConnection.class).getJoin("mysql");
        Assert.assertEquals(first, second);
    }
}
