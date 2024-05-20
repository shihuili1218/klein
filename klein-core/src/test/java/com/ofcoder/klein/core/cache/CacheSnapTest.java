package com.ofcoder.klein.core.cache;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import org.junit.Before;
import org.junit.Test;

public class CacheSnapTest {

    CacheSnap cacheSnap;

    Object cache;

    @Before
    public void setUp() {
        cache = mock(Object.class);
        cacheSnap = new CacheSnap(cache, null);
    }

    @Test
    public void testGetCache() {
        assertEquals(cache, cacheSnap.getCache());
    }
}
