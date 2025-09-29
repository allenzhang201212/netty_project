package com.netty.common;

public enum SerializeType {
    JSON((byte) 0);

    public final byte code;

    SerializeType(byte c) {
        this.code = c;
    }

    public static SerializeType from(byte c) {
        for (SerializeType t : values())
            if (t.code == c)
                return t;
        throw new IllegalArgumentException("Unkown msgType: " + c);
    }
}
