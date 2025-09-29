package com.netty.common;

public class RpcRequest {
    public long requestId;
    public String service;
    public String method;
    public Class<?>[] paramTypes;
    public Object[] args;
}
