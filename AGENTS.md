# AGENTS.md

Java 17 library (no SQL generation; maps `ResultSet` -> POJO/record). Maven multi-module:

- `core` — artifactId `sql2o`, groupId `org.sql2o`. Everything lives under `org.sql2o.*`.
- `extensions/{postgres,oracle,oracle-joda-time,db2}` — groupId `org.sql2o.extensions`, each depends on core.

Upstream is `github.com/aaberg/sql2o` (this checkout is a fork); README/wiki links point there. `.editorconfig`: 4-space Java indent, LF, UTF-8, final newline — keep it in sync with the wiki coding guidelines below. No lint/format/checkstyle plugin exists — don't invent one.

The current version line (1.9.0-SNAPSHOT) is the Java 17+ line, because it added record support; 1.8.x targets Java 11 and 1.6.x Java 8. Lowering `maven.compiler.source/target` is therefore an API break for users, not a local tweak.

## Coding style

Source of truth is the wiki: https://github.com/aaberg/sql2o/wiki/Coding-guidelines — spaces never tabs, 4-space indent, no trailing whitespace, US English names, no leading underscore on private fields (`myVariable`, not `_myVariable`), no unused imports. Existing files do contain trailing whitespace and (rarely) tabs; don't propagate that into code you touch, but don't reformat files you aren't changing either.

Nothing in the build enforces any of this (no checkstyle/lint plugin), so it is on you to check:

- `git diff --check` before committing — reports trailing whitespace.
- Review `git diff` for unrelated hunks: whitespace-only or drive-by changes get a PR rejected, and drive-by fixes belong in their own PR.
- The compiler runs with `-Xlint:unchecked`, and the build is expected to produce **zero** unchecked warnings. A new one either gets rewritten, or gets a local `@SuppressWarnings("unchecked")` with a comment proving why that specific spot is safe. `rawtypes` is deliberately not enabled: it would add a lot of noise and mostly covers published API.
- New behaviour needs tests; `mvn -pl core test` must stay at 431.

## Commands

- `mvn -pl core test` — core suite only, embedded H2 + HSQLDB, no external services. Fast; the default verification step.
- Single class/method: `mvn -pl core test -Dtest=RecordsTest` (add `-DfailIfNoSpecifiedTests=false` when running from the root reactor).
- Root `mvn test` / `mvn package` **fails without a database**: `extensions/postgres`'s `DataSourceTest` is JUnit 5 and needs a live server.
- `docker compose up -d` starts Postgres on host port **15432** (`testuser`/`testpassword`, db `postgres`), Oracle XE 21c on **1521** (`system`/`testpassword`) and Db2 Community Edition on **50000** (`db2inst1`/`testpassword`, db `testdb`) — these match the JDBC URLs hardcoded in the extension tests.
- Release is CI-driven only: a GitHub *release* event makes the pipeline run `mvn versions:set` + `mvn -P release deploy -DskipTests` and push to Maven Central. Don't bump versions by hand and don't run the `release` profile locally.
- Compiler source/target `17` is duplicated in the root pom and `core/pom.xml` — change both.
- `maven-surefire-plugin` is pinned in the root pom; without it the version comes from the Maven super POM and the selected test provider can change with the Maven version.
- JaCoCo is wired into the root pom: `mvn -pl core test` writes `core/target/site/jacoco/index.html` (plus `jacoco.csv`) as part of the `test` phase, so coverage needs no separate command. Reports land under `target/`, which is git-ignored. `mvn -pl extensions/postgres -am test` measures the postgres module the same way, once `docker compose up -d postgres-db` is running; the oracle and db2 modules need their own service up.
- Maven builds drop Eclipse `.project`/`.classpath`/`.settings/` files that are **not** in `.gitignore`. Run `git clean -fd` before committing so they don't get staged.

## Testing traps

