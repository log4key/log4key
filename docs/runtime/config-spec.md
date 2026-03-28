# Configuration

---

## TL;DR

> Log4Key uses a structured XML configuration to control
> logging behavior across admission, formatting, and output stages.

The configuration is organized into four sections:

* configuration
* formatters
* appenders
* loggers

---

## Configuration Structure

The root element is:

<logkey>

It contains the following sections:

* <configuration> — global runtime settings
* <formatters> — log format definitions
* <appenders> — output targets
* <loggers> — logger routing

---

## Global Settings

The <configuration> section defines system-wide behavior.

| Key                | Type    | Default | Description                                               |
| ------------------ | ------- | ------- | --------------------------------------------------------- |
| defaultLevel       | string  | INFO    | Default admission level when no logger level is specified |
| defaultDirectory   | string  | ./logs  | Default directory for file-based appenders                |
| defaultCharset     | string  | UTF-8   | Default output encoding                                   |
| executor.threads   | int     | 4       | Number of async worker threads                            |
| executor.queueSize | int     | 8192    | Maximum async queue size                                  |
| shutdownHook       | boolean | true    | Flush logs on JVM shutdown                                |

These settings control performance, resource usage, and defaults.

---

## Formatters

Formatters define how a log event is rendered.

Each formatter has:

* name — unique identifier
* type — Text or Json

---

### Text Formatter

The Text formatter uses a pattern-based layout.

Supported placeholders:

* %d{...} — timestamp
* %level — log level
* %thread — thread name
* %logger{N} — logger name
* %msg — message
* %n — newline

Example:

%d{yyyy-MM-dd HH:mm:ss.SSS} %5level [%thread] %logger{36} : %msg%n

---

### Json Formatter

The Json formatter outputs structured logs.

| Field         | Type    | Description          |
| ------------- | ------- | -------------------- |
| timestamp     | string  | ISO8601 format       |
| includeLevel  | boolean | include level field  |
| includeLogger | boolean | include logger field |
| includeThread | boolean | include thread field |
| includeMdc    | boolean | include MDC context  |

---

## Appenders

Appenders define where logs are written.

Supported types:

* console
* file

---

### Common Properties

| Property       | Type          | Description            |
| -------------- | ------------- | ---------------------- |
| name           | string        | unique identifier      |
| level          | string        | output level threshold |
| charset        | string        | output encoding        |
| consoleEnabled | boolean       | also output to console |
| formatter      | ref or inline | formatting definition  |

Inline formatter overrides referenced formatter.

---

### Console Appender

Writes logs to standard output.

No additional properties are required.

---

### File Appender

Writes logs to files.

| Property    | Type   | Description       |
| ----------- | ------ | ----------------- |
| directory   | string | output directory  |
| levelPolicy | string | EXACT or AT_LEAST |

levelPolicy controls how levels are matched:

* AT_LEAST → includes higher levels
* EXACT → only exact match

---

## Loggers

Loggers define routing from code to appenders.

---

### Root Logger

The root logger handles all unmatched logs.

It must define:

* level
* at least one appender reference

---

### Named Loggers

Named loggers match by name.

| Property     | Description            |
| ------------ | ---------------------- |
| name         | logger name or package |
| level        | admission level        |
| appender-ref | output targets         |

Matching supports:

* exact match
* wildcard patterns (com.example.*)

---

### Routing Model

Log4Key uses **explicit routing**.

* log events are routed directly to configured appenders
* no implicit propagation chain is required

This keeps behavior predictable and easy to reason about.

---

## Default vs Custom Configuration

| Aspect           | Default          | Custom           |
| ---------------- | ---------------- | ---------------- |
| Console appender | CONSOLE          | DEFAULT_CONSOLE  |
| File appenders   | FILE             | custom names     |
| Root logger      | console only     | multiple outputs |
| Business loggers | disabled         | enabled          |
| JSON output      | optional         | commonly used    |

---

## Best Practices

### Naming

* use clear prefixes: CONSOLE_, FILE_, JSON_
* align logger names with package structure

---

### Performance

* use INFO or higher in production
* tune queueSize for high throughput

---

### Observability

* use JSON appenders for structured logs
* separate audit logs when needed

---

### Debugging

* enable DEBUG selectively per module

---

## Summary

> Configuration defines how logs are admitted, formatted, and emitted.

A well-structured configuration enables:

* predictable behavior
* clear routing
* scalable performance

