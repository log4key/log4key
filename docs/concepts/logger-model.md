# Logger Model (Non-Inheritance Design)

---

## What is Log4Key’s Logger Model?

Log4Key does **not** use the traditional hierarchical logger model (like logback or log4j2).

Instead:

> Each logger is **independent**, and log routing is determined by a **key**, not by package hierarchy.

---

## Traditional Logger Model (for comparison)

In frameworks like log4j2:

```
com.foo.bar → com.foo → root
```

* Loggers form a hierarchy
* Logs may propagate upward (additivity)
* Multiple appenders may handle the same log

---

## Log4Key Model

```
log event → key → routing → output
```

* No hierarchy
* No propagation
* One routing decision per log event

---

## Example

Java example:
```text
logger.info("order created", LogKey.of("order-123"));
```

Output:

```text
logs/  
├── order-123.log
```

---

## Why No Inheritance?

### 1. Deterministic Behavior

Each log event has a **single, predictable output path**.

---

### 2. No Duplicate Logs

No propagation means no accidental duplicate outputs.

---

### 3. Easier Reasoning

You don’t need to trace:

* parent loggers
* root logger
* additivity

---

### 4. Better for Automation

The behavior is explicit and machine-friendly.

---

## Multiple Outputs Still Supported

XML example:

```xml
<logger name="com.example">  
    <appender-ref>File</appender-ref>  
    <appender-ref>Console</appender-ref>  
</logger>  
```

You can attach multiple appenders directly to a logger.

---

## Summary

> Log4Key replaces hierarchical inheritance with **explicit routing**.

This leads to:

* predictable behavior
* simpler mental model
* better debugging experience
