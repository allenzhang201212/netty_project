package com.netty.common;

import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonSerializer implements Serializer {
    private static final ObjectMapper M = new ObjectMapper();

    @Override
    public byte code() {
        return SerializeType.JSON.code;
    }

    @Override
    public byte[] serialize(Object obj) {
        try {
            return M.writeValueAsBytes(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public <T> T deserialize(byte[] b, Class<T> c) {
        try {
            return M.readValue(b, c);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
