# Log4Key Spring Boot Starter 接口说明

> 模块：`log4key-spring-boot-starter`（group `com.log4key`，**独立版本**，当前 `0.1.0`）
> 依赖方向：`log4key-spring-boot-starter → log4key-core → log4key-api`（starter 内部对 Log4Key 全部 `compileOnly`）

---

## 1. 模块定位与依赖模型

| 项 | 说明 |
| :--- | :--- |
| 职责 | Spring Boot 集成：启动初始化、生命周期关闭、`/log4key` 本地日志查询 Servlet |
| 不做 | 不实现配置解析（复用 Log4Key 既有机制）、不引入 Spring MVC、不新增 `log4key.*` 配置项 |
| Log4Key 本体 | **由应用端依赖**，最低版本 **≥ 0.3.1**（`com.log4key:log4key-all`） |
| starter 版本 | **独立版本**（与 `log4key-all` 解耦）：当前 `0.1.0`，由 `gradle.properties` 的 `starterVersion` 管理 |
| starter 内部依赖 | `compileOnly`：core / api / slf4j-api / servlet-api（运行期不传递、不锁版本） |
| 运行环境 | Java 8、Spring Boot 2.7.x（`javax.servlet` 4）、需 servlet 容器（内嵌 Tomcat 或外部容器） |
| Spring 依赖 | 仅 `spring-boot` / `spring-boot-autoconfigure`（无 `spring-boot-starter-web`、无 `spring-webmvc`） |

---

## 2. 快速接入

### Gradle

```groovy
dependencies {
    // 1) Log4Key 本体（版本由应用决定，要求 >= 0.3.1）
    implementation 'com.log4key:log4key-all:0.3.1'

    // 2) 本 starter（独立版本，当前 0.1.0）
    implementation 'com.log4key:log4key-spring-boot-starter:0.1.0'

    // 3) Servlet 容器 + 业务 Web 能力
    //    排除 spring-boot-starter-logging：Log4Key 自带 SLF4J 2.x provider，
    //    避免与 logback 1.2（SLF4J 1.7 绑定）并存
    implementation('org.springframework.boot:spring-boot-starter-web') {
        exclude group: 'org.springframework.boot', module: 'spring-boot-starter-logging'
    }
}
```

### 配置（沿用 Log4Key 既有方式，starter 不新增配置项）

把 `log4key.xml` 放在应用 classpath（如 `src/main/resources/log4key.xml`），由 core 的配置加载器自动发现：

```xml
<logkey>
    <configuration>
        <defaultLevel>INFO</defaultLevel>
        <rootDirectory>./logs</rootDirectory>   <!-- 同时是查询接口的安全边界 -->
        <defaultCharset>UTF-8</defaultCharset>
    </configuration>
    <appenders>
        <console name="CONSOLE">
            <formatter><type>Text</type></formatter>
        </console>
        <file name="FILE">
            <consoleEnabled>true</consoleEnabled>
            <!-- 未显式指定时使用模板默认：directory={level}/{date}、fileName={key}.log -->
        </file>
    </appenders>
    <loggers>
        <root level="INFO">
            <appender-ref>CONSOLE</appender-ref>
            <appender-ref>FILE</appender-ref>
        </root>
    </loggers>
</logkey>
```

---

## 3. 自动装配

| 自动配置类 | 生效条件 | 提供的 Bean |
| :--- | :--- | :--- |
| `com.log4key.spring.autoconfigure.Log4KeyAutoConfiguration` | 存在 `com.log4key.LogManager` | `Log4KeyLifecycle`、`LogQueryService` |
| `com.log4key.spring.autoconfigure.Log4KeyWebAutoConfiguration` | servlet Web 应用 且存在 `javax.servlet.http.HttpServlet` | `ServletRegistrationBean<LogsQueryServlet>`（映射 `/log4key`、`/log4key/*`） |

注册入口：`META-INF/spring.factories` 中的 `EnableAutoConfiguration`。

**可覆盖点**（均带 `@ConditionalOnMissingBean`）：自定义 `Log4KeyLifecycle`、自定义 `LogQueryService`、自定义名为 `logsQueryServletRegistration` 的注册 Bean。

---

## 4. 生命周期

