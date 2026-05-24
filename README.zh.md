# Log4Key

> 🔑 基于 Key 的日志路由，用于高吞吐量 Java 系统\
> 轻量、可嵌入、生产就绪

> **对开源和初创公司免费 · 企业提供商业许可**

[!\[License: GPL-3.0\](https://img.shields.io/badge/License-GPL--3.0-blue.svg null)](LICENSE)
!\[Java]\(https\://img.shields.io/badge/java-8+-orange null)
!\[Build]\(https\://img.shields.io/badge/build-passing-brightgreen null)
!\[Status]\(https\://img.shields.io/badge/status-active-success null)
!\[Architecture]\(https\://img.shields.io/badge/architecture-single--node%20%7C%20distributed-informational null)
!\[Commercial]\(https\://img.shields.io/badge/license-GPLv3%20%2B%20Commercial-blueviolet null)

***

**Log4Key 是一个库，而非独立日志平台。**\
它设计为嵌入到应用程序或监控系统中，而不是作为独立的日志 SaaS 产品。

***

## 概览

Log4Key 是一个轻量级、高性能的日志库，用于**细粒度日志路由**。

与其将所有日志发送到单个目标，Log4Key 可以根据业务 key（如 `tenantId`、`orderId` 或 `userId`）动态分发日志。

这能够在多租户或高吞吐量系统中实现更好的隔离、可观测性和可扩展性。

***

## 核心特性

- 🔑 **基于 Key 的路由**：\
  可按任意字段路由日志（如 `user_id`、`env`、`cluster`）
- 🎯 **多目标输出**：\
  同时写入多个目的地
- ⚡ **高吞吐设计**：\
  优化性能，低延迟
- 📦 **可嵌入**：\
  小于 500KB JAR，无外部依赖
- 🛠️ **可扩展输出**：\
  可插入自定义格式化器、过滤器和输出端

***

## 快速上手

下面示例展示了 Log4Key 与 SLF4J 的基本用法：

- 写入 **无 key 的日志**（默认日志文件）
- 写入 **带 key 的日志**，日志将路由到不同文件
- 在同一应用中使用多个 key
- 在 key 路由中使用 SLF4J 占位符
- 使用 **Log4Key 标准 API**（`Log4KeyLogger`）进行推荐的 key 路由日志

### 最小用法

#### SLF4J 风格（兼容）

```java
ILogger logger = LoggerFactory.getLogger(Demo.class);

ILogKey orderKey = DefaultLogKey.of("order-1001");

// IDE 可能提示警告，但可用于 key 路由
logger.info("create order", orderKey);
logger.info("ship order：{}", "ship-001", orderKey);
```

#### Log4Key 标准 API（推荐）

```java
Log4KeyLogger logger = Log4KeyLoggerFactory.getLog4KeyLogger(Demo.class);

ILogKey orderKey = DefaultLogKey.of("order-1001");

logger.info(orderKey, "create order");
logger.info(orderKey, "ship order：{}", "ship-001");
```

### 运行示例

```bash
Linux / macOS:
./gradlew :samples:log4key-quick-start:run
```

```bat
Windows:
gradlew\.bat :samples:log4key-quick-start:run
```

> 注意：在 SLF4J 风格中传递 `ILogKey` 会触发 IDEA 警告（"参数多于占位符"），这是 key 路由所需。\
> 使用 `Log4KeyLogger` 可避免此警告，推荐生产使用。

### 执行结果

运行后查看 `logs` 目录：

```text
logs/info/yyyyMMdd/
├─ info.log
├─ order-1001.log
└─ order-1002.log
```

- **info.log** – 无 key 日志
- **order-1001.log** – key 为 "order-1001" 的日志
- **order-1002.log** – key 为 "order-1002" 的日志

#### 示例输出

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

### 注意事项

- `LogManager.getInstance().shutdown()` 会刷新所有日志。在实际应用（如 Spring Boot）中，关闭由框架自动处理。
- key 可以将日志路由到不同文件，按业务逻辑隔离（如用户 ID、订单 ID）。
- **SLF4J 风格**简单兼容，但会触发 IDE 警告；
- **Log4Key 标准 API** (`Log4KeyLogger`) 推荐生产使用，可避免警告。

***

## Spring Boot 集成

展示 Log4Key 在 Spring Boot 应用中的使用方法。

### 最小用法

```java
Log4KeyLogger logger = Log4KeyLoggerFactory.getLog4KeyLogger(Demo.class);

ILogKey key = DefaultLogKey.of("user-1001");

logger.info(key, "Received message: {}", "hello");
```

### 运行示例

```bash
Linux / macOS:
./gradlew :samples:log4key-spring-boot-simple:bootRun
```

```bat
Windows:
gradlew.bat :samples:log4key-spring-boot-simple:bootRun
```

### 尝试访问

浏览器打开：

```
http://localhost:8080/log/user-1001/hello
```

### 执行结果

发送请求到 `/log/{userId}/{message}`，日志将根据 key (`userId`) 路由到单独文件。无 key 日志写入默认 `info.log`。

```text
logs/info/yyyyMMdd/
├─ info.log             # 无 key 日志
└─ user-1001.log        # key 为 user-1001 的日志
```

### 注意事项

- 与 Spring Boot 应用无缝集成
- 无需额外配置
- key 路由行为与纯 Java 使用一致

***

## 示例目录

探索可直接运行的示例，快速上手：

📁 **`examples/`** 目录

- `log4key-quick-start/` – 基础 key 路由示例（默认日志文件 + 多 key）
- `log4key-spring-boot-simple/` – 最小 Spring Boot 集成示例
- `log4key-config-sample/` – 配置示例，演示：
  - Logger 级别及策略 (`AT_LEAST` / `EXACT`)
  - 根 logger 与包级 logger 路由
  - 文件输出可选控制台镜像 (`consoleEnabled`)
  - 实战中的 key 路由

> 所有示例均包含构建脚本和可运行演示。\
> 运行后查看 logs 目录，观察 key 路由效果。

***

## 工作原理

### 1. Logger → Appender 路由

Logger 明确指定日志输出位置：

- `root` → 控制台 + 默认文件
- `com.log4key.sample.business.*` → 业务文件

使日志路由**可预测、易理解**。

### 2. 文件 → 控制台镜像（Appender 级别控制）

每个文件 Appender 可选择是否镜像到控制台：

```xml
<consoleEnabled>true</consoleEnabled>
```

- `true` → 也输出到控制台（如果绑定控制台 Appender）
- `false` → 仅写入文件

默认开启，避免重复配置。

### 3. 级别过滤

Log4Key 支持两种级别策略：

| 策略        | 行为      |
| --------- | ------- |
| AT\_LEAST | >= 配置级别 |
| EXACT     | == 配置级别 |

示例：

```xml
<level>WARN</level>
<levelPolicy>EXACT</levelPolicy>
```

→ 仅写入 WARN 日志（不包含 ERROR）。

### 4. 设计原则

Log4Key 配置围绕核心原则：

- **显式优先**：无任何默认行为，一切需配置
- **职责分离**：
  - Logger → 路由
  - Appender → 输出行为
- **可组合行为**：文件 Appender 可选镜像控制台
- **简约但强大**：覆盖实际需求，同时避免配置复杂性

***

## 配置（XML）

Log4Key 提供灵活的 XML 配置系统，**易懂、行为显式、足够强大**。

### 1. 核心特性

- **职责清晰**：配置 / 格式化器 / Appender / Logger 分离
- **灵活路由**：可按包或 key 路由
- **路径模板系统**：用 `{date}`、`{level}`、`{key}` 组合目录与文件名
- **Appender 级别控制**：可选控制台镜像
- **简单级别过滤**：支持 `AT_LEAST` 与 `EXACT`
- **无隐藏行为**：所有输出需显式配置

### 2. 配置示例

```xml
<?xml version="1.0" encoding="UTF-8"?>
<logkey>
  <!-- 全局配置 -->
  <configuration>
    <defaultLevel>INFO</defaultLevel>
    <rootDirectory>./logs</rootDirectory>
    <defaultCharset>UTF-8</defaultCharset>
    <executor>
      <threads>4</threads>
      <queueSize>8192</queueSize>
    </executor>
    <shutdownHook>true</shutdownHook>
  </configuration>

  <!-- 格式化器 -->
  <formatters>
    <formatter name="TEXT_DEFAULT">
      <type>Text</type>
      <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} %5level [%thread] %logger{36} : %msg%n</pattern>
    </formatter>
  </formatters>

  <!-- Appenders -->
  <appenders>
    <console name="SAMPLE_CONSOLE">
      <level>INFO</level>
      <formatter>
        <type>Text</type>
        <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger : %msg</pattern>
      </formatter>
    </console>

    <file name="DEFAULT_FILE">
      <!-- 缺省的的日志输出目录与文件名 -->
<!--            <directory>/{level}/{date}</directory>-->
<!--            <fileName>{key}.log</fileName>-->
      <level>INFO</level>
      <levelPolicy>AT_LEAST</levelPolicy>
      <consoleEnabled>true</consoleEnabled>
      <formatter ref="TEXT_DEFAULT"/>
    </file>

    <file name="BUSINESS_FILE">
      <directory>business/{level}/{date}</directory>
      <fileName>{key}.log</fileName>
      <level>WARN</level>
      <levelPolicy>EXACT</levelPolicy>
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

### 3. 执行结果

```text
logs/
├─ error/20260521/
├─ info/20260521/     # root 日志（INFO 及以上）
├─ warn/20260521/
└─ business/
   └─ warn/20260521/   # 仅业务包 WARN 日志
```

每个目录下包含按 key 命名的 `.log` 文件（例如 `order-1001.log`）。

### 4. 注意事项

- 不必定义所有部分即可上手
- JSON 格式化器未内置，可根据需要自行提供
- 此配置可作为生产就绪的基础模板

### 5. 路径模板策略

Log4Key 使用组合式路径模板系统来决定日志文件的写入位置。

#### 路径组合规则

```
finalPath = rootDirectory + directory + fileName
```

| 组件             | 作用域     | 说明 |
|-----------------|-----------|------|
| `rootDirectory` | 全局       | 所有日志输出的基准目录，在 `<configuration>` 中定义一次。 |
| `directory`     | Appender  | `rootDirectory` 下的相对目录模板，支持占位符。 |
| `fileName`      | Appender  | 文件名模板。未指定时默认为 `{key}.log`。 |

#### 占位符

| 占位符     | 描述                      | 后备值 |
|-----------|---------------------------|--------|
| `{date}`  | 日期，格式 `yyyyMMdd`       | —      |
| `{level}` | 小写日志级别                | `info`（级别为 null 时） |
| `{key}`   | 来自 `LogEvent.getKey()`  | key 为 null 或空时回退到级别（小写） |

> **重要**：`{key}` 回退到级别同时对 `directory` 和 `fileName` 模板生效。当日志事件没有 key 时，如果两个模板都包含 `{key}`，级别值会同时出现在目录路径和文件名中。

#### 策略示例

**示例 A：按级别组织**

```xml
<directory>{level}/{date}</directory>
<fileName>{key}.log</fileName>
```

输出结构：

```text
logs/
└─ info/
   └─ 20260521/
      ├─ info.log
      ├─ order-1001.log
      └─ order-1002.log
```

---

**示例 B：按业务 key 组织**

```xml
<directory>{key}/{date}</directory>
<fileName>app.log</fileName>
```

输出结构：

```text
logs/
└─ order-1001/
   └─ 20260521/
      └─ app.log
```

---

**示例 C：每日单文件**

```xml
<directory>{date}</directory>
<fileName>app.log</fileName>
```

输出结构：

```text
logs/
└─ 20260521/
   └─ app.log
```

---

> ⚠️ **高基数 key 风险**：当 `{key}` 用于 `fileName` 时，大量不同的 key 将产生大量小文件。对于高基数场景，建议将 `{key}` 放在 `directory` 模板中而非 `fileName`。

***

## 安装

将 Log4Key 添加为项目依赖。

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

⚠️ 注意：将 1.0.0 替换为最新版本。

***

## 使用场景

- **多租户 SaaS**：按客户隔离日志 (`tenantId`)
- **订单/事件跟踪**：按 `orderId` 或 `eventId` 分组日志
- **区域感知日志**：按 `region` 或 `cluster` 路由
- **观测管道**：日志预处理与分发
- **内部工具**：自定义日志路由器用于开发/测试环境

***

## 文档

### 核心概念

- [Logger 模型](docs/concepts/logger-model.zh.md)
- [Key 路由](docs/concepts/key-routing.zh.md)
- [事件处理](docs/concepts/event-processing.zh.md)

### 运行时模型

- [Runtime 模型](docs/runtime/runtime-model.zh.md)
- [输出策略](docs/runtime/output-strategy.zh.md)

### 设计

- [设计取舍](docs/concepts/design-decisions.zh.md)

***

## 许可与法律文档

Log4Key 采用**双许可模式**：

### 许可类型

1. **社区版** – [GNU GPL v3](LICENSE) 许可，开源和内部使用免费
2. **初创公司许可** – 符合条件的早期公司免费商业使用
3. **企业许可** – 用于专有再分发、SaaS 或大规模部署

详情见 [`LICENSING.md`](LICENSING.md)。

### 商业及法律咨询

📧 <legal@log4key.com>

### 文档

- 📄 [LICENSE](LICENSE) – GNU GPLv3（社区版）
- 📄 [Startup License 条款](licenses/STARTUP_LICENSE.md) – 初创公司免费商业使用
- 📝 [个人 CLA](cla/ICLA.md) – 个人贡献者
- 🏢 [公司 CLA](cla/CCLA.md) – 公司赞助贡献者
- ℹ️ 完整许可模式： [LICENSING.md](LICENSING.md)

---

## FAQ

### 我可以在 SaaS 产品中使用 Log4Key 吗？

可以。你可以将 Log4Key 嵌入应用，包括 SaaS 平台或内部基础设施。\
但是，如果主要将 Log4Key 提供为独立的 **日志路由即服务 (LRaaS)** 产品，则需要商业许可。详见 [`LICENSING.md`](LICENSING.md)。

### 我们使用了 GPL 版本，现在公司规模扩大，怎么办？

可申请 **Startup 或 Enterprise 商业许可**，我们提供迁移指导，确保日志使用不受影响。

### 学生或个人项目需要许可吗？

不需要。GPL 允许开源或个人项目免费使用。

---

## 贡献

欢迎贡献！请阅读 [`CONTRIBUTING.md`](CONTRIBUTING.md) 并签署 CLA 后提交 PR。

---

## 总结

| 用户类型 | 许可类型            | Copyleft? | 成本  |
| ---- | --------------- | --------- | --- |
| 社区   | GNU GPLv3       | 是\*       | 免费  |
| 初创公司 | Startup License | 否         | 免费† |
| 企业   | 商业许可            | 否         | 收费  |

\* 仅适用于分发 Log4Key 或修改版本\
† 需满足资格要求

---

有问题？\
→ 阅读完整 [GPLv3 许可](LICENSE)\
→ 查看 [Startup License 条款](licenses/STARTUP_LICENSE.md)\
→ 联系我们: <contact@log4key.com>
