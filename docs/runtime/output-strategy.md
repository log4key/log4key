# Output Strategy

---

## TL;DR

> Log4Key separates file persistence and console visibility
> to ensure deterministic and non-duplicated log output.

A log event:

* may be written to multiple files
* appears at most once in the console

---

## Design Goals

The output system is designed to provide:

* predictable behavior
* clear semantics
* consistent observability
* no unintended duplication

---

## Core Model

A log event is processed once and then evaluated by output targets.

There are two distinct roles:

* File appenders → persistence and routing
* Console → unified observation

---

## Log Event

A LogEvent represents a single logging action.

It contains:

* level
* logger name
* message
* timestamp
* thread
* optional key

A LogEvent is created once and shared across all appenders.

---

## File Appenders

File appenders are responsible for persistence.

Each file appender:

* evaluates its own level threshold
* writes to its own output location

Multiple file appenders may process the same event.

This enables:

* log routing
* multi-file partitioning
* key-based storage

---

## Console as a Unified View

The console acts as a single observation point.

It is:

* global
* centralized
* non-duplicating

Console output is controlled at the root level.

---

## Console Visibility Model

The console does not independently generate output decisions.

Instead, visibility is derived from appenders.

---

### consoleEnabled

The consoleEnabled flag defines:

> whether an event is allowed to appear in the console

It does not:

* trigger console output
* create additional log entries

---

## Output Decision Flow

For a given LogEvent:

1. File appenders independently evaluate the event
2. Each appender may mark the event as console-visible
3. Visibility is combined using a logical OR

If any appender allows console visibility:

the event becomes eligible for console output

---

## Root-Level Filtering

The root logger controls final console emission.

An event is printed to the console only if:

* it is console-visible
* it satisfies the root level

The console always uses the original event level.

---

## Deterministic Output

This design ensures:

* no duplicate console output
* consistent ordering
* clear ownership of output decisions

---

## Comparison with Traditional Logging

Traditional logging systems often rely on:

* propagation chains
* multiple console bindings
* implicit duplication

Log4Key instead uses:

* explicit routing
* centralized console control
* visibility-based output

---

## Design Tradeoffs

This model prioritizes:

* clarity over flexibility
* determinism over implicit behavior

It avoids:

* hidden propagation
* unexpected duplication
* configuration ambiguity

---

## Summary

> A log event may be persisted multiple times,
> but observed only once.

The console is:

* not a branch
* not a destination among many

It is the final, unified view of the system.