| 阶段 | 行为 |
| :--- | :--- |
| Spring 启动 | `Log4KeyLifecycle.start()` → `LogManager.ensureInitialized(null)`：触发 core 既有配置加载（默认 + 用户 `log4key.{xml,properties,yaml}`）与组件初始化；**幂等**，即使已被首次 `getLogger` 隐式初始化也不重复 |
| Spring 关闭 | `Log4KeyLifecycle.stop()` → `LogManager.shutdown()`：等待已提交任务排空（flush 语义）+ 关闭全部 Appender（close 语义）+ 摘除 JVM 钩子；**幂等** |
| 进程退出 | core 注册的 JVM 关闭钩子兜底（`shutdownHook=true` 时） |
| 已知限制 | `LogManager` 为 JVM 级单例，上下文关闭后**同一 JVM 内不会重新初始化**（core 既有语义）；devtools 热重启场景不承诺可用 |

`LogQueryService` Bean 的工厂方法内同样先 `ensureInitialized`，因此 `rootDirectory` / `defaultCharset` 取到的是**配置加载后**的真实值。

---

## 5. HTTP 接口（`/log4key`）

### 5.1 端点与规则

| 请求 | 行为 | 响应 `type` |
| :--- | :--- | :--- |
| `GET /log4key`、`GET /log4key/<dir…>` | 目标是目录且**无 `file` 参数** → 只返回一级子目录 | `directory` |
| `GET /log4key/<dir…>?file=` | 目录 + **全部文件**（`file` 为空串 = 不过滤） | `directory`（多 `files` 字段） |
| `GET /log4key/<dir…>?file=10001` | 目录 + 文件名模糊匹配（`contains`，区分大小写） | `directory`（多 `files` 字段） |
| `GET /log4key/<file>?page=2` | 目标文件 → 第 2 页（每页固定 1000 行） | `file` |
| `GET /log4key/<file>` | 目标文件 → **最后一页**（回显实际页码） | `file` |

**判别顺序**：带 `page` 时只接受文件目标；不带 `page` 时先按文件尝试（目标是文件即返回内容），若目标是目录则转为目录浏览。

**排序**：子目录名升序；文件按最后修改时间降序（时间相同按文件名升序）。

### 5.2 响应格式

```json
// 目录（无 file 参数）
{"type":"directory","data":["error","info"]}

// 目录 + 文件（带 file 参数；空串=全部文件）
{"type":"directory","data":["20260910","20260911"],"files":["user-1001.log"]}

// 文件分页
{"type":"file","page":3,"data":["2026-09-10 12:13:12.628  INFO [main] demo.App : line"]}

// 错误
{"type":"error","code":404,"message":"Path does not exist: info/20260910/nope.log"}
```

响应固定 `Content-Type: application/json; charset=UTF-8`（与日志文件字符集解耦）。

### 5.3 错误码

| HTTP | 语义错误码 | 触发场景 |
| :--- | :--- | :--- |
| 400 | `INVALID_PATH` / `INVALID_ARGUMENT` / `OUTSIDE_ROOT` | 路径段非法（`..`、反斜杠、控制字符、平台非法路径）、`page` 非正整数或超 int 范围、解析后越出根目录 |
| 404 | `NOT_FOUND` / `NOT_DIRECTORY` / `NOT_FILE` | 目标不存在、期望目录却是文件、期望文件却是目录或非普通文件 |
| 413 | `PAGE_TOO_LARGE` | 单页原始字节超过 8MB 护栏 |
| 500 | `IO_ERROR` | 文件系统 IO 异常 |

错误消息只包含**相对路径**，不泄露绝对路径。

---

## 6. 安全边界

- **唯一边界**：配置项 `rootDirectory`（`Log4KeyConfiguration#getDefaultDirectory()`），不新增独立查询根配置；所有写入本就在该根之下。
- **两层校验**（`PathResolver`）：
  1. 段级预校验：按 `/` 分段，拒绝空段、`.`/`..`、反斜杠、控制字符；
  2. canonical 边界校验：对真实存在的目标求 real path，断言 `startsWith(root real path)`，覆盖符号链接逃逸、盘符路径、大小写/归一化差异。
