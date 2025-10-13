package com.netty.consumer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class HelloServiceTests {

    @Autowired
    private HelloClient client; 

    @Test
    void smoke() {
        String r = client.call("JUnit");
        Assertions.assertEquals("Hello, JUnit", r);
        System.out.println("[IT] smoke ok: " + r);
    }

    @Test
    void concurrent_calls() throws Exception {
        int threads = 10, total = 200;
        var pool = java.util.concurrent.Executors.newFixedThreadPool(threads);
        var start = new java.util.concurrent.CountDownLatch(1);
        var done = new java.util.concurrent.CountDownLatch(total);
        var ok = new java.util.concurrent.atomic.AtomicInteger();
        for (int i = 0; i < total; i++) {
            pool.submit(() -> {
                try {
                    start.await();
                    if ("Hello, t".equals(client.call("t"))) ok.incrementAndGet();
                } catch (Exception ignore) {
                } finally {
                    done.countDown();
                }
            });
        }
        start.countDown();
        done.await();
        pool.shutdown();
        System.out.println("[IT] concurrent ok=" + ok.get() + "/" + total);
        Assertions.assertTrue(ok.get() >= total * 0.95, "success rate too low");
    }
}