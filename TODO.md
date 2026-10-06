# TODO

Legend: **[run]** = reproduced by running code, **[read]** = read from the code only (no runtime proof, needs a
live DB or a scratch test to confirm), **[design]** = contract or design decision, not a defect.

## P0 — confirmed resource and state bugs

All four reproduced by running sql2o against H2; re-verify with a scratch test before fixing, and keep the
scratch test as the regression test.

- [run] **A reused `Query` fails after `executeScalar()`.** `executeScalar()` closes the cached
      `PreparedStatement` via try-with-resources (`Query.java:700-701`), but neither it nor `close()`
      (`Query.java:398-406`) resets `preparedStatement` or sets a closed flag, so `buildPreparedStatement()`
      hands the closed statement to the next execution (`Query.java:420-436`). Observed:
      `Sql2oException: ... The object is already closed`.
- [run] **`executeScalar()` never calls `removeStatement()`**, so closed statements accumulate in
      `Connection.statements` for the lifetime of the connection and are closed again by
      `Connection.close()`. The only `removeStatement` call is in `Query.close()` (`Query.java:400`).
- [run] **A second execution with array parameters is corrupted.** `parsedQuery` and `paramNameToIdxMap`
      are mutated in place (`Query.java:420`, `ArrayParameters.java:43-56`). Observed for
      `select * from t where a = :ids and b = :other` with `ids` of size 2:
      - after the 1st expansion: SQL `a = ?,? and b = ?`, map `{ids=[1], other=[3]}` — `ids` should be
        `[1,2]`, and `other` is already shifted;
      - after the 2nd expansion: SQL `a = ?,?,? and b = ?`, map `{ids=[1], other=[4]}`.
      So the second run binds one value for `ids`, leaves a placeholder unbound, and addresses a
      non-existent index for `other`. Work on copies of both the SQL and the index map.
- [read] **A failing fetch leaks the connection and skips the rollback.** `buildPreparedStatement()`
      converts `SQLException` into `Sql2oException` (a `RuntimeException`), which the
      `catch (SQLException)` in the `ResultSetIterableBase` constructor does not catch, so the exception
      escapes the constructor and neither `close()` nor `onException()` runs (`Query.java:464-474`).
      Worst for the deprecated auto-close `Sql2o.createQuery(...)` path (`Sql2o.java:167-189`).

## P1 — parser and mapping

- [run] **`''` inside a literal breaks parameter parsing.** `QuoteParser` ends a literal at the first
      repeated quote (`QuoteParser.java:14-25`). For `select 'a''b :x', :y from t` the parser returns
      `select 'a''b :x', ? from t` with map `{y=[1]}` — `:x` is swallowed, `:y` gets index 1 instead of 2,
      and `addParameter("x", ...)` then fails with "No parameter with that name is declared in the sql".
- [run] **Array expansion ignores quoting.** `updateQueryWithArrayParameters` counts every `?` character
      (`ArrayParameters.java:113`). For `select * from t where note = 'why?' and id in (:ids)` it produced
      `note = 'why?,?,?' and id in (?)` — the placeholders are injected *inside the string literal* while
      the real placeholder is left unbound. The parser itself is quote aware, the array expander is not.
- [run] **SQL starting with a named parameter throws.** `ParameterParser.canParse` evaluates
      `charAt(idx - 1)` with `idx == 0` (`ParameterParser.java:21`); `parseSql(":foo", map)` throws
      `StringIndexOutOfBoundsException: Index -1 out of bounds for length 4`.
- [read] **Superclass members overwrite subclass members.** `initializeForClassRecursive` walks subclass
      first, then the superclass (`PojoMetadata.java:96-100`), and `computeIfAbsent` only guards creation
      while `withSetter`/`withGetter`/`withField` always assign (`PojoPropertyBuilder.java:22-36`). A
      covariant setter in a subclass is replaced by the superclass one; a shadowed field always writes to
      the superclass field. `PojoIntrospector` does the opposite (`:53`, `:86`), so the two introspectors
      in the same package disagree.
