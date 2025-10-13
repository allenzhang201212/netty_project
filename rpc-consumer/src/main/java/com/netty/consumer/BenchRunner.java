package com.netty.consumer;

import com.netty.common.HelloService;
import com.netty.core.annotation.RpcReference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.LongStream;

@Component
@Profile("bench")
public class BenchRunner implements ApplicationRunner {

    @RpcReference
    private HelloService hello;

    private final ApplicationContext ctx;
    public BenchRunner(ApplicationContext ctx) { this.ctx = ctx; }

    @Value("${bench.threads:10}")    int threads;
    @Value("${bench.requests:1000}") int requests;
    @Value("${bench.warmup:50}")     int warmup;
    @Value("${bench.progressStep:0}") int progressStep; // 每处理多少请求打印一次进度（0=不打印）

    @Override
    public void run(org.springframework.boot.ApplicationArguments args) throws Exception {
        for (int i = 0; i < warmup; i++) hello.hello("warmup");

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1), done = new CountDownLatch(requests);
        long[] times = new long[requests];
        AtomicInteger ok = new AtomicInteger();
        AtomicInteger finished = new AtomicInteger();

        for (int i = 0; i < requests; i++) {
            final int idx = i;
            pool.submit(() -> {
                try {
                    start.await();
                    long t0 = System.nanoTime();
                    String r = hello.hello("bench");
                    long t1 = System.nanoTime();
                    if ("Hello, bench".equals(r)) { ok.incrementAndGet(); times[idx] = t1 - t0; }
                    else times[idx] = -1;
                } catch (Exception e) {
                    times[idx] = -1;
                } finally {
                    int fin = finished.incrementAndGet();
                    if (progressStep > 0 && fin % progressStep == 0) {
                        System.out.printf("[BENCH] progress %d/%d%n", fin, requests);
                    }
                    done.countDown();
                }
            });
        }

        long tStart = System.currentTimeMillis();
        start.countDown();
        done.await();
        long tEnd = System.currentTimeMillis();
        pool.shutdown();

        long[] okTimes = LongStream.of(times).filter(x -> x >= 0).toArray();
        Arrays.sort(okTimes);
        double avg = okTimes.length==0? -1 : (LongStream.of(okTimes).average().orElse(0)/1_000_000.0);
        double p50 = percentile(okTimes,0.50)/1_000_000.0;
        double p95 = percentile(okTimes,0.95)/1_000_000.0;
        double p99 = percentile(okTimes,0.99)/1_000_000.0;

        double durationSec = (tEnd - tStart)/1000.0;
        double qps = durationSec > 0 ? ok.get()/durationSec : 0;

        System.out.printf("[BENCH] threads=%d requests=%d ok=%d qps=%.1f avg=%.3fms p50=%.3fms p95=%.3fms p99=%.3fms%n",
                threads, requests, ok.get(), qps, avg, p50, p95, p99);

        int code = SpringApplication.exit(ctx, () -> 0);
        System.exit(code);
    }

    private static long percentile(long[] sortedNs, double p) {
        if (sortedNs.length == 0) return -1;
        int idx = Math.min(sortedNs.length-1, (int)Math.ceil(p*sortedNs.length)-1);
        return sortedNs[idx];
    }
}