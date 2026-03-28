# Design Decisions

---

## TL;DR

> Log4Key is a key-based logging system, not a hierarchical logger framework.
> To ensure deterministic behavior and simplicity, it does not support additivity or filter mechanisms.

---

## Core Difference

Traditional frameworks:

```
logger → appender → output
```

Log4Key:

```
log event → key → routing → output
```

---

## Why No Additivity?

### What is Additivity?

Additivity means logs propagate to parent loggers.

---

### Problems in Log4Key Context

#### 1. Breaks Determinism

A log may go to multiple unpredictable outputs.

---

#### 2. Conflicts with Key Routing

* additivity = static hierarchy
* key routing = dynamic decision

---

#### 3. Performance Cost

* extra propagation
* multiple writes

---

### Decision

* No additivity
* One log → one routing decision

---

## Why No Filter?

### What is Filter?

Filter decides whether to write a log after it is created.

---

### Problems

#### 1. Redundant Layer

Routing already determines the output destination.

---

#### 2. Unclear Behavior

Example:

```properties
logger.level = INFO
filter = WARN
```

Users cannot easily tell:

* Was the log filtered out?
* Or was it never emitted?

---

#### 3. Performance Overhead

* extra condition checks
* chain evaluation

---

## Alternative: Explicit Level Policy

Example:

```properties
appender.file.level = INFO
appender.file.levelPolicy = AT_LEAST
```

| Policy   | Behavior              |
| -------- | --------------------- |
| EXACT    | only exact level      |
| AT_LEAST | include higher levels |

---

## Runtime Constraints

---

### No Hot Reload

Log4Key does not support runtime configuration mutation.

All configuration changes are applied through process restart to preserve:

* Deterministic execution paths
* Stable performance characteristics
* Consistent log ordering guarantees

Introducing runtime reconfiguration would require dynamic routing changes, component switching, and additional synchronization, which would impact critical execution paths and introduce non-deterministic behavior.

→ See: [Runtime Model](runtime-model.md)

---

### Deterministic Execution Model

Log4Key treats threading and queue topology as part of the execution model rather than tunable runtime parameters.

A log event follows a stable processing path:

Log Event → Key → Queue → Worker Thread → Output

This establishes a key guarantee:

> Events with the same key are processed in a consistent execution context and in order.

This guarantee depends on a stable execution topology throughout the process lifecycle.

Allowing runtime mutation would break this assumption and introduce inconsistencies that cannot be reliably detected or corrected.

For this reason, the execution model remains fixed once the process starts.

→ See: [Runtime Model](runtime-model.md)

---


## Design Principles

1. Determinism over flexibility
2. Simplicity over abstraction
3. Predictable performance
4. Low cognitive load

---

## Final Note

> Missing features are not limitations — they are intentional design choices.

