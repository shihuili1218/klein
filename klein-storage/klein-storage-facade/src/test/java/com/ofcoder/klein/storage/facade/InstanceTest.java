package com.ofcoder.klein.storage.facade;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;

public class InstanceTest {

    Instance instance;

    long testInstanceId = 100L;

    @Before
    public void setUp() {
        instance = mock(Instance.class);
    }

    @Test
    public void testGetInstanceId() {
        when(instance.getInstanceId()).thenReturn(testInstanceId);

        long result = instance.getInstanceId();

        assertEquals(
                "getInstanceId should return the instanceId set in the instance object",
                testInstanceId,
                result);
    }
}