- [read] **`@Column` on methods is dead.** The method branch probes `pojoPropertyBuilders.containsKey(methodName)`
      with `"setId"` while properties are keyed by the derived name `"id"` (`PojoMetadata.java:79-81`), so
      the annotation is never read. The field branch (`:91-93`) does work, but `withAnnotatedName(null)`
      overwrites unconditionally, so a second member with the same property name erases the name.
      Bridge and synthetic methods are not filtered either.
- [read] **`RecordBuilder` picks the canonical constructor by index** (`getDeclaredConstructors()[0]`,
      `RecordBuilder.java:21`), never calls `setAccessible`, ignores `columnMappings` and
      `throwOnMappingError` (`ObjectBuildableFactory.java:15`), and throws `IllegalArgumentException`
      where `PojoBuilder` throws `Sql2oException`.
- [run] **`PojoBuilder` rebuilt nested metadata on every row.** For a dotted column name it created
      `new PojoMetadata<>(subObj.getClass(), settings)` per row, bypassing the cache. It now asks
      `ObjectBuildableFactory` for it like any other class, so the nested metadata is cached and shared
      between instances that use the same naming convention. That change also removed the raw type the
      nested builder used to fall back to, which had silently erased its generic contract. The NPE that used
      to happen for an unmapped prefix in the same branch is fixed and covered by `PojoNestedColumnTest`.

## P2 — converters

- [read] **`NumberConverter` and `ByteArrayConverter` throw unchecked exceptions**, so every
      `catch (ConverterException)` in the mapping path is bypassed and the column context is lost
      (`NumberConverter.java:31`, `ByteArrayConverter.java:52`).
- [read] **`BooleanConverter` maps the string `"1"` to `false`** while the `Number` branch handles `1`
      correctly (`BooleanConverter.java:32-33`).
- [read] **`AbstractDateConverter.toDatabaseParam` up-casts `java.sql.Date` to `Timestamp`**, so binding
      a date sends a timestamp (`AbstractDateConverter.java:40-45`).
- [read] **`NoQuirks.converterOf` always falls back to `DefaultConverter`** (`NoQuirks.java:46-51`), so
      `Convert.throwIfNull` and the "No converter registered for class: X" message are unreachable; a
      missing converter surfaces as a raw `ClassCastException` or `IllegalArgumentException` from `Field.set`
      with no mention of the column.

## P2 — dialects and extensions

- [read] **The db2 extension ships no `META-INF/services/org.sql2o.quirks.QuirksProvider`**, so
      `Db2Quirks`/`Db2QuirksProvider` are dead code and `QuirksDetector` always falls back to `NoQuirks`.
- [read] **`PostgresQuirks` does not override the parsing strategy**, so `$$...$$` bodies and the
      `?`/`?|`/`?&` JSON operators are corrupted.
- [read] **`JSONConverter.toDatabaseParam` returns a bare `String`**, so writes to a `json`/`jsonb` column
      fail (`extensions/postgres/.../JSONConverter.java:41-46`).
- [read] **Extension converters register into the global `Convert` registry** through `ServiceLoader`
      (`Convert.java:106-112`), so adding `sql2o-oracle` changes `Date` handling for every database, and
      the winner depends on classpath order.
