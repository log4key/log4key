# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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
