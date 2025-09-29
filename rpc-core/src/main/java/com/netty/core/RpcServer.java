package com.netty.core;

import com.netty.common.*;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.timeout.IdleStateHandler;

public class RpcServer implements AutoCloseable {
    private final int port;
    private final ServiceInvoker invoker;
    private final Serializer serializer = SerializerRegistry.defaultSerializer();
    private EventLoopGroup boss;
    private EventLoopGroup worker;
    private Channel serverChannel;

    public RpcServer(int port, ServiceInvoker invoker) {
        this.port = port;
        this.invoker = invoker;
    }

    /** 启动（绑定端口后立即返回，不阻塞主线程） */
    public void start() {
        boss = new NioEventLoopGroup(1);
        worker = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap()
                    .group(boss, worker)
                    .channel(NioServerSocketChannel.class)
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override protected void initChannel(SocketChannel ch) {
                            ChannelPipeline p = ch.pipeline();
                            p.addLast(new IdleStateHandler(60, 20, 0));
                            p.addLast(new KeepAliveHandler());
                            p.addLast(new LengthFieldBasedFrameDecoder(1024 * 1024, 0, 4, 0, 4));
                            p.addLast(new RpcMessageDecoder());
                            p.addLast(new LengthFieldPrepender(4));
                            p.addLast(new RpcMessageEncoder(serializer));
                            p.addLast(new SimpleChannelInboundHandler<RpcRequest>() {
                                @Override protected void channelRead0(ChannelHandlerContext ctx, RpcRequest req) throws Exception {
                                    RpcResponse resp = new RpcResponse();
                                    resp.requestId = req.requestId;
                                    try {
                                        Object result = invoker.invoke(req);
                                        resp.code = 0; resp.data = result;
                                    } catch (Throwable t) {
                                        resp.code = 1; resp.message = t.toString();
                                    }
                                    ctx.writeAndFlush(resp);
                                }
                            });
                        }
                    });
            ChannelFuture bind = b.bind(port).sync();           // 仅等待绑定完成
            serverChannel = bind.channel();                     // 不再等待 closeFuture，非阻塞返回
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    /** 优雅关闭 */
    public void stop() {
        try {
            if (serverChannel != null) serverChannel.close().syncUninterruptibly();
        } finally {
            if (boss != null) boss.shutdownGracefully();
            if (worker != null) worker.shutdownGracefully();
        }
    }

    @Override public void close() { 
        stop(); 
    }
}