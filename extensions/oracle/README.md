# sql2o-oracle

Oracle support for [sql2o](../../README.md): the quirks, the converters and the service registration that let sql2o
talk to Oracle through `ojdbc`.

```xml
<dependency>
    <groupId>org.sql2o.extensions</groupId>
    <artifactId>sql2o-oracle</artifactId>
    <version>1.9.0-SNAPSHOT</version>
</dependency>
```

The quirks are found by `ServiceLoader`, so nothing has to be configured:

```java
Sql2o sql2o = new Sql2o("jdbc:oracle:thin:@localhost:1521:XE", "system", "testpassword");
```

Most of what follows is not guesswork. `OracleTypedParameterTest` binds every type a caller can name in
`Query.addParameter(String, Class, Object)` against a real Oracle XE and reads it back, and the behaviours described
here are what that test observes. Run it with `docker compose up -d oracle-xe-21c` followed by
`mvn -pl extensions/oracle -am test`.

## UUID

Oracle has no uuid type. A uuid is kept as the **sixteen bytes** of a `RAW(16)` column, which is the two `long`s a uuid
is made of laid end to end.

| | |
| --- | --- |
| Stored as | `RAW(16)` |
| Written by | `OracleQuirks.setParameter(PreparedStatement, int, UUID)`, via `setBytes` |
| Written as | `OracleUUIDConverter.toDatabaseParam` — two `long`s into a 16 byte buffer |
| Read by | `OracleUUIDConverter.convert` — the bytes read back as two `long`s |

The converter is registered for `UUID.class` in the quirks, and `NoQuirks.converterOf` looks in the quirks before it
looks in the global `Convert` registry, so it covers every read path there is: `executeScalar(UUID.class)`, a POJO field
of type `UUID`, and a record component of type `UUID`.

### How to bind a uuid

Either way works, because `OracleQuirks.setParameter(PreparedStatement, int, Object)` routes a uuid to the overload
that has one for it before it goes anywhere near the driver:

```java
query.addParameter("id", UUID.class, id);   // named
query.addParameter("id", id);               // or simply handed over
```

That routing is there for a reason. The typed overload in core (`Query.addParameter(String, Class, T)`) has branches for
`InputStream`, `Integer`, `Long`, `String`, `Timestamp`, `Time`, arrays and collections, and **no branch for `UUID`**,
so a uuid would fall through to `setParameter(Object)` with the uuid still in it. The driver answers for it with the
thirty-two character hex form and then refuses its own answer:

```
org.sql2o.Sql2oException: Invalid UUID string: 6BA7B8109DAD11D180B400C04FD430C8
```

The untyped `addParameter(String, Object)` hands the value's own class straight to the typed overload, so it lands in
the same place and needs the same routing. `Boolean`, `Short`, `Byte`, `Float`, `Double`, `BigDecimal`,
`java.sql.Date` and every `java.time` type fall through that gap as well; the ones that reach a driver anyway are on
their own below.

### A caveat about your own converter map

`new OracleQuirks(myConverters)` uses `myConverters` and nothing else, so a uuid stops working unless the map has a
`UUID.class` entry of your own. The same is true of `Db2Quirks`. Register `new OracleUUIDConverter()` in the map if you
pass one.

## Column types worth knowing

| what you write | column to use | why |
| --- | --- | --- |
| `Integer`, `Long`, `Short`, `Byte`, `Boolean` | `NUMBER(…)` | Oracle has no integral and no boolean type. `NUMBER(1)` for a boolean, `NUMBER(10)` for an int |
| `BigDecimal` | `NUMBER` | has no scale of its own, so `1234.56` comes back as the two digits it went in with, where a `DECIMAL(12,4)` on another database pads it to four |
| `LocalDate`, `java.sql.Date` | `DATE` | carries a time part as well; a date read out of it is a `Timestamp` |
| `Timestamp`, `LocalDateTime`, `Instant` | `TIMESTAMP` | `TIMESTAMP WITH TIME ZONE` is not covered here |
| `LocalTime`, `java.sql.Time` | **no column** | Oracle has no time type at all, see below |
| `UUID` | `RAW(16)` | see above |
| `byte[]`, `InputStream` | `BLOB` | both ways of binding are covered by the test |

### There is no TIME type

Oracle has no type for a time of day, and a `DATE` carries a time part, so there is nowhere to put a value that is a
time and nothing else — and reading one out of a `DATE` would hand it a date as well. `OracleTypedParameterTest` leaves
`java.sql.Time`, `LocalTime` and `OffsetTime` out of its matrix for this reason, with the reason written down in the
test rather than left to a reader. If you need a time of day, store it in a `NUMBER` as seconds or minutes of the day,
or in a `VARCHAR2` in a format you control.

### Instant is not accepted

`addParameter("v", Instant.class, instant)` fails with

```
org.sql2o.Sql2oException: Error adding parameter 'v' - ORA-17004: Invalid column type
```

and so does the untyped `addParameter("v", instant)`, since it reaches the same place. The value arrives at the driver
as a `java.time.Instant`, which it cannot place in a `TIMESTAMP` column. This is not an Oracle quirk but a gap in sql2o:
nothing converts an `Instant` to a `Timestamp` on the way in, and the same is true on Postgres and DB2. Pass a
`java.sql.Timestamp`, or convert it yourself.

## Other Oracle specifics

- **The URL form matters for some callers.** `jdbc:oracle:thin:@localhost:1521:XE` is the SID form. The service form
  `@//localhost:1521/XE` is equivalent for sql2o, but `DriverManager` based tests in this repository use the SID form.
- **Casting a string to a timestamp fails** with `ORA-01843`, because the session NLS format is not ISO. Write
  `timestamp '2020-01-01 12:34:56'` instead, or give `to_timestamp_tz` an explicit format.
- **`returnGeneratedKeys` is off** by default, since Oracle needs the key names rather than a flag.
- **Oracle timestamps** may come back as `oracle.sql.TIMESTAMP`, which is not convertible to a `java.util.Date`;
  `OracleQuirks.getRSVal` reads those as a `java.sql.Timestamp` instead.
- **The test database.** The tests connect as `system`/`testpassword`. `src/test/resources/setup/test.sql` is not
  applied by `docker compose` and nothing uses the `test` user it creates.

## Known gaps

`OracleTypedParameterTest` runs 80 tests and **3 fail** against Oracle XE 21c, all three of them `Instant`, described
above. Everything else in the matrix passes: `String`, the integral types, `Boolean`, `BigDecimal`, `java.sql.Date`,
`java.sql.Time` is absent, `Timestamp`, `java.util.Date`, `LocalDate`, `LocalDateTime`, `OffsetDateTime`, `UUID`, enums
and blobs bound both as a byte array and as a stream; a null going in and coming back out for every type that has a
column here; and a clob written and read back whole, in both a field and a scalar.
