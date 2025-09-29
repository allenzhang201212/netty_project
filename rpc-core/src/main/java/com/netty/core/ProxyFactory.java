package com.netty.core;

import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

public class ProxyFactory {
    private final PooledRpcClient client;
    private final AddressDirectory directory;
    private final LoadBalancer lb;

    // 可按需外部配置
    private final long timeoutMs;
    private final int maxRetries;

    public ProxyFactory(PooledRpcClient client, AddressDirectory directory, LoadBalancer lb) {
        this(client, directory, lb, 3000, 2);
    }
    public ProxyFactory(PooledRpcClient client, AddressDirectory directory, LoadBalancer lb, long timeoutMs, int maxRetries) {
        this.client = client; this.directory = directory; this.lb = lb;
        this.timeoutMs = timeoutMs; this.maxRetries = maxRetries;
    }

    @SuppressWarnings("unchecked")
    public <T> T create(Class<T> iface) {
        return (T) Proxy.newProxyInstance(
            iface.getClassLoader(),
            new Class<?>[]{iface},
            (proxy, method, args) -> {
                // 取一份节点快照，失败就剔除并重试
                List<InetSocketAddress> nodes = new ArrayList<>(directory.lookup(iface.getName()));
                Exception last = null;

                for (int attempt = 0; attempt <= maxRetries && !nodes.isEmpty(); attempt++) {
                    InetSocketAddress addr = lb.select(nodes);
                    try {
                        return client.invoke(
                            addr,
                            iface.getName(),
                            method.getName(),
                            method.getParameterTypes(),
                            args,
                            timeoutMs
                        );
                    } catch (Exception e) {
                        last = e;
                        // 该节点失败，剔除换下一个
                        nodes.remove(addr);
                    }
                }
                throw new RuntimeException("RPC failed after retries: " + (maxRetries + 1), last);
            }
        );
    }
}