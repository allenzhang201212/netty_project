package com.netty.common;

public enum MsgType {
    REQUEST((byte) 0),
    RESPONSE((byte) 1),
    HEARTBEAT((byte) 2);

    public final byte code;

    MsgType(byte c) {
        this.code = c;
    }

    public static MsgType from(byte c) {
        for (MsgType t : values())
            if (t.code == c)
                return t;
        throw new IllegalArgumentException("Unkown msgType: " + c);
    }
}
