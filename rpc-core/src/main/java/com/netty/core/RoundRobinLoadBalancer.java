package com.netty.core;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class RoundRobinLoadBalancer implements LoadBalancer {
    private final ConcurrentHashMap<Integer, AtomicInteger> seq = new ConcurrentHashMap<>();

    @Override
    public InetSocketAddress select(List<InetSocketAddress> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            throw new IllegalStateException("No available provider");
        }
        int key = nodes.hashCode();
        int idx = seq.computeIfAbsent(key, k -> new AtomicInteger()).getAndIncrement();
        return nodes.get(Math.floorMod(idx, nodes.size()));
    }
}