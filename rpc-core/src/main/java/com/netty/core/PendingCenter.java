package com.netty.core;

import com.netty.common.RpcResponse;
import java.util.concurrent.*;

public final class PendingCenter {
    private static final ConcurrentHashMap<Long, CompletableFuture<RpcResponse>> TABLE = new ConcurrentHashMap<>();

    public static void put(long reqId, CompletableFuture<RpcResponse> f) {
        TABLE.put(reqId, f);

    }

    public static void complete(long reqId, RpcResponse resp) {
        CompletableFuture<RpcResponse> f = TABLE.remove(reqId);
        if (f != null)
            f.complete(resp);
    }

    private PendingCenter() {
    }
}
