# 输出策略

---

## 省流版

> Log4Key 将文件输出与控制台展示解耦，
> 以保证日志输出行为确定且不会重复。

一条日志：

* 可以写入多个文件
* 在控制台最多出现一次

---

## 设计目标

输出系统的设计目标是：

* 行为可预测
* 语义清晰
* 观测一致
* 避免重复输出

---

## 核心模型

日志事件只会被创建一次，然后交由不同输出组件处理。

系统中存在两个角色：

* File Appender：负责持久化与分流
* Console：负责统一展示

---

## LogEvent

LogEvent 表示一次日志调用。

包含：

* level
* logger 名称
* message
* timestamp
* thread
* 可选 key

LogEvent 只创建一次，并被多个 Appender 共享。

---

## File Appender

File Appender 负责日志持久化。

每个 Appender：

* 独立判断 level
* 写入自己的文件

多个 File Appender 可以同时处理同一个事件。

这使得：

* 日志分流
* 多文件写入
* 按 key 存储

成为可能。

---

## Console 统一视图

Console 是全局唯一的观察出口。

它具有：

* 全局性
* 集中性
* 不重复性

Console 的输出由 root 控制。

---

## Console 可见性模型

Console 本身不独立做输出决策。

它依赖于 Appender 的可见性标记。

---

### consoleEnabled

consoleEnabled 表示：

> 该事件是否允许在 Console 中显示

它并不会：

* 触发输出
* 生成额外日志

---

## 输出决策流程

对于一条日志：

1. 各 File Appender 独立判断
2. 标记是否允许 Console 可见
3. 使用 OR 规则合并

只要任意一个 Appender 允许：

该事件即可进入 Console 阶段。

---

## Root 控制输出

最终是否输出由 root 决定：

满足以下条件才输出：

* 允许 Console 可见
* 满足 root level

Console 输出始终使用原始日志级别。

---

## 确定性输出

该设计保证：

* Console 不会重复输出
* 输出顺序稳定
* 行为可推导

---

## 与传统日志系统对比

传统日志系统依赖：

* 继承传播
* 多 Console 绑定
* 隐式重复

Log4Key 采用：

* 显式路由
* Console 集中控制
* 可见性驱动输出

---

## 设计取舍

该模型更强调：

* 清晰性优先
* 可预测优先

避免：

* 隐式传播
* 重复输出
* 配置歧义

---

## 总结

> 一条日志可以被多次存储，
> 但只会被统一展示一次。

Console：

* 不是分支
* 不是普通输出目标

而是系统的最终观察视图。