- [read] **`Quirks.getRSVal` is not used on the POJO/record path** — `DefaultResultSetHandlerFactory`
      reads `resultSet.getObject(i)` directly (`:26`). Verified with `javap` against ojdbc11 23.3 and
      then confirmed against Oracle XE 21c: `oracle.sql.TIMESTAMPTZ` and `TIMESTAMPLTZ` extend `Datum`
      directly and are **not** subclasses of `oracle.sql.TIMESTAMP`, which is the only type
      `OracleDateConverter` handles (`OracleDateConverter.java:16`), so mapping such a column to a
      `java.util.Date` property fails with
      `ConverterException: Cannot convert type class oracle.sql.TIMESTAMPTZ to java.util.Date`.
      Measured per column type, reading a literal of each kind with the shipped quirks:

      | sql2o source type          | `ResultSet.getObject(i)` | `OracleQuirks.getRSVal` | `OracleDateConverter` |
      |----------------------------|--------------------------|-------------------------|------------------------|
      | `timestamp`                | `oracle.sql.TIMESTAMP`   | `java.sql.Timestamp`   | ok                     |
      | `timestamp with time zone` | `oracle.sql.TIMESTAMPTZ` | `java.sql.Timestamp`   | **throws**             |
      | `timestamp with local time zone` | `oracle.sql.TIMESTAMPLTZ` | `java.sql.Timestamp` | **throws**        |
      | `date`                     | `java.sql.Timestamp`     | `java.sql.Timestamp`   | ok                     |

      Two things worth noting. The quirks hook *does* cope with the zone types, because it compares the
      `oracle.sql.TIMESTAMP` name prefix, so a mapper that went through `getRSVal` would have been fine —
      the breakage is specific to the path that reads `getObject` directly. And `date` columns never
      produce an `oracle.sql.DATE` here, so that part of the class hierarchy argument is moot in
      practice; only the two zone types are actually reachable. Fixing means matching on `Datum` in
      `OracleDateConverter`, at which point the joda converters already do the right thing.
- [read] **`OracleLocalTimeConverter` builds a Joda `LocalTime` from a `Timestamp`** in the JVM default
      zone (`OracleLocalTimeConverter.java:22`), so the wall-clock value depends on the JVM time zone.
- [read] **`gson` is a hard non-optional dependency** of the postgres extension and appears in a published
      signature (`extensions/postgres/pom.xml:26-30`).
- [read] **`QuirksDetector.forObject` reduces anything with `$` in the class name to its superclass**, so a
      JDK proxy or lambda becomes `java.lang.Object` and no provider matches; `getSuperclass()` can also
      return null (`QuirksDetector.java:34-36`). The `static final ServiceLoader providers` field is dead —
      a fresh loader is built on every call.

## P3 — misc

- [run] **`logger.warn("... {}", e)` has no matching overload** in the `Logger` interface, so
      `SysOutLogger` prints a literal `{}` and only SLF4J users see a formatted message
      (`Connection.java:143`, `:330`).
- [read] **`Connection.close()` starts with `isClosed()`** and releases nothing if it throws; because it is
      called from `finally` blocks, that also replaces the original exception (`Connection.java:276-282`).
- [read] **`createQuery` resurrects a closed connection** by calling `createConnection()` when
      `jdbcConnection.isClosed()` (`Connection.java:96-113`) — there is no guard for "this `Connection` was
      closed", so a joined (`NestedConnection`) handle silently acquires a new pooled connection.
- [read] **`NestedConnection` ignores the transaction isolation level** (`setTransactionIsolation` is a
      no-op, `NestedConnection.java:44-47`). Its `commit()` and `rollback()` behaviour is deliberate
      (commit is a no-op, rollback propagates to the parent with a warning), and `rollbackOnClose` does
      work because `setAutoCommit` is tracked locally — an earlier claim that all three flags are no-ops
      was wrong.
- [read] **`WrappedConnection.unwrap()` violates the `java.sql.Wrapper` contract** by delegating to the
      parent instead of returning `this` (`WrappedConnection.java:283-290`).

## P3 — cache locking (low severity, read before touching)

Reading a `HashMap` is fine as long as nothing writes concurrently, so neither of these is a race in
normal operation. They are contract violations with a narrow window.

- [read] **`Cache.get()` reads the map outside the lock** (`Cache.java:18`) and writes under the write
      lock (`Cache.java:31`). The lookup runs per mapped row
      (`DefaultResultSetHandlerFactory.java:21` -> `ObjectBuildableFactory.forClass`), but the write only
      happens on a cache miss, so the read-during-write window is limited to concurrent warm-up and the
      likely symptom is a redundant recompute. `AbstractCache` next to it does it correctly. One line
      fixes it: `ConcurrentHashMap`, or take the read lock.
