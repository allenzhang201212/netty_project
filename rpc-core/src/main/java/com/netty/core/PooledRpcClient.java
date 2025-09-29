package com.netty.core;

import com.netty.common.Ids;
import com.netty.common.RpcRequest;
import com.netty.common.RpcResponse;
import com.netty.common.Serializer;
import com.netty.common.SerializerRegistry;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.timeout.IdleStateHandler;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class PooledRpcClient {
    private final EventLoopGroup group = new NioEventLoopGroup();
    private final Bootstrap boot;
    private final Map<String, Channel> pool = new ConcurrentHashMap<>();
    private final Serializer serializer = SerializerRegistry.defaultSerializer();

    public PooledRpcClient() {
        this.boot = new Bootstrap()
            .group(group)
            .channel(NioSocketChannel.class)
            .option(ChannelOption.TCP_NODELAY, true)
            .handler(new ChannelInitializer<SocketChannel>() {
                @Override protected void initChannel(SocketChannel ch) {
                    ChannelPipeline p = ch.pipeline();

                    // --- Step 1.3: 心跳保活（写空闲触发 ping，读空闲关闭）---
                    p.addLast(new IdleStateHandler(0, 20, 0)); // 每 20s 触发写空闲
                    p.addLast(new KeepAliveHandler());

                    // 编解码：长度域帧 -> 解码 -> 长度前置 -> 编码
                    p.addLast(new LengthFieldBasedFrameDecoder(1024 * 1024, 0, 4, 0, 4));
                    p.addLast(new RpcMessageDecoder());
                    p.addLast(new LengthFieldPrepender(4));
                    p.addLast(new RpcMessageEncoder(serializer));

                    // 客户端入站处理：收到响应 -> 完成 Future
                    p.addLast(new ClientInboundHandler());
                }
            });
    }

    private Channel getOrCreate(InetSocketAddress addr) {
        String key = addr.toString();
        return pool.computeIfAbsent(key, k -> {
            try {
                return boot.connect(addr).sync().channel();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        });
    }

    public Object invoke(InetSocketAddress addr,
                         String service,
                         String method,
                         Class<?>[] paramTypes,
                         Object[] args,
                         long timeoutMs) throws Exception {

        RpcRequest req = new RpcRequest();
        req.requestId = Ids.next();
        req.service = service;
        req.method = method;
        req.paramTypes = paramTypes;
        req.args = args;

        CompletableFuture<RpcResponse> fut = new CompletableFuture<>();
        PendingCenter.put(req.requestId, fut);

        Channel ch = getOrCreate(addr);
        ch.writeAndFlush(req);

        RpcResponse resp = fut.get(timeoutMs, TimeUnit.MILLISECONDS);
        if (resp.code != 0) throw new RuntimeException(resp.message);
        return resp.data;
    }

    /** 可选：在需要时关闭所有连接并释放线程组 */
    public void close() {
        pool.values().forEach(ch -> {
            try { ch.close().syncUninterruptibly(); } catch (Exception ignore) {}
        });
        group.shutdownGracefully();
    }
}