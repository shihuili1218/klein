package com.ofcoder.klein.common.serialization;

import org.junit.Assert;

import junit.framework.TestCase;

public class Hessian2UtilTest extends TestCase {

    public void testSerialize() {
        String resource = "Hello Klein";
        byte[] serialize = Hessian2Util.serialize(resource);
        Assert.assertNotNull(serialize);
        String deserialize = Hessian2Util.deserialize(serialize);
        Assert.assertEquals(deserialize, resource);

    }
}