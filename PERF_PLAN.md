# BudgetBot — Performance Optimization Plan

## Current baseline (measured 2026-06-12)

| Metric | Value |
|---|---|
| Docker image (new Alpine) | 262 MB |
| Fat JAR size | 51 MB compressed |
| Cold build time | ~1 min 54 s |
| Warm rebuild (pom.xml changed) | ~36 s (with BuildKit cache mount) |
| Container startup | ~4–5 s (crashes on missing token; Hibernate init = ~3–4 s of that) |

## JAR size breakdown (uncompressed classes)

| Package | Size | Root cause |
|---|---|---|
| `org/hibernate` | 46 MB | `micronaut-data-hibernate-jpa` |
| `net/bytebuddy` | 16 MB | Hibernate lazy-load proxy generation |
| `io/micronaut` | 26 MB | framework |
| `org/glassfish` | 20 MB | Glassfish JAXB RI — pulled by Hibernate XML config |
| `io/netty` | 14 MB | Micronaut Netty HTTP server |
| `org/apache` | 9.2 MB | Apache HTTP client via `telegrambots` |
| `com/fasterxml` | 8.6 MB | Jackson |
| `org/telegram` | 4.3 MB | telegrambots |
| `ch/qos` | 3.2 MB | Logback |
| `javassist` | 2.5 MB | Hibernate bytecode manipulation |
| `com/home` | 880 KB | **app code** |

**Hibernate stack total (hibernate + bytebuddy + javassist + glassfish JAXB): ~85 MB uncompressed**
The app has only 2 entities: `BalanceHistoryEntity`, `BudgetConfigEntity`.

---

## Phase 1 — Micronaut AOT (no code changes)

**Expected: 10–25% startup improvement**

Change in `pom.xml`:
```xml
<micronaut.aot.enabled>false</micronaut.aot.enabled>
```
→
```xml
<micronaut.aot.enabled>true</micronaut.aot.enabled>
```

The `micronaut-maven-plugin` already handles AOT processing when this flag is true.
AOT pre-computes bean definitions, eliminates runtime service loader lookups,
and bakes `application.yaml` config into generated code (safe since secrets come from env vars).

Also fix the Hibernate dialect deprecation warning in `application.yaml` — remove:
```yaml
jpa:
  default:
    properties:
      hibernate:
        dialect: org.hibernate.dialect.H2Dialect
```
Hibernate 6 auto-detects the dialect from the JDBC URL.

---

## Phase 2 — AppCDS (Dockerfile only, no code changes)

**Expected: 15–25% startup improvement on top of Phase 1**

AppCDS bakes a shared class metadata archive into the image. Class loading is the
dominant startup cost for Micronaut — skipping `.class` parsing for already-known
classes saves hundreds of milliseconds.

Add to `Dockerfile` runtime stage (after COPY of the JAR):
```dockerfile
# Generate default JVM class-share archive (improves class-loading on startup)
RUN java -Xshare:dump 2>/dev/null || true
```

This generates the default JDK CDS archive for the Alpine JRE's class library.
Full AppCDS (including app classes) requires a dry-run training pass which needs
a valid bot token — skip for now; the JDK-level CDS still helps.

---

## Phase 3 — Hibernate → Micronaut Data JDBC (code refactor)

**Expected: ~35 MB JAR reduction (~70% of current 51 MB), startup ~1–2 s instead of ~4–5 s**

Hibernate is 46 MB + 16 MB ByteBuddy + 2.5 MB Javassist + 20 MB Glassfish JAXB = ~84 MB
uncompressed that goes away entirely.

### pom.xml changes

Remove:
```xml
<dependency>
    <groupId>io.micronaut.data</groupId>
    <artifactId>micronaut-data-hibernate-jpa</artifactId>
</dependency>
```
Remove from annotationProcessorPaths:
```xml
<path>
    <groupId>io.micronaut.data</groupId>
    <artifactId>micronaut-data-processor</artifactId>
    <version>${micronaut.data.version}</version>
</path>
```

Add:
```xml
<dependency>
    <groupId>io.micronaut.data</groupId>
    <artifactId>micronaut-data-jdbc</artifactId>
</dependency>
```
Add to annotationProcessorPaths:
```xml
<path>
    <groupId>io.micronaut.data</groupId>
    <artifactId>micronaut-data-processor</artifactId>
    <version>${micronaut.data.version}</version>
</path>
```
(same processor, different runtime dep — keep it)

Also remove `javax.xml.bind:jaxb-api` — it was added only because Hibernate pulled
`jackson-module-jaxb-annotations` which requires the JAXB API at runtime.
Verify after removal that tests pass.

### application.yaml changes

Remove the entire `jpa:` block.
Change `datasources.default` schema init:
```yaml
datasources:
  default:
    url: jdbc:h2:file:./data/database
    driver-class-name: org.h2.Driver
    username: admin
    password: admin
    schema-generate: CREATE_IF_NOT_EXISTS   # replaces hbm2ddl.auto: update
    dialect: H2
```

### Entity changes (2 files)

`BalanceHistoryEntity.java` — replace JPA imports:
```java
// Before
import jakarta.persistence.*;
// After
import io.micronaut.data.annotation.*;
import io.micronaut.data.model.naming.NamingStrategies;
```
`@Entity` → `@MappedEntity`
`@GeneratedValue(strategy = GenerationType.IDENTITY)` → `@GeneratedValue`
`@Column` annotations work as-is (Micronaut Data JDBC supports them).

`BudgetConfigEntity.java` — same changes.

### Repository changes (2 files)

`BalanceHistoryRepository.java`:
```java
// Before
import io.micronaut.data.repository.JpaRepository;
public interface BalanceHistoryRepository extends JpaRepository<BalanceHistoryEntity, Long>
// After
import io.micronaut.data.repository.CrudRepository;
public interface BalanceHistoryRepository extends CrudRepository<BalanceHistoryEntity, Long>
```
Any JPQL queries (`@Query` with `from BalanceHistoryEntity`) need rewriting to SQL:
```java
// Before: @Query("SELECT b FROM BalanceHistoryEntity b WHERE b.date >= :from")
// After:  @Query("SELECT * FROM balance_history WHERE date >= :from")
```

`ConfigRepository.java` — same pattern.

### What stays the same
- Hikari connection pool config
- H2 database file location (`./data/database`)
- All service/controller/bot code above the repository layer
- All tests (they use `@MicronautTest` which works with JDBC too)

---

## Verification checklist

- [ ] Phase 1: `mvn package` succeeds with AOT enabled; startup log shows AOT init messages
- [ ] Phase 2: Docker build includes the `java -Xshare:dump` line
- [ ] Phase 3: `mvn test` — all 23 tests pass
- [ ] Phase 3: JAR size drops below 20 MB
- [ ] Phase 3: Container startup under 2 seconds (check logs)
- [ ] Phase 3: `./data/database.mv.db` file created on first run (schema auto-created)
