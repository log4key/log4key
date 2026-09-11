# Log4Key Spring Boot Starter — API Reference

> Module: `log4key-spring-boot-starter` (group `com.log4key`, version follows the root project `version`)
> Dependency direction: `log4key-spring-boot-starter → log4key-core → log4key-api`
> All Log4Key dependencies inside the starter are `compileOnly` (not transitive, no version pinning).

---

## 1. Scope and dependency model

| Item | Description |
| :--- | :--- |
| Responsibility | Spring Boot integration: startup initialization, lifecycle shutdown, `/log4key` local log query servlet |
| Out of scope | No config parsing (reuses Log4Key's existing mechanism), no Spring MVC, no new `log4key.*` properties |
| Log4Key runtime | **Provided by the application**, minimum version **≥ 0.3.1** (`com.log4key:log4key-all`) |
| Starter's own deps | `compileOnly`: core / api / slf4j-api / servlet-api |
| Runtime requirements | Java 8, Spring Boot 2.7.x (`javax.servlet` 4), a servlet container (embedded Tomcat or external) |
| Spring deps | Only `spring-boot` / `spring-boot-autoconfigure` (no `spring-boot-starter-web`, no `spring-webmvc`) |

---

## 2. Getting started

### Gradle

```groovy
dependencies {
    // 1) Log4Key itself (version chosen by the application, >= 0.3.1)
    implementation 'com.log4key:log4key-all:0.3.1'

    // 2) This starter
    implementation 'com.log4key:log4key-spring-boot-starter:<starter-version>'

    // 3) Servlet container + web endpoints
    //    Exclude spring-boot-starter-logging: Log4Key ships its own SLF4J 2.x provider
    //    and must not be mixed with logback 1.2 (SLF4J 1.7 binding)
    implementation('org.springframework.boot:spring-boot-starter-web') {
        exclude group: 'org.springframework.boot', module: 'spring-boot-starter-logging'
    }
}
```

### Configuration (existing Log4Key mechanism; the starter adds none)

Put `log4key.xml` on the application classpath (e.g. `src/main/resources/log4key.xml`); it is discovered by the core configuration loader:

```xml
<logkey>
    <configuration>
        <defaultLevel>INFO</defaultLevel>
        <rootDirectory>./logs</rootDirectory>   <!-- also the security boundary of the query API -->
        <defaultCharset>UTF-8</defaultCharset>
    </configuration>
    <appenders>
        <console name="CONSOLE">
            <formatter><type>Text</type></formatter>
        </console>
        <file name="FILE">
            <consoleEnabled>true</consoleEnabled>
            <!-- defaults when omitted: directory={level}/{date}, fileName={key}.log -->
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

## 3. Auto-configuration

| Auto-configuration class | Condition | Beans |
| :--- | :--- | :--- |
| `com.log4key.spring.autoconfigure.Log4KeyAutoConfiguration` | `com.log4key.LogManager` present | `Log4KeyLifecycle`, `LogQueryService` |
| `com.log4key.spring.autoconfigure.Log4KeyWebAutoConfiguration` | servlet web application and `javax.servlet.http.HttpServlet` present | `ServletRegistrationBean<LogsQueryServlet>` mapped to `/log4key`, `/log4key/*` |

Registered through `META-INF/spring.factories` (`EnableAutoConfiguration`).

**Extension points** (all guarded by `@ConditionalOnMissingBean`): custom `Log4KeyLifecycle`, custom `LogQueryService`, or a custom registration bean named `logsQueryServletRegistration`.

---

## 4. Lifecycle

| Phase | Behavior |
| :--- | :--- |
| Spring startup | `Log4KeyLifecycle.start()` → `LogManager.ensureInitialized(null)`: triggers the core config loading (default + user `log4key.{xml,properties,yaml}`) and component initialization; **idempotent** |
| Spring shutdown | `Log4KeyLifecycle.stop()` → `LogManager.shutdown()`: drains queued writes (flush) + closes all appenders + removes the JVM hook; **idempotent** |
| Process exit | Core's JVM shutdown hook acts as a fallback (when `shutdownHook=true`) |
| Known limitation | `LogManager` is a JVM-level singleton: after shutdown it is **not re-initialized in the same JVM** (core semantics); devtools restarts are not supported |

The `LogQueryService` bean factory also calls `ensureInitialized` first, so `rootDirectory` / `defaultCharset` are read **after** configuration loading.

---

## 5. HTTP API (`/log4key`)

### 5.1 Endpoints and rules

| Request | Behavior | Response `type` |
| :--- | :--- | :--- |
| `GET /log4key`, `GET /log4key/<dir…>` | directory target and **no `file` parameter** → immediate sub-directories only | `directory` |
| `GET /log4key/<dir…>?file=` | directory + **all files** (empty value = no filter) | `directory` (with `files`) |
| `GET /log4key/<dir…>?file=10001` | directory + fuzzy file-name match (`contains`, case-sensitive) | `directory` (with `files`) |
| `GET /log4key/<file>?page=2` | file target → page 2 (fixed 1000 lines per page) | `file` |
| `GET /log4key/<file>` | file target → **last page** (actual page number echoed) | `file` |

**Dispatch order**: with `page`, only a file target is accepted; without `page`, a file is tried first (content if the target is a file), otherwise it falls back to directory browsing.

**Ordering**: sub-directories ascending by name; files descending by last-modified time (ties by name ascending).

### 5.2 Response shapes

```json
// directory (no file parameter)
{"type":"directory","data":["error","info"]}

// directory + files (file parameter present; empty string = all files)
{"type":"directory","data":["20260910","20260911"],"files":["user-1001.log"]}

// file page
{"type":"file","page":3,"data":["2026-09-10 12:13:12.628  INFO [main] demo.App : line"]}

// error
{"type":"error","code":404,"message":"Path does not exist: info/20260910/nope.log"}
```

Responses are always `Content-Type: application/json; charset=UTF-8` (independent of the log file charset).

### 5.3 Error codes

| HTTP | Error code | Trigger |
| :--- | :--- | :--- |
| 400 | `INVALID_PATH` / `INVALID_ARGUMENT` / `OUTSIDE_ROOT` | illegal path segment (`..`, backslash, control chars, platform-invalid path), `page` not a positive int or out of int range, resolved path escaping the root |
| 404 | `NOT_FOUND` / `NOT_DIRECTORY` / `NOT_FILE` | target missing, expected directory but found file, expected file but found directory/non-regular file |
| 413 | `PAGE_TOO_LARGE` | a single page exceeds the 8 MB raw-byte guard |
| 500 | `IO_ERROR` | filesystem I/O failure |

Error messages only contain **relative** paths; absolute paths are never leaked.

---

## 6. Security boundary

- **Single boundary**: the `rootDirectory` configuration key (`Log4KeyConfiguration#getDefaultDirectory()`); no separate query-root configuration, and all appenders already write below it.
- **Two validation layers** (`PathResolver`):
  1. segment pre-validation: split on `/`, reject empty segments, `.`/`..`, backslashes and control characters;
  2. canonical boundary check: resolve the real path of existing targets and assert `startsWith(root real path)` — covers symlink escapes, drive-letter paths and case/normalization differences.
- **Read-only**: no file is created or modified; a missing target is always 404, never "predicted".
- Containers usually reject encoded traversal (`..%2f`, `%2e%2e/`) with 400 before the request reaches the servlet — defense in depth.

---

## 7. Limitations (important)

1. **Reads only flushed data**: Log4Key writes asynchronously (`flushInterval` ≈ 1s by default), so the very latest lines may be briefly invisible; the starter does not force a flush (core exposes no such API).
2. **No recursion**: directory listing is one level deep; fuzzy matching only covers the current directory.
3. **Fixed 1000 lines per page**, not configurable; out-of-range pages return an empty array with the page number echoed.
4. **Enabled by default**: any servlet web application adding the starter gets `/log4key*`; there is no switch (no new configuration item).
5. No full-text line search or cross-file aggregation — this is a lightweight local log viewer.

---

## 8. Class reference

| Class (fully qualified) | Description | Key API |
| :--- | :--- | :--- |
| `com.log4key.spring.autoconfigure.Log4KeyAutoConfiguration` | Core auto-configuration | `log4KeyLifecycle()`, `logQueryService()` |
| `com.log4key.spring.autoconfigure.Log4KeyWebAutoConfiguration` | Query endpoint auto-configuration | `logsQueryServletRegistration(LogQueryService)` |
| `com.log4key.spring.autoconfigure.Log4KeyLifecycle` | Spring lifecycle bridge | `start()`, `stop()`, `isRunning()` |
| `com.log4key.spring.query.LogQueryService` | Query core (no servlet dependency) | `PAGE_SIZE`, `MAX_PAGE_BYTES`, `getRootDirectory()`, `browse(String, String)`, `readPage(String, Integer)` |
| `com.log4key.spring.query.DirectoryBrowse` | Directory browse result | `getDirectories()`, `getFiles()` |
| `com.log4key.spring.query.FilePage` | Paged result | `getPage()`, `getLines()` |
| `com.log4key.spring.query.PathResolver` | Path resolution and security validation | `getRoot()`, `resolveDirectory(String)`, `resolveFile(String)` |
| `com.log4key.spring.query.QueryException` | Query exception | `getCode()`; `ErrorCode`: `NOT_FOUND`, `INVALID_PATH`, `INVALID_ARGUMENT`, `NOT_DIRECTORY`, `NOT_FILE`, `OUTSIDE_ROOT`, `PAGE_TOO_LARGE`, `IO_ERROR` |
| `com.log4key.spring.query.JsonWriter` | Minimal JSON escaping | `escape(String)`, `quote(String)` |
| `com.log4key.spring.web.LogsQueryServlet` | Query servlet | `LogsQueryServlet(LogQueryService)`, `doGet(...)` |

Sources: `log4key-spring-boot-starter/src/main/java/com/log4key/spring/{autoconfigure,query,web}/`.

---

## 9. Viewing and publishing the API docs

Three options:

1. **Markdown (current, zero setup)** — rendered directly by GitHub / Gitea:
   - English: `docs/api/log4key-spring-boot-starter.md`
   - Chinese: `docs/api/log4key-spring-boot-starter.zh.md`
2. **Javadoc HTML on GitHub Pages**:
   ```bash
   ./gradlew :log4key-spring-boot-starter:javadoc
   # output: log4key-spring-boot-starter/build/docs/javadoc/index.html
   ```
   Commit the output to `docs/api/apidocs/`, then enable Settings → Pages → “Deploy from a branch: master /docs”
   to get a URL such as `https://<owner>.github.io/<repo>/api/apidocs/`; or add an `actions/deploy-pages` workflow.
3. **javadoc.io (after publishing to Maven Central)**: the CI publishes on tags, so
   `https://javadoc.io/doc/com.log4key/log4key-spring-boot-starter/<version>/` becomes available
   (requires publishing a `-javadoc.jar`).

> Recommendation: keep this Markdown file as the maintained contract; treat Javadoc HTML as a generated artifact produced per release to avoid drift.
