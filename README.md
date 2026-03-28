# Log4Key

> 🔑 Key-based log routing for high-throughput Java systems  
> Lightweight, embeddable, and production-ready

> **Free for open source and startups · Commercial license for enterprises**

[![License: GPL-3.0](https://img.shields.io/badge/License-GPL--3.0-blue.svg)](LICENSE)
![Java](https://img.shields.io/badge/java-8+-orange)
![Build](https://img.shields.io/badge/build-passing-brightgreen)
![Status](https://img.shields.io/badge/status-active-success)
![Architecture](https://img.shields.io/badge/architecture-single--node%20%7C%20distributed-informational)
![Commercial](https://img.shields.io/badge/license-GPLv3%20%2B%20Commercial-blueviolet)

***

**Log4Key is a library, not a logging platform.**  
It is designed to be embedded into your applications or observability pipelines —  
not to power standalone log SaaS products.

***

## Overview

Log4Key is a lightweight, high-performance logging library for **fine-grained log routing**.

Instead of sending all logs to a single destination, Log4Key dynamically dispatches logs based on business keys such as `tenantId`, `orderId`, or `userId`.

This enables better isolation, observability, and scalability in multi-tenant or high-throughput systems.

***

## Key Features

- 🔑 **Key-based routing**:   
  Route logs by any field (e.g., `user_id`, `env`, `cluster`)

- 🎯 **Multi-target output**:  
  Write to multiple destinations simultaneously

- ⚡ **High-throughput design**: 
  Optimized for performance and low-latency logging

- 📦 **Embeddable**:  
  Less than 500KB JAR; no external dependencies 

- 🛠️ **Extensible sinks**:   
  Plug in custom formatters, filters, and sinks

---

## Quick Start

This example demonstrates the basic usage of Log4Key with SLF4J:

- Writing logs **without a key** (default log file)
- Writing logs **with a key**, which routes logs to separate files
- Using multiple keys in the same application
- Using SLF4J placeholders with key-based routing
- Using **Log4Key standard API** (`Log4KeyLogger`) for recommended key-based logging

### Minimal Usage

#### SLF4J style (compatible)

```java
ILogger logger = LoggerFactory.getLogger(Demo.class);

ILogKey orderKey = DefaultLogKey.of("order-1001");

// May trigger IDE warning, but works for key-based routing
logger.info("create order", orderKey);
logger.info("ship order：{}", "ship-001", orderKey);
```

#### Log4Key standard API (recommended)

```java
Log4KeyLogger logger = Log4KeyLoggerFactory.getLog4KeyLogger(Demo.class);

ILogKey orderKey = DefaultLogKey.of("order-1001");

logger.info(orderKey, "create order");
logger.info(orderKey, "ship order：{}", "ship-001");
```

### Run the Example
```bash
Linux / macOS:
./gradlew :samples:log4key-quick-start:run
```

```bat
Windows:
gradlew\.bat :samples:log4key-quick-start:run
```

> Note: Passing an `ILogKey` as an argument in SLF4J triggers an IDEA warning\
> ("More arguments provided than placeholders"), but this is required for Log4Key key-based routing.\
> Using `Log4KeyLogger` avoids this warning and is the recommended standard approach.

### What Happens

After running, check the `logs` directory:

```text
logs/info/yyyyMMdd/
├─ info.log
├─ order-1001.log
└─ order-1002.log
```

- **info.log** – logs without a key
- **order-1001.log** – logs for key "order-1001"
- **order-1002.log** – logs for key "order-1002"

#### Example Output
```text
**info.log**
Application started  
application log without key  

**order-1001.log**
create order  
pay order  
ship order：ship-001  
ship order：ship-002  

**order-1002.log**
create order
pay order
```

### Notes

- `LogManager.getInstance().shutdown()` flushes all logs. In real applications (e.g., Spring Boot), shutdown is handled automatically.
- Keys allow you to route logs into separate files based on business logic (user ID, order ID, etc.).
- **SLF4J style** is compatible and simple, but may trigger IDE warnings when passing `ILogKey`.
- **Log4Key standard API** (`Log4KeyLogger`) is recommended for production use and avoids IDE warnings.

***

## Spring Boot Integration

This example shows how to use Log4Key in a Spring Boot application.

### Minimal Usage

```text
Log4KeyLogger logger = Log4KeyLoggerFactory.getLog4KeyLogger(Demo.class);

ILogKey key = DefaultLogKey.of("user-1001");

logger.info(key, "Received message: {}", "hello");
````

### Run the Example

```bash
Linux / macOS:
./gradlew :samples:log4key-spring-boot-simple:bootRun
```

```bat
Windows:
gradlew.bat :samples:log4key-spring-boot-simple:bootRun
```

### Try It

Open in browser:

```
http://localhost:8080/log/user-1001/hello
```

### What Happens

When you send a request to `/log/{userId}/{message}`, Log4Key routes the log to a separate file based on the key (`userId`). Normal logs without a key go to the default `info.log`.


```text
logs/info/yyyyMMdd/
├─ info.log             # normal logs without a key
└─ user-1001.log        # key-based logs for user-1001
```

### Notes

* Works seamlessly with Spring Boot applications
* No special configuration required
* Key-based routing behavior is identical to plain Java usage

---

## Examples

Explore ready-to-run examples to get started quickly:

📁 **`examples/`** **directory**

- `log4key-quick-start/` – Quick Start demo for basic key-based file routing (default log file + multiple keys)
- `log4key-spring-boot-simple/` – Minimal Spring Boot integration example using Log4Key
- `log4key-config-sample/` – Configuration sample demonstrating:
    * Logger levels and level policies (`AT_LEAST` / `EXACT`)
    * Root vs. package logger routing
    * File appenders with console mirroring (`consoleEnabled`)
    * Key-based routing in practice

> All examples include build scripts and runnable demos.
> Check the logs directory after running each sample to see key-based routing in action.

---

## How It Works

### 1. Logger → Appender Routing

Loggers explicitly define where logs are written:

* `root` → console + default file
* `com.log4key.sample.business.*` → business file

This makes log routing **predictable and easy to reason about**.


### 2. File → Console Mirroring (Appender-Level Control)

Each file appender can optionally mirror logs to console:

```xml
<consoleEnabled>true</consoleEnabled>
```

* `true` → also print to console (if console appender is bound)
* `false` → file-only logging

By default, this is enabled (equivalent to true).
This avoids duplicating logger configuration.


### 3. Level Filtering

Log4Key supports two level policies:

| Policy   | Behavior                 |
| -------- |--------------------------|
| AT_LEAST | \>= configured level    |
| EXACT    | == configured level only |

Example:

```text
<level>WARN</level>
<levelPolicy>EXACT</levelPolicy>
```

→ Only `WARN` logs will be written (no `ERROR`).


### 4. Design Principles

Log4Key configuration is built around a few core principles:

* **Explicit over implicit**
  Nothing is automatically enabled — everything must be configured.

* **Separation of responsibilities**

    * Logger → routing
    * Appender → output behavior

* **Composable behavior**
  File appenders can optionally mirror logs to console.

* **Minimal but powerful**
  Covers real-world needs without overwhelming configuration complexity.

---

## Configuration (XML)

Log4Key provides a flexible and structured XML-based configuration system.
It is designed to be **easy to understand**, **explicit in behavior**, and **powerful enough for real-world use cases**.

### 1. Key Features

* **Clear separation of concerns**: configuration / formatters / appenders / loggers
* **Flexible log routing**: route logs by package and key
* **Appender-level control**: file appenders can optionally mirror logs to console
* **Simple level filtering**: supports both `AT_LEAST` and `EXACT` policies
* **No hidden behavior**: all outputs must be explicitly configured


### 2. Example Configuration

```xml
<?xml version="1.0" encoding="UTF-8"?>
<logkey>

    <!-- Global Configuration -->
    <configuration>
        <defaultLevel>INFO</defaultLevel>
        <defaultDirectory>./logs</defaultDirectory>
        <defaultCharset>UTF-8</defaultCharset>

        <executor>
            <threads>4</threads>
            <queueSize>8192</queueSize>
        </executor>

        <shutdownHook>true</shutdownHook>
    </configuration>

    <!-- Formatters -->
    <formatters>
        <formatter name="TEXT_DEFAULT">
            <type>Text</type>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} %5level [%thread] %logger{36} : %msg%n</pattern>
        </formatter>
    </formatters>

    <!-- Appenders -->
    <appenders>

        <!-- Console Appender (must be explicitly referenced) -->
        <console name="SAMPLE_CONSOLE">
            <level>INFO</level>
            <formatter>
                <type>Text</type>
                <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger : %msg</pattern>
            </formatter>
        </console>

        <!-- Default File -->
        <file name="DEFAULT_FILE">
            <directory>./logs/default</directory>
            <level>INFO</level>

            <!-- Level policy:
                 AT_LEAST = >= level
                 EXACT    = == level -->
            <levelPolicy>AT_LEAST</levelPolicy>

            <!-- Also output to console (if console appender is bound) -->
            <consoleEnabled>true</consoleEnabled>

            <formatter ref="TEXT_DEFAULT"/>
        </file>

        <!-- Business File -->
        <file name="BUSINESS_FILE">
            <directory>./logs/business</directory>
            <level>WARN</level>

            <!-- Only WARN logs (no ERROR) -->
            <levelPolicy>EXACT</levelPolicy>

            <!-- Disable console output -->
            <consoleEnabled>false</consoleEnabled>

            <formatter ref="TEXT_DEFAULT"/>
        </file>

    </appenders>

    <!-- Loggers -->
    <loggers>
        <root level="INFO">
            <appender-ref>SAMPLE_CONSOLE</appender-ref>
            <appender-ref>DEFAULT_FILE</appender-ref>
        </root>

        <logger name="com.log4key.sample.business.*" level="WARN">
            <appender-ref>BUSINESS_FILE</appender-ref>
        </logger>
    </loggers>

</logkey>
```

### 3. What Happens

With the above configuration:

```text
logs/
├─ default/
│  └─ error/       
│  └─ info/        # root logs (INFO and above)
│  └─ warn/        
└─ business/
   └─ warn/        # only WARN logs from business package
```

### 4. Notes

* You do **not** need to define all sections to get started
* JSON formatting is intentionally not built-in
  → users can plug in their own formatter if needed
* This configuration can be used as a **production-ready baseline**

---

## Installation

Add Log4Key as a dependency in your project.

### Maven

```xml
<dependency>
    <groupId>com.log4key</groupId>
    <artifactId>log4key-all</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Gradle
```groovy
dependencies {
implementation 'com.log4key:log4key-all:1.0.0'
}
```

⚠️ Note: Replace 1.0.0 with the latest release version.

---

## Use Cases

- **Multi-tenant SaaS**: Isolate logs per customer (`tenantId`)
- **Order/event tracing**: Group logs by `orderId` or `eventId`
- **Region-aware logging**: Route by `region` or `cluster`
- **Observability pipelines**: Pre-filter and fan-out logs before ingestion
- **Internal tooling**: Build custom log routers for dev/test environments

---

## Documentation

### Core Concepts
- [Logger Model](docs/concepts/logger-model.md)
- [Key Routing](docs/concepts/key-routing.md)
- [Event Processing](docs/concepts/event-processing.md)

### Runtime Model
- [Runtime Model](docs/runtime/runtime-model.md)
- [Output Strategy](docs/runtime/output-strategy.md)

### Design
- [Design Decisions](docs/design/design-decisions.md)

---

## License & Legal Documents

Log4Key is offered under a **dual-license model**:

### License Types

1. **Community Edition** – Licensed under [GNU GPL v3](LICENSE). Free for open source and internal use
2. **Startup License** – Free commercial license for eligible early-stage companies
3. **Enterprise License** – For proprietary redistribution, SaaS, or large-scale deployments

See [`LICENSING.md`](LICENSING.md) for full details, eligibility, and usage rights.

### Commercial & Legal Inquiries

For commercial inquiries:\
📧 <legal@log4key.com>

### Documents

- 📄 [LICENSE](LICENSE) – GNU GPLv3 (Community Edition)
- 📄 [Startup License Terms](licenses/STARTUP_LICENSE.md) – Free commercial use for eligible startups
- 📝 [Individual CLA](cla/ICLA.md) – For personal contributors
- 🏢 [Corporate CLA](cla/CCLA.md) – For company-sponsored contributions
- ℹ️ Full licensing model: [LICENSING.md](LICENSING.md)

---

## FAQ

### Can I use Log4Key in my SaaS product?

Yes. You may embed Log4Key within your applications, including SaaS platforms or internal infrastructure.\
However, offering Log4Key primarily as a standalone **Log Routing-as-a-Service (LRaaS)** product requires a commercial license. See [`LICENSING.md`](LICENSING.md) for details.

### We used the GPL version, now our company is growing. What should we do?

Apply for a **Startup or Enterprise commercial license**. We provide guidance to safely transition your usage without disrupting logs.

### Do students or personal projects require a license?

No. GPL allows free usage for open source or personal projects.

---

## Contributing

We welcome contributions! Please read [`CONTRIBUTING.md`](CONTRIBUTING.md) and sign the CLA before submitting pull requests.

---

## Summary

| User Type  | License            | Copyleft? | Cost  |
| ---------- | ------------------ | --------- | ----- |
| Community  | GNU GPLv3          | Yes\*     | Free  |
| Startup    | Startup License    | No        | Free† |
| Enterprise | Commercial License | No        | Paid  |

\* Only applies when distributing Log4Key or modified versions\
† Subject to eligibility

---

Have questions?\
→ Read the full [GPLv3 license](LICENSE)\
→ Review the [Startup License terms](licenses/STARTUP_LICENSE.md)\
→ Contact us: <contact@log4key.com>
