# Netty + ZooKeeper Distributed RPC

A distributed RPC framework built with **Spring Boot**, **Netty (NIO)**, and **ZooKeeper**. It provides service registration/discovery, long-lived connections with heartbeat/idle detection, JSON serialization, synchronous & asynchronous RPC with timeout/retry, and simple load balancing—so consumers invoke remote interfaces like local methods.

---

## Features

- **Registry/Discovery (ZooKeeper)**: Providers register under `/rpc/<service>/<instance>`; consumers watch for changes.
- **High-throughput I/O (Netty)**: Length-prefixed framing to avoid TCP sticky/half packets; back-pressure friendly.
- **Long-lived connections**: Heartbeat + idle detection to keep channels healthy.
- **Sync & Async calls**: Futures/promises, per-call timeout and retry.
- **JSON serialization**: Pluggable serializer hook.
- **Load balancing**: Round-robin by default; strategy is extensible.
- **Spring integration**: `@RpcService` on providers, `@RpcReference` on consumers.

---

## Project Structure (example)

```
.
├─ rpc-common/        # DTOs, protocol, codecs, shared utils
├─ rpc-registry/      # ZooKeeper client, registration & discovery
├─ rpc-provider/      # Demo provider (Spring Boot)
├─ rpc-consumer/      # Demo consumer (Spring Boot)
├─ pom.xml
└─ README.md
```

---

## Prerequisites

- **JDK 17+**
- **Maven 3.8+** (or the included `./mvnw`)
- **ZooKeeper 3.7+** reachable at `127.0.0.1:2181`

Quick start ZooKeeper with Docker:

```bash
docker run -d --name zk -p 2181:2181 zookeeper
```

---

## Quick Start

### 1) Configure (`application.yml`)
```yaml
rpc:
  zk: 127.0.0.1:2181
  namespace: /rpc
  serializer: json
  client:
    connectTimeoutMs: 3000
    readTimeoutMs: 5000
  heartbeat:
    intervalSec: 30
  lb: round_robin
server:
  port: 8081  # provider port; use different ports for multiple instances
```

### 2) Build
```bash
./mvnw -q -DskipTests clean package
# Windows: mvnw.cmd -q -DskipTests clean package
```

### 3) Run Provider(s)
```bash
./mvnw -q -DskipTests -f rpc-provider/pom.xml spring-boot:run \
  -Drpc.zk=127.0.0.1:2181 \
  -Dserver.port=8081
```

### 4) Run Consumer
```bash
./mvnw -q -DskipTests -f rpc-consumer/pom.xml spring-boot:run \
  -Dspring-boot.run.main-class=com.netty.consumer.ConsumerApplication \
  -Drpc.zk=127.0.0.1:2181
```

### 5) Test the Call
```bash
curl "http://localhost:8080/hello?name=Netty"
```

---

## How It Works

1. **Provider boot**: `@RpcService(Interface.class)` beans auto-register to ZooKeeper (ephemeral nodes) under `/rpc/<service>/<instance>`.
2. **Consumer injection**: `@RpcReference` creates a dynamic proxy; it discovers providers from ZooKeeper and subscribes to watch updates.
3. **Invocation**: Proxy encodes request (JSON + headers). Netty client sends a length-prefixed TCP frame over a reused channel.
4. **Server pipeline**: Frame decode → message decode → dispatch → reflectively invoke bean → encode response → send back.
5. **Resilience**: Heartbeats keep channels alive; per-call timeouts trigger retries; load balancer selects a healthy instance.

---

## Configuration Keys

| Key                           | Description                          | Default          |
|-------------------------------|--------------------------------------|------------------|
| `rpc.zk`                      | ZooKeeper connect string             | `127.0.0.1:2181` |
| `rpc.namespace`               | Root ZK path for services            | `/rpc`           |
| `rpc.serializer`              | Payload codec (pluggable)            | `json`           |
| `rpc.client.connectTimeoutMs` | Netty connect timeout (ms)           | `3000`           |
| `rpc.client.readTimeoutMs`    | Per-request timeout (ms)             | `5000`           |
| `rpc.heartbeat.intervalSec`   | Heartbeat interval (seconds)         | `30`             |
| `rpc.lb`                      | Load balancer strategy               | `round_robin`    |

---

## Example Code

**Common API**
```java
// rpc-common/src/main/java/com/example/api/HelloService.java
public interface HelloService {
    String hello(String name);
}
```

**Provider**
```java
// rpc-provider/.../HelloServiceImpl.java
@RpcService(HelloService.class)
public class HelloServiceImpl implements HelloService {
    @Override
    public String hello(String name) { return "Hello, " + name; }
}
```

**Consumer**
```java
// rpc-consumer/.../DemoController.java
@RestController
public class DemoController {

    @RpcReference
    private HelloService helloService;

    @GetMapping("/hello")
    public String call(@RequestParam String name) {
        return helloService.hello(name); // RPC call like a local method
    }
}
```

---

## Netty Pipelines (Typical)

- **Client**: `IdleStateHandler` → encoder → `LengthFieldPrepender` → socket  
- **Server**: `LengthFieldBasedFrameDecoder` → decoder → request handler → encoder

> Framing avoids TCP sticky/half-packet issues and preserves message boundaries.

---

## Troubleshooting

- **Cannot connect to ZK**: Check `rpc.zk`, ensure ZooKeeper is running and port `2181` is reachable.
- **SLF4J multiple bindings**: Exclude duplicate logging implementations; keep only Logback.
- **Port already in use**: Change `server.port` for each provider instance.
- **No provider found**: Verify `@RpcService` annotation and Spring component scanning.
- **Frequent timeouts**: Increase `readTimeoutMs`; confirm provider health/network reachability.

---

## .gitignore (Java)

```
/target/
/**/target/
.idea/
*.iml
.vscode/
.DS_Store
logs/
*.log
```

---
