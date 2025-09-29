package com.netty.core;

import com.netty.common.Heartbeat;
import io.netty.channel.*;
import io.netty.handler.timeout.*;

public class KeepAliveHandler extends ChannelDuplexHandler {
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent e) {
            if (e.state() == IdleState.WRITER_IDLE) {
                // 定时写一个心跳帧，触发对端 read，避免 read-idle 误判
                ctx.writeAndFlush(Heartbeat.INSTANCE);
            } else if (e.state() == IdleState.READER_IDLE) {
                // 长时间未读到任何数据，认为连接失活，主动关闭
                ctx.close();
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }
}