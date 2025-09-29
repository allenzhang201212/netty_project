package com.netty.core;

import com.netty.common.*;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import java.util.List;

public class RpcMessageDecoder extends ByteToMessageDecoder {
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        int magic = in.readInt();
        if (magic != ProtocolConstants.MAGIC)
            throw new IllegalStateException("bad magic: " + magic);
        byte ver = in.readByte();
        byte mt = in.readByte();
        byte st = in.readByte();
        long reqId = in.readLong();
        int bodyLen = in.readInt();

        byte[] body = new byte[bodyLen];
        if (bodyLen > 0)
            in.readBytes(body);

        Serializer s = SerializerRegistry.byCode(st);
        if (mt == MsgType.REQUEST.code) {
            RpcRequest req = s.deserialize(body, RpcRequest.class);
            req.requestId = reqId;
            out.add(req);
        } else if (mt == MsgType.RESPONSE.code) {
            RpcResponse resp = s.deserialize(body, RpcResponse.class);
            resp.requestId = reqId;
            out.add(resp);
        } // heartbeat 忽略
    }
}