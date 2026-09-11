# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [Unreleased]
### Added
- 新增 `log4key-spring-boot-starter` 模块：Spring Boot 集成
  - 自动配置：启动时 `LogManager.ensureInitialized`（复用既有配置加载），上下文关闭时 `LogManager.shutdown`（排空 + 关闭 Appender）
  - 本地日志查询端点 `/log4key`（原生 Servlet，**不引入 Spring MVC**）：目录浏览、文件名模糊查询、按 1000 行分页、默认最后一页
  - 路径安全：以配置 `rootDirectory` 为唯一边界，段级校验 + canonical（real path）边界校验
  - 依赖模型：对 core/api/slf4j/servlet-api 全部 `compileOnly`，不向应用传递；应用端需自行依赖 `com.log4key:log4key-all`（≥ 0.3.1）
- 新增接口说明文档：`docs/api/log4key-spring-boot-starter.zh.md` / `docs/api/log4key-spring-boot-starter.md`

---

## [0.3.1] - 2026-07-20
### Added
- 新增批量超时（batch timeout）机制，强制写入长时间滞留的数据，避免日志长时间停留在缓冲区
- 新增 `maintainChannels`，在 Worker 主循环中周期性处理批量超时与刷新，并补充相关单元测试

### Fixed
- 修复小字节日志无法及时写入磁盘的问题：flush 与批量超时逻辑从写入流程移出，改由 Worker 主循环周期性管理

### Changed
- 写入流程简化为两阶段（append → write）；flush 仅负责刷盘，不再承担写入边界职责

---

## [0.3.0] - 2026-07-05
### Added
- 新增 V2 异步写入架构：`FileChannel` / `FileChannelManager`（format + route + shard 在业务线程完成，仅最终 write 异步投递到 Worker）
- 新增 `ExecutorController` 注入与按 `PathKey` 分片的 Worker 路由
- 新增全局文件描述符（FD）上限计算与 LRU 淘汰策略；最大打开文件数由「每 Worker 平分」改为「全局限制」
- 新增 `storage` 配置节点解析（`maxOpenFiles`、`batchSize`、`flushInterval`、`highWaterMark`、`initialBufferSize`）
- 新增日志事件拒绝计数统计（Mailbox 背压 Level3 拒绝次数）
- 新增 `PathKey`、`FileChannel`、`FileChannelManager` 及 V2 配置键的单元测试

### Changed
- 重构 `FileAppender`：移除旧的 `LogFileWriter` 依赖与同步写入逻辑，改为 `executeWrite` 异步投递
- 分离写入与刷新：`shouldWrite`/`write` 与 `shouldFlush`/`flush` 独立触发；Worker 采用 append → write → flush 三阶段处理
- 配置项 `maxFileWriters` 重命名为 `maxOpenFiles`，并迁移到 `storage` 节点（同步更新 XML 配置与转换器）
- 最大打开文件数默认值 64 → 1024，并修正相关单元测试断言
- 重构核心度量指标与配置管理：统一 `IoMetrics` / `LogMetrics` 的事件计数职责
- 优化文件通道管理器的系统兼容性与性能监控：改用 `UnixOperatingSystemMXBean` 获取 FD 上限（移除反射调用）、增加缓存命中率统计
- 优化工作线程实现：`Thread.sleep` 改为 `LockSupport.parkNanos`、增加中断检查、精简冗余字段
- 重构 `FileChannel` 的 write / flush 分离机制，移除冗余注释

### Removed
- 移除废弃的存储与缓冲区实现（`StorageProvider` 接口及 `LocalFileStorageProvider`、`CircularBufferStrategy`）
- 移除废弃的批量大小配置项（`MAX_BATCH_SIZE` / `MAX_BATCH_SIZE_KEY`）
- 移除 `FileAppender` 的错误计数功能、空闲写入器清理执行器，以及 `writerIdleTimeout` / `maxBatchSize` / `maxFileWriters` 等旧配置

---

## [0.2.0] - 2026-05-24
### Added
- 添加 fileName 配置选项用于指定日志文件名模板

### Changed
- 重构路径模板引擎：支持 {date}、{level}、{key} 占位符动态生成日志路径
- 将 defaultDirectory 重命名为 rootDirectory 并更新相关配置键

### Removed
- 移除 ShardingStrategy 分片策略相关功能

---

## [0.1.4] - 2026-05-16
### Added
- 新增 JsonSerializer SPI 接口，解耦 JSON 序列化依赖
- 新增 JsonSerializerProvider 内部提供者，通过 Java SPI 机制自动发现 JsonSerializer 实现
- 移除 log4key-core 对 Gson 的直接依赖，改由用户自行选择 JSON 库
- 新增 GsonJsonSerializer 示例实现，演示如何集成第三方 JSON 库

### Changed
- JsonLogFormatter 改为通过 JsonSerializerProvider SPI 获取序列化能力

---

## [0.1.2] - 2026-04-06
### 新功能
- 增加CI自动化处理
- CI自动化构建打包
- CI自动化发布maven

---

## [0.1.1] - 2026-03-29
### Added
- Initial commit for github-master

---
