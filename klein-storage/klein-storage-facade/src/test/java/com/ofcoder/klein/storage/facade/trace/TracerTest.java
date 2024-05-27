//package com.ofcoder.klein.storage.facade.trace;
//
//import org.junit.Before;
//import org.junit.Test;
//import org.mockito.Mock;
//import org.mockito.MockedStatic;
//
//import com.ofcoder.klein.spi.ExtensionLoader;
//import com.ofcoder.klein.storage.facade.TraceManager;
//import com.ofcoder.klein.storage.facade.config.StorageProp;
//
//import static org.mockito.Mockito.mockStatic;
//import static org.mockito.Mockito.times;
//import static org.mockito.Mockito.verify;
//import static org.mockito.Mockito.when;
//
//public class TracerTest {
//    @Mock
//    TraceManager mockManager;
//    @Mock
//    ExtensionLoader<TraceManager> traceManagerExtensionLoader;
//
//    @Before
//    public void setUp() {
//        try (MockedStatic<ExtensionLoader> mocked = mockStatic(ExtensionLoader.class)) {
//            mocked.when(ExtensionLoader::getExtensionLoader).thenReturn(traceManagerExtensionLoader);
//            when(traceManagerExtensionLoader.getJoin("test")).thenReturn(mockManager);
//        }
//    }
//
//    @Test
//    public void testTrace() {
//        final StorageProp mockProp = new StorageProp();
//
//        final Tracer tracer = new Tracer("test", mockProp);
//
//        for (int i = 0; i < 100; i++) {
//            tracer.trace("data");
//        }
//
//        verify(mockManager, times(100));
//    }
//}
