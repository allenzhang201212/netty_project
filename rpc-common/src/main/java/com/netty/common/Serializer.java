package com.netty.common;

public interface Serializer {
    byte code();

    byte[] serialize(Object obj);

    <T> T deserialize(byte[] bytes, Class<T> clazz);
}
