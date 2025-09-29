package com.netty.core;

import com.netty.common.*;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class RpcMessageEncoder extends MessageToByteEncoder<Object> {
    private final Serializer serializer;

    public RpcMessageEncoder(Serializer serializer) {
        this.serializer = serializer;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {
        byte msgType;
        long reqId;
        byte[] body;

        if (msg instanceof RpcRequest r) {
            msgType = MsgType.REQUEST.code;
            reqId = r.requestId;
            body = serializer.serialize(r);
        } else if (msg instanceof RpcResponse r) {
            msgType = MsgType.RESPONSE.code;
            reqId = r.requestId;
            body = serializer.serialize(r);
        } else {
            msgType = MsgType.HEARTBEAT.code;
            reqId = 0L;
            body = new byte[0];
        }

        out.writeInt(ProtocolConstants.MAGIC);
        out.writeByte(ProtocolConstants.VERSION);
        out.writeByte(msgType);
        out.writeByte(serializer.code());
        out.writeLong(reqId);
        out.writeInt(body.length);

        if (body.length > 0) {
            out.writeBytes(body);
        }
    }
}
