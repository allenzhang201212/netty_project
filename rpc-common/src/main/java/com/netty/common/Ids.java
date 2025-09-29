package com.netty.common;

import java.util.concurrent.atomic.AtomicLong;

public final class Ids {
    private static final AtomicLong GEN = new AtomicLong(1);

    public static long next() {
        return GEN.getAndIncrement();
    }

    private Ids() {
    }
}
