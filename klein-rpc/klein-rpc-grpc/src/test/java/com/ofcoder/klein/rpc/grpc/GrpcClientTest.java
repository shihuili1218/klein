package com.ofcoder.klein.rpc.grpc;

import com.ofcoder.klein.rpc.facade.Endpoint;
import com.ofcoder.klein.rpc.facade.InvokeCallback;
import com.ofcoder.klein.rpc.facade.InvokeParam;
import com.ofcoder.klein.rpc.facade.RpcClient;
import com.ofcoder.klein.rpc.facade.RpcProcessor;
import com.ofcoder.klein.rpc.facade.RpcServer;
import com.ofcoder.klein.rpc.facade.config.RpcProp;
import com.ofcoder.klein.rpc.grpc.ext.HelloProcessor;
import com.ofcoder.klein.spi.ExtensionLoader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author 释慧利
 */
public class GrpcClientTest {
    private static final Logger LOG = LoggerFactory.getLogger(GrpcClientTest.class);
    private final HelloProcessor processor = new HelloProcessor();

    private RpcClient rpcClient;
    private RpcServer rpcServer;

    @Before
    public void setup() {
        RpcProp prop = new RpcProp();
        rpcServer = ExtensionLoader.getExtensionLoader(RpcServer.class).register("grpc", prop);
        rpcClient = ExtensionLoader.getExtensionLoader(RpcClient.class).register("grpc", prop);
        rpcServer.registerProcessor(processor);
    }

    @After
    public void shutdown() {
        rpcClient.shutdown();
        rpcServer.shutdown();
    }

    @Test
    public void testSendRequest() throws InterruptedException {
        InvokeParam param = InvokeParam.Builder.anInvokeParam()
            .service("".getClass().getSimpleName())
            .method(RpcProcessor.KLEIN)
            .data("I'm Klein".getBytes(StandardCharsets.UTF_8)).build();


        CountDownLatch latch = new CountDownLatch(2);
        rpcClient.sendRequestAsync(new Endpoint("1", "127.0.0.1", 1218, false), param, new InvokeCallback() {
            @Override
            public void error(Throwable err) {
                LOG.error(err.getMessage(), err);
                latch.countDown();
            }

            @Override
            public void complete(byte[] result) {
                LOG.info("receive server message: {}", new String(result, StandardCharsets.UTF_8));
                latch.countDown();
            }
        }, 5000);

        Thread.sleep(500);

        rpcClient.sendRequestAsync(new Endpoint("1", "127.0.0.1", 1218, false), param, new InvokeCallback() {
            @Override
            public void error(Throwable err) {
                LOG.error(err.getMessage(), err);
                latch.countDown();
            }

            @Override
            public void complete(byte[] result) {
                LOG.info("receive server message: {}", new String(result, StandardCharsets.UTF_8));
                latch.countDown();
            }
        }, 5000);

        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

    }

}
