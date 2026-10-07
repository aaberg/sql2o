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

`new OracleQuirks(myConverters)` uses `myConverters` and nothing else, so everything this extension adds stops
working unless the map carries it: a uuid needs a `UUID.class` entry, and an `Instant` needs an `Instant.class` one. The
same is true of `Db2Quirks`. Register `new OracleUUIDConverter()` and `InstantToTimestampConverter` in the map if you pass
one.

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

### Instant has to be written as a Timestamp

Oracle cannot place a `java.time.Instant` in a `TIMESTAMP` column and says so twice over — `ORA-17004` for the object,
`ORA-17132` when an explicit type is asked for instead — so `OracleQuirks` registers `InstantToTimestampConverter` from
core and the value goes as a `java.sql.Timestamp`. An instant carries no offset, so that is not a loss: the same point on
the timeline comes back out, and `TIMESTAMP WITH TIME ZONE` would have normalised it to the instant anyway.

`OffsetDateTime` is left alone. Oracle takes it as it is and has a column type with a zone in it to keep the offset, which
a `Timestamp` would have thrown away. It is not covered here that a value written as an `OffsetDateTime` into a
`TIMESTAMP WITH TIME ZONE` round trips; what is covered is that it goes into a plain `TIMESTAMP`, where the offset is not
stored and reading it back gives the offset of the jvm doing the reading.

## Other Oracle specifics

- **The URL form matters for some callers.** `jdbc:oracle:thin:@localhost:1521:XE` is the SID form. The service form
  `@//localhost:1521/XE` is equivalent for sql2o, but `DriverManager` based tests in this repository use the SID form.
- **Casting a string to a timestamp fails** with `ORA-01843`, because the session NLS format is not ISO. Write
  `timestamp '2020-01-01 12:34:56'` instead, or give `to_timestamp_tz` an explicit format.
- **`returnGeneratedKeys` is off** by default, since Oracle needs the key names rather than a flag.
- **Oracle timestamps** may come back as `oracle.sql.TIMESTAMP`, which is not convertible to a `java.util.Date`;
  `OracleQuirks.getRSVal` reads those as a `java.sql.Timestamp` instead. That is also the path an `Instant` field is read
  through, which is why reading one is not a special case anywhere.
- **The test database.** The tests connect as `system`/`testpassword`. `src/test/resources/setup/test.sql` is not
  applied by `docker compose` and nothing uses the `test` user it creates.

## Known gaps

`OracleTypedParameterTest` runs 81 tests and **all of them pass** against Oracle XE 21c: `String`, the integral types,
`Boolean`, `BigDecimal`, `java.sql.Date`, `java.sql.Time` is absent, `Timestamp`, `java.util.Date`, `LocalDate`,
`LocalDateTime`, `OffsetDateTime`, `UUID`, enums and blobs bound both as a byte array and as a stream; a null going in and
coming back out for every type that has a column here; a clob of twenty thousand characters written three ways — as a String with its
type named, without, and as a reader — and read back whole, in both a field and a scalar;
and an instant read back into a field of its own type.

What is not covered is stated above rather than left out: a time of day has no column here at all, and a with-zone
column is not exercised.
