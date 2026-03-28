# Event Processing

---

## TL;DR

> Log4Key processes logs in two clear stages:
> Admission and Output.

Each stage has a distinct responsibility.

---

## Processing Flow

A log event goes through the following lifecycle:

```
Logger Call
↓
Admission
↓
Event Creation
↓
Output / Dispatch
↓
Appender Execution
```

---

## Admission Stage

### Role

Determines whether a log event should be created.

---

### Behavior

* evaluated before event creation
* influences isEnabled()
* avoids unnecessary object creation

---

### Result

If a log does not meet the admission level, it is ignored early.

---

## Output Stage

### Role

Determines whether an event is emitted to each appender.

---

### Behavior

* works on already created events
* each appender evaluates independently
* supports multiple outputs

---

## Separation of Responsibilities

Admission and Output serve different purposes:

* Admission → controls creation
* Output → controls emission

This separation keeps the system simple and predictable.

---

## Appender Behavior

Appenders:

* operate only on created events
* decide independently whether to emit
* do not affect earlier stages

---

## Design Characteristics

Log4Key emphasizes:

* explicit behavior
* deterministic flow
* minimal hidden logic

---

## Compared to Traditional Logging

Instead of:

* propagation chains
* filter layers

Log4Key relies on:

* clear stage boundaries
* direct routing decisions

---

## Summary

> One event is either created or not,
> then independently emitted by appenders.

