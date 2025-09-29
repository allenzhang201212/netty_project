package com.netty.registry.zk;

import com.netty.core.AddressDirectory;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.CuratorCache;
import org.apache.curator.framework.recipes.cache.CuratorCacheListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class ZkRegistry implements AddressDirectory, Closeable {

    private final CuratorFramework client;
    private final Map<String, List<InetSocketAddress>> cache = new ConcurrentHashMap<>();
    private final Map<String, CuratorCache> watchers = new ConcurrentHashMap<>();

    public ZkRegistry(String connect) {
        this.client = CuratorFrameworkFactory.newClient(connect, new ExponentialBackoffRetry(1000, 3));
        this.client.start();
    }

    private static String servicePath(String service) { return "/rpc/" + service + "/providers"; }

    /** Provider 注册临时节点 */
    public void register(String service, String host, int port) {
        String path = servicePath(service) + "/" + host + ":" + port;
        try {
            client.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL).forPath(path);
        } catch (Exception e) {
            throw new RuntimeException("ZK register failed: " + path, e);
        }
    }

    @Override
    public List<InetSocketAddress> lookup(String service) {
        String path = servicePath(service);
        try {
            // 1) 拉取一次
            List<String> children = safeChildren(path);
            List<InetSocketAddress> list = parse(children);
            cache.put(service, new CopyOnWriteArrayList<>(list));

            // 2) 首次创建 CuratorCache watcher（后续事件自动刷新缓存）
            watchers.computeIfAbsent(service, s -> {
                CuratorCache c = CuratorCache.build(client, path);
                CuratorCacheListener listener = CuratorCacheListener.builder()
                    .forCreates(childData -> refresh(service, path))
                    .forChanges((oldData, newData) -> refresh(service, path))
                    .forDeletes(childData -> refresh(service, path))
                    .build();
                c.listenable().addListener(listener);
                c.start();
                return c;
            });

            return list;
        } catch (Exception e) {
            List<InetSocketAddress> hit = cache.get(service);
            if (hit != null && !hit.isEmpty()) return hit;
            throw new RuntimeException("ZK lookup failed: " + path, e);
        }
    }

    private void refresh(String service, String path) {
        try {
            List<String> ch = safeChildren(path);
            cache.put(service, new CopyOnWriteArrayList<>(parse(ch)));
        } catch (Exception ignore) {
        }
    }

    private List<String> safeChildren(String path) throws Exception {
        if (client.checkExists().forPath(path) == null) return Collections.emptyList();
        return client.getChildren().forPath(path);
    }

    private static List<InetSocketAddress> parse(List<String> children) {
        return children.stream()
            .map(s -> {
                String[] hp = s.split(":");
                return new InetSocketAddress(hp[0], Integer.parseInt(hp[1]));
            }).collect(Collectors.toList());
    }

    @Override public void close() throws IOException {
        for (CuratorCache c : watchers.values()) {
            try { c.close(); } catch (Exception ignore) {}
        }
        client.close();
    }
}