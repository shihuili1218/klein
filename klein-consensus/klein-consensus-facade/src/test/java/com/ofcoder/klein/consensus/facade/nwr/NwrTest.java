package com.ofcoder.klein.consensus.facade.nwr;

import com.ofcoder.klein.spi.ExtensionLoader;
import junit.framework.TestCase;

public class NwrTest extends TestCase {

    public void testR() {
        Nwr majority = ExtensionLoader.getExtensionLoader(Nwr.class).getJoin("majority");
        Nwr fastWrite = ExtensionLoader.getExtensionLoader(Nwr.class).getJoin("fastWrite");

        assertEquals(2, majority.r(3));
        assertEquals(3, fastWrite.r(3));
    }

    public void testW() {
        Nwr majority = ExtensionLoader.getExtensionLoader(Nwr.class).getJoin("majority");
        Nwr fastWrite = ExtensionLoader.getExtensionLoader(Nwr.class).getJoin("fastWrite");

        assertEquals(majority.w(3), 2);
        assertEquals(fastWrite.w(3), 1);
    }
}