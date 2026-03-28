# 配置说明

---

## 省流版

> Log4Key 通过结构化的 XML 配置，
> 控制日志的准入、格式化与输出行为。

配置由四个部分组成：

* configuration
* formatters
* appenders
* loggers

---

## 配置结构

根元素为：

<logkey>

包含以下模块：

* <configuration>：全局运行参数
* <formatters>：日志格式定义
* <appenders>：输出目标
* <loggers>：日志路由

---

## 全局配置

<configuration> 用于定义系统级行为。

| 键                  | 类型  | 默认值    | 说明            |
| ------------------ | --- | ------ | ------------- |
| defaultLevel       | 字符串 | INFO   | 默认准入级别        |
| defaultDirectory   | 字符串 | ./logs | 默认日志目录        |
| defaultCharset     | 字符串 | UTF-8  | 默认字符编码        |
| executor.threads   | 整数  | 4      | 异步线程数         |
| executor.queueSize | 整数  | 8192   | 队列容量          |
| shutdownHook       | 布尔  | true   | JVM 关闭时 flush |

这些参数影响性能与资源使用。

---

## 格式化器

用于定义日志的输出格式。

每个 formatter 包含：

* name：唯一标识
* type：Text 或 Json

---

### 文本格式（Text）

支持占位符：

* %d{...} 时间
* %level 日志级别
* %thread 线程名
* %logger{N} Logger 名
* %msg 日志内容
* %n 换行

---

### JSON 格式

用于结构化日志输出。

| 字段            | 类型  | 说明          |
| ------------- | --- | ----------- |
| timestamp     | 字符串 | ISO8601     |
| includeLevel  | 布尔  | 是否包含 level  |
| includeLogger | 布尔  | 是否包含 logger |
| includeThread | 布尔  | 是否包含 thread |
| includeMdc    | 布尔  | 是否包含上下文     |

---

## 输出（Appenders）

定义日志输出位置：

* console
* file

---

### 通用属性

| 属性             | 类型    | 说明        |
| -------------- | ----- | --------- |
| name           | 字符串   | 唯一标识      |
| level          | 字符串   | 输出级别      |
| charset        | 字符串   | 编码        |
| consoleEnabled | 布尔    | 是否同时输出控制台 |
| formatter      | 引用或内联 | 格式定义      |

---

### Console

输出到控制台。

无需额外配置。

---

### File

输出到文件。

| 属性          | 类型  | 说明   |
| ----------- | --- | ---- |
| directory   | 字符串 | 输出目录 |
| levelPolicy | 字符串 | 匹配策略 |

策略说明：

* AT_LEAST：大于等于
* EXACT：完全匹配

---

## 日志器（Loggers）

定义日志从代码到输出的路由。

---

### Root Logger

处理所有未匹配日志。

必须包含：

* level
* appender-ref

---

### 命名 Logger

| 属性           | 说明          |
| ------------ | ----------- |
| name         | Logger 名或包名 |
| level        | 准入级别        |
| appender-ref | 输出目标        |

支持：

* 精确匹配
* 通配符匹配（com.example.*）

---

### 路由模型

Log4Key 使用**显式路由**：

* 日志直接发送到配置的 Appender
* 不依赖隐式传播机制

这种方式使行为更清晰、更可控。

---

## 默认与自定义对比

| 配置项    | 默认配置    | 自定义配置           |
| --------- | ------- | --------------- |
| Console   | CONSOLE | DEFAULT_CONSOLE |
| File appenders   | FILE    | 自定义             |
| Root logger      | CONSOLE | 多输出             |
| Business loggers | 关闭      | 启用              |
| JSON output      | 可选      | 常用              |

---

## 最佳实践

### 命名

* 使用前缀：CONSOLE_ / FILE_ / JSON_
* 与包结构保持一致

---

### 性能

* 生产环境使用 INFO 以上
* 根据吞吐调整队列

---

### 可观测性

* 使用 JSON 输出结构化日志
* 审计日志独立输出

---

### 调试

* 按模块开启 DEBUG

---

## 总结

> 配置决定日志如何被创建、格式化与输出。

合理的配置可以带来：

* 行为可预测
* 路由清晰
* 性能稳定

