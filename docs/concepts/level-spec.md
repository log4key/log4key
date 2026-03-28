# Level Model

---

## TL;DR

> In Log4Key, a level is not a global concept.
> It is always tied to a specific stage in the logging pipeline.

There are two level roles:

* Admission Level
* Output Level

---

## Design Overview

Log4Key separates level semantics to avoid ambiguity found in traditional logging systems.

Instead of one overloaded "level", Log4Key uses:

* Admission Level → controls event creation
* Output Level → controls event emission

---

## Two-Level Model

A log event flows through two distinct stages:

* Admission
* Output

Each stage interprets level differently.

---

## Configuration Mapping

| Configuration            | Meaning                    |
| ------------------------ | -------------------------- |
| defaultLevel             | global admission threshold |
| logger.xxx.level         | logger admission level     |
| root.level               | root admission level       |
| appender.xxx.level       | appender output level      |
| appender.xxx.levelPolicy | output policy              |

This mapping keeps configuration familiar while making internal behavior explicit.

---

## Admission Level

### What it Does

Admission Level determines:

> Whether a log event should be created.

---

### Key Characteristics

* evaluated before event creation
* drives isEnabled() behavior
* directly impacts performance

---

### Behavior

If a log level is below the admission level, the event is simply not created.

---

## Output Level

### What it Does

Output Level determines:

> Whether an already created event should be emitted by a specific appender.

---

### Output Policies

* EXACT → only matching level
* AT_LEAST → include higher levels

---

### Behavior

Each appender independently decides whether to emit the event.

---

## Why This Model?

Traditional logging often mixes:

* filtering
* thresholds
* propagation

Log4Key separates these concerns into clear stages.

---

## What Log4Key Does Differently

* does not rely on propagation (additivity)
* avoids filter chains
* keeps level semantics stage-specific

---

## Summary

> A level only has meaning within its stage.

By separating admission and output, Log4Key achieves:

* clearer behavior
* predictable performance
* easier debugging

