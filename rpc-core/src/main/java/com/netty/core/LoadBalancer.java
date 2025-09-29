package com.netty.core;

import java.net.InetSocketAddress;
import java.util.List;

public interface LoadBalancer {
    InetSocketAddress select(List<InetSocketAddress> nodes);
}
