# Level 模型

---

## 省流版

> 在 Log4Key 中，level 不是全局概念，
> 它始终绑定在日志处理流程的某个阶段中。

系统中存在两种 level：

* 准入级别（Admission）
* 输出级别（Output）

---

## 设计概览

Log4Key 将 level 语义进行拆分，以避免传统日志系统中的歧义问题。

不同于“一个 level 多种用途”，Log4Key 中：

* Admission Level → 控制是否创建日志事件
* Output Level → 控制是否输出日志

---

## 两阶段模型

日志处理分为两个阶段：

* 准入阶段
* 输出阶段

每个阶段对 level 的理解不同。

---

## 配置映射

| 配置字段                     | 含义          |
| ------------------------ | ----------- |
| defaultLevel             | 全局准入阈值      |
| logger.xxx.level         | logger 准入级别 |
| root.level               | root 准入级别   |
| appender.xxx.level       | 输出级别        |
| appender.xxx.levelPolicy | 输出策略        |

这种设计在保持配置习惯的同时，使内部语义更加清晰。

---

## 准入级别（Admission）

### 作用

> 判断是否创建 LogEvent

---

### 特点

* 在事件创建前判断
* 决定 isEnabled() 行为
* 直接影响性能

---

### 行为说明

当日志级别低于准入级别时，事件不会被创建。

---

## 输出级别（Output）

### 作用

> 判断 Appender 是否输出该事件

---

### 输出策略

* EXACT：仅匹配
* AT_LEAST：包含更高级别

---

### 行为说明

每个 Appender 独立决定是否输出该事件。

---

## 设计差异

传统日志系统通常混合：

* filter
* threshold
* 继承传播

Log4Key 将这些概念拆分为不同阶段。

---

## Log4Key 的选择

* 不依赖 additivity 传播
* 不使用 filter 链
* level 语义按阶段划分

---

## 总结

> level 只在其所属阶段有意义。

这种设计带来：

* 行为清晰
* 性能可预测
* 调试更简单

