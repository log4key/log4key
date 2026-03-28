# Logger 模型（非继承设计）

---

## 什么是 Log4Key 的 Logger 模型？

Log4Key 不采用传统日志框架（如 logback / log4j2）的层级 Logger 模型。

相反：

> 每个 Logger **相互独立**，日志的去向由 **key 决定**，而不是包名层级。

---

## 传统日志模型（对比）

在传统日志框架中：

```
com.foo.bar → com.foo → root
```

* Logger 具有层级结构
* 日志可能向父级传播（additivity）
* 一条日志可能被多个 appender 处理

---

## Log4Key 模型

```
log event → key → routing → output
```

* 无层级
* 无传播
* 每条日志只做一次路由决策

---

## 示例

Java 示例：

```
logger.info("order created", LogKey.of("order-123"));
```

输出：

```
logs/  
├── order-123.log
```

---

## 为什么不使用继承模型？

### 1. 确定性

每条日志都有**唯一且可预测的输出路径**。

---

### 2. 避免重复日志

没有传播，就不会出现重复输出问题。

---

### 3. 更容易理解

无需理解复杂的继承链：

* 父级 logger
* root logger
* additivity

---

### 4. 更适合自动化

行为是显式的，更适合自动化工具处理。

---

## 仍然支持多输出

XML 示例：

```xml
<logger name="com.example">  
    <appender-ref>File</appender-ref>  
    <appender-ref>Console</appender-ref>  
</logger>
```

可以直接为一个 Logger 绑定多个 appender。

---

## 总结

> Log4Key 用“显式路由”替代“隐式继承”。

带来的好处：

* 行为可预测
* 模型更简单
* 更容易调试
