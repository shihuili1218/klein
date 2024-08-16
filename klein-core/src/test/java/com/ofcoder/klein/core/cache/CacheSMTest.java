package com.ofcoder.klein.core.cache;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CacheSMTest {

    CacheSM cacheSM;

    @Mock
    private CacheContainer mockContainer;

    @Before
    public void setUp() throws NoSuchFieldException, IllegalAccessException {
        MockitoAnnotations.initMocks(this);
        cacheSM = new CacheSM(new CacheProp()) {
            @Override
            public Object makeImage() {
                return mockContainer;
            }
        };

        Map<String, CacheContainer> containers = new HashMap<>();
        containers.put("test", mockContainer);

        Field containerField = CacheSM.class.getDeclaredField("containers");
        containerField.setAccessible(true);
        containerField.set(cacheSM, containers);
    }

    @Test
    public void testApplyWithPutOperation() {
        CacheMessage message = new CacheMessage();
        message.setOp(CacheMessage.PUT);
        message.setCacheName("test");
        message.setKey("key");
        message.setData("data");
        message.setExpire(1000L);

        cacheSM.apply(message);
        verify(mockContainer).put(eq("key"), eq("data"), eq(1000L));
    }

    @Test
    public void testApplyWithGetOperation() {
        CacheMessage message = new CacheMessage();
        message.setCacheName("test");
        message.setOp(CacheMessage.GET);
        message.setKey("key");

        when(mockContainer.get("key")).thenReturn("data");

        assertEquals("data", cacheSM.apply(message));
    }

    @Test
    public void testApplyWithInvalidateOperation() {
        CacheMessage message = new CacheMessage();
        message.setCacheName("test");
        message.setOp(CacheMessage.INVALIDATE);
        message.setKey("key");

        cacheSM.apply(message);
        verify(mockContainer).remove("key");
    }

    @Test
    public void testApplyWithInvalidateAllOperation() {
        CacheMessage message = new CacheMessage();
        message.setCacheName("test");
        message.setOp(CacheMessage.INVALIDATEALL);

        cacheSM.apply(message);
        verify(mockContainer).clear();
    }

    @Test
    public void testApplyWithPutIfPresentOperation() {
        CacheMessage message = new CacheMessage();
        message.setCacheName("test");
        message.setOp(CacheMessage.PUTIFPRESENT);
        message.setKey("key");
        message.setData("data");
        message.setExpire(1000L);

        when(mockContainer.putIfAbsent("key", "data", 1000L)).thenReturn(true);

        assertEquals(true, cacheSM.apply(message));
    }

    @Test
    public void testApplyWithExistOperation() {
        CacheMessage message = new CacheMessage();
        message.setCacheName("test");
        message.setOp(CacheMessage.EXIST);
        message.setKey("key");

        when(mockContainer.containsKey("key")).thenReturn(true);

        assertEquals(true, cacheSM.apply(message));
    }


    @Test
    public void testApplyWithUnknownMessage() {
        assertNull(cacheSM.apply(new Object()));
        verify(mockContainer, never()).put(any(), any(), anyLong());
        verify(mockContainer, never()).get(any());
        verify(mockContainer, never()).remove(any());
        verify(mockContainer, never()).clear();
    }
}
