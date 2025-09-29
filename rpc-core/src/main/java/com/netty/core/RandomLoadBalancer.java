package com.netty.core;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class RandomLoadBalancer implements LoadBalancer {
    @Override public InetSocketAddress select(List<InetSocketAddress> nodes) {
        if (nodes == null || nodes.isEmpty())
            throw new IllegalStateException("No available provider");
        return nodes.get(ThreadLocalRandom.current().nextInt(nodes.size()));
    }
}