- **只读**：不创建、不修改任何文件；目标不存在一律 404，不做“未来路径”推理。
- 容器层通常还会先拦截 `..%2f`、`%2e%2e/` 等编码穿越（Tomcat 返回 400），与本层校验形成纵深防御。

---

## 7. 依赖与限制（重要）

1. **查询只读已落盘数据**：Log4Key 为异步写入（`flushInterval` 默认约 1s），刚写入的日志可能短暂查不到，稍后刷新即可；starter 不提供强制 flush（core 无该 API）。
2. **不递归**：目录只会返回一级；文件模糊匹配仅针对当前目录。
3. **固定 1000 行/页**，不可配置；越界页返回空数组并回显页码。
4. **默认常开**：servlet Web 应用引入 starter 即注册 `/log4key*`，不提供开关（不新增配置项）。
5. 不提供关键字检索行内容、不做跨文件聚合——本接口只是轻量本地日志查看工具。

---

## 8. 类与接口参考

| 类（全限定名） | 说明 | 关键 API |
| :--- | :--- | :--- |
| `com.log4key.spring.autoconfigure.Log4KeyAutoConfiguration` | 核心自动配置 | `log4KeyLifecycle()`、`logQueryService()` |
| `com.log4key.spring.autoconfigure.Log4KeyWebAutoConfiguration` | 查询端点自动配置 | `logsQueryServletRegistration(LogQueryService)` |
| `com.log4key.spring.autoconfigure.Log4KeyLifecycle` | Spring 生命周期桥接 | `start()`、`stop()`、`isRunning()` |
| `com.log4key.spring.query.LogQueryService` | 查询核心（无 servlet 依赖） | `PAGE_SIZE`、`MAX_PAGE_BYTES`、`getRootDirectory()`、`browse(String, String)`、`readPage(String, Integer)` |
| `com.log4key.spring.query.DirectoryBrowse` | 目录浏览结果 | `getDirectories()`、`getFiles()` |
| `com.log4key.spring.query.FilePage` | 分页结果 | `getPage()`、`getLines()` |
| `com.log4key.spring.query.PathResolver` | 路径解析与安全校验 | `getRoot()`、`resolveDirectory(String)`、`resolveFile(String)` |
| `com.log4key.spring.query.QueryException` | 查询异常 | `getCode()`；`ErrorCode`：`NOT_FOUND`、`INVALID_PATH`、`INVALID_ARGUMENT`、`NOT_DIRECTORY`、`NOT_FILE`、`OUTSIDE_ROOT`、`PAGE_TOO_LARGE`、`IO_ERROR` |
| `com.log4key.spring.query.JsonWriter` | 极简 JSON 转义 | `escape(String)`、`quote(String)` |
| `com.log4key.spring.web.LogsQueryServlet` | 查询 Servlet | `LogsQueryServlet(LogQueryService)`、`doGet(...)` |

源码位置：`log4key-spring-boot-starter/src/main/java/com/log4key/spring/{autoconfigure,query,web}/`。

---

## 9. 如何查看 / 发布接口文档

三种方式，按需选择：

1. **Markdown（当前，零配置）**：直接在 GitHub / Gitea 打开本文件即可渲染：
   - 中文：`docs/api/log4key-spring-boot-starter.zh.md`
   - English：`docs/api/log4key-spring-boot-starter.md`
2. **Javadoc HTML（GitHub Pages）**：本地生成后由 Pages 托管；
   ```bash
   ./gradlew :log4key-spring-boot-starter:javadoc
   # 产物：log4key-spring-boot-starter/build/docs/javadoc/index.html
   ```
   若希望线上可点：把产物提交到 `docs/api/apidocs/`，在仓库 Settings → Pages 选择 “Deploy from a branch: master /docs”，
   得到形如 `https://<owner>.github.io/<repo>/api/apidocs/` 的链接；或用 `actions/deploy-pages` 增加一个 workflow 自动发布。
3. **javadoc.io（发布到 Maven Central 后免维护）**：CI 在 tag 时发布 Maven Central，发布后可访问
   `https://javadoc.io/doc/com.log4key/log4key-spring-boot-starter/<version>/`（需要随包发布 `-javadoc.jar`）。

> 建议：本文件（Markdown）作为“接口契约”长期维护；Javadoc HTML 作为“生成物”按发布节奏产出，避免手写与生成内容漂移。
