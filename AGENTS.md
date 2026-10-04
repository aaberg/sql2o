# AGENTS.md

Java 17 library (no SQL generation; maps `ResultSet` -> POJO/record). Maven multi-module:

- `core` — artifactId `sql2o`, groupId `org.sql2o`. Everything lives under `org.sql2o.*`.
- `extensions/{postgres,oracle,oracle-joda-time,db2}` — groupId `org.sql2o.extensions`, each depends on core.

Upstream is `github.com/aaberg/sql2o` (this checkout is a fork); README/wiki links and the coding guide point there. `.editorconfig`: 4-space Java indent, LF, UTF-8, final newline. No lint/format/checkstyle plugin exists — don't invent one.

## Commands

- `mvn -pl core test` — core suite only, embedded H2 + HSQLDB, no external services. Fast; the default verification step.
- Single class/method: `mvn -pl core test -Dtest=RecordsTest` (add `-DfailIfNoSpecifiedTests=false` when running from the root reactor).
- Root `mvn test` / `mvn package` **fails without a database**: `extensions/postgres`'s `DataSourceTest` is JUnit 5 and needs a live server.
- `docker compose up -d` starts Postgres on host port **15432** (`testuser`/`testpassword`, db `postgres`) and Oracle XE 21c on **1521** (`system`/`testpassword`) — these match the JDBC URLs hardcoded in the extension tests.
- Release is CI-driven only: a GitHub *release* event makes the pipeline run `mvn versions:set` + `mvn -P release deploy -DskipTests` and push to Maven Central. Don't bump versions by hand and don't run the `release` profile locally.
- Compiler source/target `17` is duplicated in the root pom and `core/pom.xml` — change both.
- Maven builds drop Eclipse `.project`/`.classpath`/`.settings/` files that are **not** in `.gitignore`. Run `git clean -fd` before committing so they don't get staged.

## Testing traps

- **JUnit 4 tests silently do not run.** Surefire selects the JUnit Platform provider (junit-jupiter is on the test classpath) and `junit-vintage-engine` is not declared, so all 16 JUnit 4 classes (`QueryTest`, `ConnectionTest`, `Sql2oTest`, `IssuesTest`, `PostgresTest`, `OracleTest`, ...) are skipped with no warning: `mvn -pl core test` reports **47** tests; with the vintage engine added it reports **207**. `mvn -pl core test -Dtest=QueryTest` prints `Tests run: 0` and still succeeds.
  - Write new tests with **JUnit 5** (`org.junit.jupiter`). Don't treat "ran an existing test and it passed" as verification if that test is JUnit 4 — it executed nothing.
  - The fix, if legacy tests must run, is a test-scoped `org.junit.jupiter:junit-vintage-engine:5.11.4` in the root pom (verified: makes the JUnit 4 classes execute and pass).
- Two parameterized styles coexist: JUnit 4 `@RunWith(Parameterized.class)` over `BaseMemDbTest` / `PostgresTestSupport`, and JUnit 5 `@ParameterizedTest` + `@ArgumentsSource` over `TestDatabasesArgumentSourceProvider` / `H2ArgumentsSourceProvider`. Prefer the JUnit 5 one.
- Legacy tests use `org.zapodot:embedded-db-junit`'s `EmbeddedDatabaseRule`; newer tests just call `new Sql2o(url, user, pass)` against the in-memory URLs. Prefer the latter.
- Extension tests hardcode external DB URLs and, for Oracle, expect `extensions/oracle/src/test/resources/setup/test.sql` to have been applied. Only run them with docker-compose up.

## Architecture / extension points

- Flow: `Sql2o` -> `Connection` (`AutoCloseable`) -> `Query` -> converters + `reflection2` mapping (`PojoIntrospector` -> `PojoBuilder` / `RecordBuilder`). `Sql2o` itself is **not** `Closeable`, so the try-with-resources snippet in `README.md` does not compile — don't copy it.
- `Quirks` (`core/.../quirks/`) is the per-driver hook: `setParameter` overloads, `getRSVal`, column naming, `returnGeneratedKeysByDefault`, and the named-parameter SQL parser (`quirks/parameterparsing`). `NoQuirks` is the default; Oracle/Postgres/DB2/H2 implementations live in `extensions/*` and `core/src/test`.
- Driver behavior and extra converters are discovered with `ServiceLoader`. Adding a quirks or converter provider requires a `META-INF/services/org.sql2o.quirks.QuirksProvider` (or `...converters.ConvertersProvider`) entry — see `extensions/*/src/main/resources/META-INF/services/` and `core/src/test/resources/META-INF/services/org.sql2o.quirks.QuirksProvider`. Without it the code is dead.
- Column-to-property matching runs through `NamingConvention.deriveName` (case-insensitive by default, optional snake_case -> camelCase) and honors `javax.persistence.@Column` (that dependency is `provided`, so it must stay optional).
- `PojoIntrospector` / `PojoMetadata` skip static and private fields and cache introspection results in static `AbstractCache`s — relevant when writing tests that mutate classes or reuse names.
- Each module ships `src/main/resources/META-INF/MANIFEST.MF` wired in via `maven-jar-plugin` (OSGi metadata); keep it when touching packaging.
