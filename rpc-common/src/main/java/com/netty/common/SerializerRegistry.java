package com.netty.common;

import java.util.HashMap;
import java.util.Map;

public final class SerializerRegistry {
    private static final Map<Byte, Serializer> BY_CODE = new HashMap<>();
    static {
        register(new JsonSerializer());
    }

    public static void register(Serializer s) {
        BY_CODE.put(s.code(), s);
    }

    public static Serializer byCode(byte code) {
        return BY_CODE.get(code);
    }

    public static Serializer defaultSerializer() {
        return BY_CODE.get(SerializeType.JSON.code);
    }

    private SerializerRegistry() {
    }
}