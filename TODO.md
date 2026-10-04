# TODO

## P0 — resource handling and query reuse (`Query`)

Found by the code review. Each item is a bug, not a cleanup.

- [ ] `Query.close()` does not reset `preparedStatement` and has no closed flag, so a reused `Query`
      works on a closed statement (`Query.java:398-406`, `Query.java:420-436`).
- [ ] `executeScalar()` closes the shared cached statement without calling `removeStatement()`, so dead
      statements pile up in `Connection.statements` until `Connection.close()` (`Query.java:700-701`
      vs `Query.java:400`).
- [ ] Array parameters corrupt the `Query` permanently: `parsedQuery` and `paramNameToIdxMap` are mutated
      in place, so a second `executeUpdate()` re-expands the already expanded SQL and binds the wrong
      indices (`Query.java:420`, `ArrayParameters.java:43-56`). Work on copies instead.
- [ ] `ResultSetIterableBase`'s constructor throws, so `close()` and `onException()` never run: the
      connection and the statement leak, and an open transaction is never rolled back
      (`Query.java:464-474`). Matters most for the deprecated `Sql2o.createQuery(...)` auto-close path.

Verification for all four: `mvn -pl core test` (207 tests) plus a new regression test per item.

## P1 — POJO metadata cache key

- [ ] `ObjectBuildableFactory` caches `PojoMetadata` under the class only, so the first `Sql2o` instance
      in the JVM fixes naming convention, case sensitivity and `throwOnMappingError` for all others
      (`ObjectBuildableFactory.java:10,19`). Include the relevant `Settings` in the cache key.

## P3 — cache locking (low severity, read this before touching it)

Reading a `HashMap` is fine as long as nothing writes concurrently, so neither of these is a race in the
normal case. They are contract violations with a narrow window, not observed bugs.

- [ ] `Cache.get()` reads the map outside the lock (`Cache.java:18`) and writes under the write lock
      (`Cache.java:31`). The lookup runs per mapped row
      (`DefaultResultSetHandlerFactory.java:21` -> `ObjectBuildableFactory.forClass`), but the write only
      happens on a cache miss, so the read-during-write window is limited to concurrent warm-up. The
      likely symptom is a redundant recompute, not corruption. `AbstractCache` next to it does it
      correctly. One line fixes it: use a `ConcurrentHashMap` or take the read lock.
- [ ] `Convert.registeredConverters` is read without the read lock and the declared `rl` is never
      acquired anywhere (`Convert.java:28`, `Convert.java:128`). It is written only in the static
      initializer, which is safe, or by the public `Convert.registerConverter()` under `wl`. The real
      defect is visibility, not integrity: the field is not `volatile` and readers take no lock, so a
      converter registered at runtime may never become visible to other threads.

## P1 — POJO mapping

- [ ] `RecordBuilder` picks the canonical constructor by index (`getDeclaredConstructors()[0]`), never
      calls `setAccessible`, ignores `columnMappings` and `throwOnMappingError`, and throws
      `IllegalArgumentException` where `PojoBuilder` throws `Sql2oException`
      (`RecordBuilder.java:21`, `ObjectBuildableFactory.java:15`).
- [ ] `PojoMetadata`: superclass members overwrite subclass members (`computeIfAbsent` guards creation
      only, then `withSetter` always assigns), the `@Column` lookup probes the method name (`"setId"`)
      instead of the property name so the annotation is dead, `withAnnotatedName(null)` wipes names,
      and bridge/synthetic methods are not filtered (`PojoMetadata.java:70-92`).
- [ ] `PojoBuilder` builds fresh nested `PojoMetadata` per column per row, bypassing the cache, and
      invokes the sub-property setter twice (`PojoBuilder.java:37-60`).

## P2 — converters

- [ ] `NumberConverter` and `ByteArrayConverter` throw unchecked exceptions, so every
      `catch (ConverterException)` in the mapping path is bypassed and the column context is lost
      (`NumberConverter.java:31`, `ByteArrayConverter.java:52`).
