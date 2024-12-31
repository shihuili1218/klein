package com.ofcoder.klein.serializer.hessian2;

import com.ofcoder.klein.serializer.Serializer;
import com.ofcoder.klein.spi.ExtensionLoader;
import org.junit.Assert;

import junit.framework.TestCase;

public class Hessian2UtilTest extends TestCase {

    public void testSerialize() {
        Serializer hessian2 = ExtensionLoader.getExtensionLoader(Serializer.class).register("hessian2");

        String resource = "Hello Klein";
        byte[] serialize = hessian2.serialize(resource);
        Assert.assertNotNull(serialize);
        String deserialize = (String) hessian2.deserialize(serialize);
        Assert.assertEquals(deserialize, resource);
    }
}