- **Core is on JUnit 5, extensions are still on JUnit 4.** `junit:junit` + `junit-vintage-engine` exist in the root pom solely for `extensions/{postgres,oracle,oracle-joda-time}`; drop both once those are migrated. Hamcrest comes from the explicit `org.hamcrest:hamcrest` dependency (junit's transitive `hamcrest-core` is excluded) — use `org.hamcrest.MatcherAssert.assertThat`, since `org.junit.Assert.assertThat` does not exist in JUnit 5.
- `mvn -pl core test` must report **431** tests. That number is the regression guard for the test sources: if it changes, a test was lost, renamed or silently not discovered.
- In-memory H2/HSQLDB databases use `DB_CLOSE_DELAY=-1`, so they **survive between test methods in the same JVM** — a `create table` in a second test fails with "table already exists". Drop the table first (see `QueryArrayTest`) or use a distinct database name per test.
- Per-database parameterization: use `TestDatabase` + the composed `@DatabaseTest` annotation (both in `core/src/test/java/org/sql2o/`). A test class needs `static Stream<TestDatabase> databases()` and a `@DatabaseTest` method taking a single `TestDatabase`. `IssuesTest` keeps its own local holder because it needs extra HSQLDB setup.
- In-memory databases are also shared with other test classes (`jdbc:h2:mem:test` is used by several), so table names must be unique across the whole suite.
- Extension tests hardcode external DB URLs and, for Oracle, expect `extensions/oracle/src/test/resources/setup/test.sql` to have been applied. Only run them with docker-compose up.
- `extensions/db2` talks to `ibmcom/db2:11.5.8.0` (Community Edition, port 50000, user `db2inst1`). It needs `--privileged` and `shm_size`, and it takes several minutes to initialise the first time, so readiness is the `Setup has completed` line in the logs rather than the container being up. `icr.io/db2_ce/db2` is the same image but the registry wants an ibm cloud entitlement; the docker hub path is the anonymous one. Three db2 notes: a null cannot be selected (`SQLCODE=-4472`), so it has to be read from a column; an alias is folded to upper case, so write `as theDay` rather than `as the_day` unless name derivation is on; and there is no `drop table if exists`, so the drop in `Db2DateReadingApiTest` swallows the error on purpose.

## Architecture / extension points

- Flow: `Sql2o` -> `Connection` (`AutoCloseable`) -> `Query` -> converters + `reflection2` mapping (`PojoIntrospector` -> `PojoBuilder` / `RecordBuilder`). `Sql2o` itself is **not** `Closeable`, so the try-with-resources snippet in `README.md` does not compile — the correct shape is `try (Connection con = sql2o.open())` with `sql2o` created outside.
- `Quirks` (`core/.../quirks/`) is the per-driver hook: `setParameter` overloads, `getRSVal`, column naming, `returnGeneratedKeysByDefault`, and the named-parameter SQL parser (`quirks/parameterparsing`). `NoQuirks` is the default; Oracle/Postgres/DB2/H2 implementations live in `extensions/*` and `core/src/test`.
- Driver behavior and extra converters are discovered with `ServiceLoader`. Adding a quirks or converter provider requires a `META-INF/services/org.sql2o.quirks.QuirksProvider` (or `...converters.ConvertersProvider`) entry — see `extensions/*/src/main/resources/META-INF/services/` and `core/src/test/resources/META-INF/services/org.sql2o.quirks.QuirksProvider`. Without it the code is dead.
- Column-to-property matching runs through `NamingConvention.deriveName` (case-insensitive by default, optional snake_case -> camelCase) and honors `javax.persistence.@Column` (that dependency is `provided`, so it must stay optional).
- `PojoIntrospector` / `PojoMetadata` skip static and private fields and cache introspection results in static `AbstractCache`s — relevant when writing tests that mutate classes or reuse names.
- Each module ships `src/main/resources/META-INF/MANIFEST.MF` wired in via `maven-jar-plugin` (OSGi metadata); keep it when touching packaging.