- [ ] `BooleanConverter` maps the string `"1"` to `false` while the `Number` branch handles `1` correctly
      (`BooleanConverter.java:32-33`).
- [ ] `AbstractDateConverter.toDatabaseParam` up-casts `java.sql.Date` to `Timestamp`, so binding a date
      sends a timestamp (`AbstractDateConverter.java:40-45`).

## P2 — dialects and extensions

- [ ] `QuoteParser` terminates a literal at the first repeated quote, so SQL-standard `''` escaping
      breaks the rest of the statement (`QuoteParser.java:14-25`).
- [ ] `ArrayParameters.updateQueryWithArrayParameters` counts every `?` in the SQL, including those
      inside literals (`ArrayParameters.java:113`).
- [ ] `ParameterParser.canParse` evaluates `charAt(idx - 1)` when `idx == 0`, so SQL starting with
      `:name` throws `StringIndexOutOfBoundsException` (`ParameterParser.java:21`).
- [ ] The db2 extension ships no `META-INF/services/org.sql2o.quirks.QuirksProvider`, so `Db2Quirks` is
      dead code.
- [ ] `PostgresQuirks` does not override the parsing strategy: `$$...$$` bodies and the `?`/`?|`/`?&`
      JSON operators are corrupted.
- [ ] `JSONConverter.toDatabaseParam` returns a bare `String`, so writes to a `json`/`jsonb` column fail
      (`extensions/postgres/.../JSONConverter.java:41-46`).
- [ ] `gson` is a hard non-optional dependency of the postgres extension and appears in a published
      signature (`extensions/postgres/pom.xml:26-30`).
- [ ] Extension converters register into the global `Convert` registry, so adding `sql2o-oracle` changes
      `Date` handling for every database; `Quirks.getRSVal` is not used on the POJO mapping path at all.

## P3 — misc

- [ ] `logger.warn("... {}", e)` has no matching overload in the `Logger` interface, so `SysOutLogger`
      prints a literal `{}` (`Connection.java:143`, `Connection.java:330`).
- [ ] `Connection.close()` starts with `isClosed()` and releases nothing if it throws; `createQuery`
      resurrects closed connections; `NestedConnection` silently turns `rollbackOnClose`,
      `rollbackOnException` and the isolation level into no-ops.
- [ ] `WrappedConnection.unwrap()` violates the `java.sql.Wrapper` contract by delegating instead of
      returning `this`.

## Low priority — cleanups

- [ ] Remove the unused imports left in `core/src/main`: `java.util.Map` in `NamingConvention`,
      `java.time.OffsetDateTime` and `java.time.OffsetTime` in `AbstractDateConverter`,
      `org.sql2o.NamingConvention` and `org.sql2o.quirks.Quirks` in `RecordBuilder`.
- [ ] Check the same in `extensions/*` (not covered by the scan that found the ones above).
- [ ] `MANIFEST.MF` files only carry `Library-Name`/`Library-Description`; the OSGi metadata is inert and
      the split packages (`org.sql2o.quirks`, `org.sql2o.converters`) would break under OSGi.
- [ ] `README.md` quick start does not compile: `Sql2o` is not `Closeable`.
- [ ] `IssuesTest` keeps its own `TestDatabase` holder because it needs extra HSQLDB setup; align it with
      the shared `org.sql2o.TestDatabase` if the HSQLDB MSS syntax switch ever becomes unnecessary.

## Test tooling

- [ ] Migrate the extensions to JUnit 5: `PostgresTest`, `UUIDTest`, `PostgresTestSupport`,
      `OracleTest` and `OracleConverterTest` in both oracle modules. Needs `docker compose up -d`.
- [ ] After that, remove `junit:junit` and `junit-vintage-engine` from the root pom.

Both of these came out of the JUnit migration; `core` is already fully on JUnit 5.