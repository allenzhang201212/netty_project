package com.netty.core;

import java.net.InetSocketAddress;
import java.util.List;

public interface AddressDirectory {
    List<InetSocketAddress> lookup(String service);
}
