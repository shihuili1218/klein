package com.ofcoder.klein;

import org.junit.Assert;
import org.junit.Test;

public class KleinPropTest {

    @Test
    public void testGetStorage() {
        KleinProp prop = new KleinProp();
        Assert.assertEquals("file", prop.getStorage());
        prop.setStorage("leveldb");
        Assert.assertEquals("leveldb", prop.getStorage());
    }
}
