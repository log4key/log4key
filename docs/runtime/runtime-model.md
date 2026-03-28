# Runtime Model

---

## Overview

Log4Key is built around a deterministic execution model.
A log event follows a fixed processing path from emission to output, without runtime mutation of routing, threading, or queue topology.

This ensures:

* Predictable system behavior
* Stable performance characteristics
* Strong ordering guarantees per key

The runtime model is designed as a **static execution pipeline**, rather than a dynamically reconfigurable system.

---

## Execution Flow

A log event is processed through the following pipeline:

Log Event → Key Extraction → Queue Routing → Worker Thread → Output

Each stage is fixed during process initialization and does not change at runtime.

---

## Key-Based Routing

Log4Key routes log events based on a user-defined key.

* Events with the same key are always routed to the same queue
* This guarantees in-order processing within a key scope
* Different keys may be processed in parallel

This model provides a balance between concurrency and ordering.

---

## Queue Model

Queues act as the boundary between log ingestion and processing.

Characteristics:

* Each queue is bound to a worker thread
* Queue topology is fixed at startup
* Queues provide buffering and decoupling

Queue behavior (e.g., capacity, backpressure strategy) is defined via configuration and remains stable during runtime.

---

## Threading Model

Log4Key uses a fixed worker-thread model.

* Each worker thread consumes from a dedicated queue
* Thread-to-queue mapping is immutable during runtime
* No dynamic scaling or reassignment occurs

This design ensures:

* Consistent execution paths
* No cross-thread reordering
* Predictable resource usage

Changing the threading model at runtime would alter event routing and break ordering guarantees.

---

## Deterministic Execution

Determinism is a core property of the runtime model.

Given the same:

* Configuration
* Input log sequence

Log4Key will produce consistent:

* Routing decisions
* Processing order
* Output results

This makes system behavior easier to reason about and debug.

---

## No Hot Reload

Log4Key does not support runtime configuration reload.

All configuration changes require a process restart.

This avoids:

* Inconsistent routing behavior
* Partial application of configuration
* Runtime-induced non-determinism

Hot reload would introduce ambiguity in execution paths and is therefore intentionally not supported.

---

## Design Rationale

The runtime model favors stability and predictability over flexibility.

Instead of allowing dynamic reconfiguration, Log4Key ensures that:

* The execution path is fixed
* The system behavior is reproducible
* Performance characteristics remain stable

This approach is particularly suitable for logging systems, where consistency and reliability are more critical than dynamic adaptability.