- [read] **`Convert.registeredConverters` is read without the read lock** and the declared `rl` is never
      acquired anywhere (`Convert.java:28`, `:128`). It is written only in the static initializer, which
      is safe, or by `Convert.registerConverter()` under `wl`. The real defect is visibility: the field is
      not `volatile` and readers take no lock, so a converter registered at runtime may never become
      visible to other threads.

## P3 — thread safety contract (documentation, not a bug)

- [design] **`Sql2o`, `Connection` and `Query` are not thread safe and nothing says so.** `Sql2o` hands out
      the live `defaultColumnMappings` map (`Sql2o.java:123-125`), `Connection.statements` is a plain
      `HashSet` (`Connection.java:267`), and `Query` mutates its SQL and index map on every execution.
      Either document the contract or make the shared state safe; do not "fix" it by adding locks to
      `Query` without a stated policy.

## Low priority — cleanups

- [read] **`PojoBuilder` rebuilt nested metadata on every row** — fixed: a dotted column name now asks
      `ObjectBuildableFactory` for the nested metadata like any other class, so it comes from the cache.
- [read] **24 `rawtypes` warnings remain**, deliberately not enabled in the compiler config. They are mostly
      published generics: `NoQuirks(Map<Class, Converter>)` and the converters registry, `Convert.getConverter`
      and `registerConverter`, `EnumConverterFactory`/`DefaultEnumConverterFactory`, plus internals in `Query`,
      `Connection`, `LocalLoggerFactory` and `ResultSetIteratorBase`. Fixing them means changing published
      signatures such as `Map<Class, Converter>` to `Map<Class<?>, Converter<?>>`, which breaks every
      third party converter and quirks implementation, so it needs its own decision.
- `MANIFEST.MF` files only carry `Library-Name`/`Library-Description`; the OSGi metadata is inert and the
  split packages (`org.sql2o.quirks`, `org.sql2o.converters`) would break under OSGi.
- `README.md` quick start does not compile: `Sql2o` is not `Closeable`. Already noted in `AGENTS.md`.
- `IssuesTest` keeps its own `TestDatabase` holder because it needs extra HSQLDB setup; align it with the
  shared `org.sql2o.TestDatabase` if the HSQLDB MSS syntax switch ever becomes unnecessary.

## Test tooling

- The extensions are all on JUnit 5 and `junit:junit` plus `junit-vintage-engine` are gone from the root pom.
  `junit.version` went with them; only `junit-jupiter.version` is left.

`PostgresTest` and `UUIDTest` got a `PostgresTestDatabase` in place of the constructor injection, the oracle tests
register their driver in `@BeforeAll`. All of them have now been run against real databases: `docker compose up -d`
brings up postgres on 15432 and Oracle XE 21c on 1521. Note the oracle tests need a url in the legacy SID form
(`@host:port:XE`), which is why they register the driver by hand.

## Fixed

Kept here as a reminder of what was already dealt with, and of what the tests cover.

- A `Query` reused after `executeScalar()` failed with "The object is already closed".
- A dotted column name whose prefix has no matching property threw a `NullPointerException` instead of a
  mapping error; both branches of `PojoBuilder` now share `handleMissingProperty`, so `throwOnMappingError`
  is honoured either way. Covered by `PojoNestedColumnTest`.
- `PojoMetadata` was cached under the class only, while the metadata derives property names from the naming
  convention and `PojoProperty` resolved converters through the quirks captured at build time. A second
  `Sql2o` instance in the same JVM therefore inherited the property names and the converters of the first
  one. The cache key is now the class plus the `NamingConvention`, converters are passed in from the
  builder, and the two argument `PojoProperty.SetProperty` is deprecated. Covered by
  `PojoMetadataCacheSettingsTest`. Note that `throwOnMappingError` was never affected: the builder reads it
  from its own settings.
- Thirteen unused imports left over from earlier changes were removed, five in `core/src/main` and eight in
  the extension tests. Kept as a commit of its own because the coding guidelines ask for drive-by cleanups to
  be kept out of unrelated changes. Nothing in the build enforces this, so it can happen again.