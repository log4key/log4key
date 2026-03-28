# Key Routing (Core Concept)

---

## TL;DR

> Log4Key routes logs by **business key**, not by logger name.

This enables:

* per-entity log isolation
* deterministic log paths
* zero configuration explosion

---

## What is Key Routing?

Traditional logging:

```
logger → appender → file
```

Log4Key:

```
log event → key → routing → file
```

> The **key** becomes the primary routing dimension.

---

## Why Key Instead of Logger?

### Traditional Limitation

Logger-based routing depends on:

* package name
* class structure

This makes it difficult to:

* separate logs by user
* isolate logs by order
* debug a single request

---

## Key-Based Routing

With Log4Key:

```java
logger.info("order created", LogKey.of("order-123"));
```

Output:

```
logs/
├── order-123.log
```

---

## Real-World Use Cases

### 1. Order-Level Logs

* key = orderId
* each order has its own log file

---

### 2. User-Level Logs

* key = userId
* isolate problematic users

---

### 3. Request Tracing

* key = requestId
* full lifecycle in one file

---

### 4. Multi-Tenant Systems

* key = tenantId
* natural tenant isolation

---

## Advantages

### 1. Natural Log Partitioning

No need to design complex directory structures.

---

### 2. Zero Configuration Explosion

You don’t need:

* hundreds of logger definitions
* dynamic config reload

---

### 3. Deterministic Output

One log event → one key → one file

---

### 4. Debugging Efficiency

Instead of:

grep across gigabytes

You get:

open one file → find everything

---

## Compared to MDC / ThreadContext

Traditional workaround:

```java
MDC.put("orderId", "123");
```

Problems:

* implicit behavior
* requires pattern config
* not first-class routing

---

### Log4Key Approach

* key is explicit
* routing is built-in
* no hidden magic

---

## Performance Consideration

Key routing is designed to be:

* O(1) routing decision
* minimal overhead
* scalable for high-cardinality keys

---

## Design Philosophy

> Logs should follow **business identity**, not code structure.

---

## Summary

Key routing transforms logging from:

* code-centric → business-centric
* static → dynamic
* implicit → explicit

---

## Final Thought

> If you can name it, you can log it — independently.

