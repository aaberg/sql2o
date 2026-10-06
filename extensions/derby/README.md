# sql2o-derby

Apache Derby support for [sql2o](../../README.md): the quirks and the service registration that let sql2o talk to Derby
through its jdbc driver.

```xml
<dependency>
    <groupId>org.sql2o.extensions</groupId>
    <artifactId>sql2o-derby</artifactId>
    <version>1.9.0-SNAPSHOT</version>
</dependency>
```

The quirks are found by `ServiceLoader`, so nothing has to be configured:

```java
Sql2o sql2o = new Sql2o("jdbc:derby:memory:mydb;create=true", null, null);
```

Derby needs nothing running, since it is an embedded database and the driver ships with the test classpath, so this is the
second extension that can be run on its own:

```
mvn -pl extensions/derby -am test
```

Most of what follows is not guesswork. `DerbyTypedParameterTest` binds every type a caller can name in
`Query.addParameter(String, Class, Object)` against a real Derby and reads it back, and the behaviours described here are
what that test observes.

## The driver version

This extension is measured against **Derby 10.14.2.0**, and that is what `${derby.version}` in the root pom pins. It is
not the newest Derby: **10.15 and later no longer ship `org.apache.derby.jdbc.EmbeddedDriver`**. The `org/apache/derby/jdbc`
package is absent from the jar altogether, and `META-INF/services/java.sql.Driver` names
`org.apache.derby.iapi.jdbc.AutoloadedDriver` instead, with a `module-info.class` at the root of it. A project on one of
those versions has to load the driver through `ServiceLoader` rather than through `Class.forName`, and
`DerbyQuirksProvider.isUsableForClass` would need revisiting, since there is no `org.apache.derby.jdbc` class left to
match on. Nothing here is claimed about those versions.

## java.time: the driver takes none of it

This is the one thing Derby genuinely needs a quirks for, and it is worth more than the uuid.

Derby's driver predates `java.time`, and **`setObject` with any of `LocalDate`, `LocalTime`, `LocalDateTime`, `Instant`,
`OffsetDateTime` or `OffsetTime` fails**:

```
SQLDataException: An attempt was made to get a data value of type 'TIMESTAMP'
from a data value of type 'java.time.LocalDate'.
```

Asking the driver for an explicit target type does not help, which is the same conclusion the other databases led to when
their drivers refused an `Instant`. So `DerbyQuirks` registers six converters from core, each of which writes the
`java.sql` type Derby does take:

| written | converter | becomes |
| --- | --- | --- |
| `java.time.Instant` | `InstantToTimestampConverter` | `Timestamp` |
| `java.time.OffsetDateTime` | `OffsetDateTimeToTimestampConverter` | `Timestamp` |
| `java.time.OffsetTime` | `OffsetTimeToTimeConverter` | `Time` |
| `java.time.LocalDate` | `LocalDateToSqlDateConverter` | `java.sql.Date` |
| `java.time.LocalTime` | `LocalTimeToSqlTimeConverter` | `java.sql.Time` |
| `java.time.LocalDateTime` | `LocalDateTimeToTimestampConverter` | `Timestamp` |

None of them is registered by the global `Convert` registry, and that is deliberate: h2, hsqldb, postgres, oracle and db2
all take a `java.time` value as it is, and converting it for every one of them would cost them the milliseconds on a
`LocalTime` and the offset on the two types that carry one, for nothing. A driver that needs one registers it in its own
quirks, where `NoQuirks.converterOf` looks before it looks in `Convert`.

Three of these six (`LocalDateToSqlDateConverter`, `LocalTimeToSqlTimeConverter`, `LocalDateTimeToTimestampConverter`) were
added for Derby; the other three already existed for postgres, oracle and db2.

**A `LocalTime` loses its milliseconds on the way in.** A `java.sql.Time` has no fraction of a second to hold, and the
converter truncates rather than rounds. On a database whose driver takes the object itself the fraction survives, so this
is a real difference between Derby and the others and not just an implementation detail.

## UUID

Derby has no uuid type. A uuid is kept as the **sixteen bytes** of a `varchar(16) for bit data` column, which is the two
`long`s a uuid is made of laid end to end.

| | |
| --- | --- |
| Stored as | `varchar(16) for bit data` |
| Written by | `DerbyQuirks.setParameter(PreparedStatement, int, UUID)`, via `setBytes` |
| Written as | `DerbyUUIDConverter.toDatabaseParam` — two `long`s into a 16 byte buffer |
| Read by | `DerbyUUIDConverter.convert` — the bytes read back as two `long`s |

The converter is registered for `UUID.class` in the quirks, and `NoQuirks.converterOf` looks in the quirks before it looks
in the global `Convert` registry, so it covers every read path there is: `executeScalar(UUID.class)`, a POJO field of type
`UUID`, and a record component of type `UUID`. It has to be there: the `UUIDConverter` of core reads a `UUID` and the text
form of one and **throws** on a `byte[]`, which is what Derby hands back.

### Which column to use

Three types hold sixteen bytes, and the choice was measured against Derby 10.14 rather than reasoned about, because
`for bit data` is a pair of prefixes rather than a type of its own:

| type | `getObject` on real bytes | padding |
| --- | --- | --- |
| `varchar(16) for bit data` | `byte[]` | none |
| `char(16) for bit data` | `byte[]` | padded with zeros to 16 |
| `blob` | a handle to a large object | |

`varchar(16) for bit data` is the one to use. A uuid is always exactly sixteen bytes, so a `varchar` holds it whole and
`char` only holds it whole by padding, which would put four bytes on the end of every value that had fewer than sixteen.
A `blob` would hold them too and bring the trouble of a large object with it.

Note that `varchar(8) for bit data` **refuses** sixteen bytes rather than truncating, with `A truncation error was
encountered trying to shrink VARCHAR () FOR BIT DATA to length 8`, so a column narrower than a uuid is an error at write
time rather than a corrupt value afterwards.

### How to bind a uuid

Either way works, because `DerbyQuirks.setParameter(PreparedStatement, int, Object)` routes a uuid to the overload that
has one for it before it goes anywhere near the driver:

```java
query.addParameter("id", UUID.class, id);   // named
query.addParameter("id", id);               // or simply handed over
```

That routing is there for a reason. The typed overload in core (`Query.addParameter(String, Class, T)`) has branches for
`InputStream`, `Integer`, `Long`, `String`, `Timestamp`, `Time`, arrays and collections, and **no branch for `UUID`**, so a
uuid would fall through to `setParameter(Object)` with the uuid still in it, and the driver would have to place the object
itself. The untyped `addParameter(String, Object)` hands the value's own class straight to the typed overload, so it lands
in the same place and needs the same routing.

### A caveat about your own converter map

`new DerbyQuirks(myConverters)` uses `myConverters` and nothing else, so a uuid and every `java.time` type stop working
unless your map has entries for them. The same is true of `OracleQuirks`, `Db2Quirks` and `HsqlQuirks`. Either register
`new DerbyUUIDConverter()` and the six converters of the table above, or pass no map at all.

## Time zones: there is nowhere to put one

Derby has **no column type carrying a time zone at all**: `timestamp with time zone` and `time with time zone` are both
syntax errors. That has one consequence here, both sides of it measured:

- An `OffsetDateTime` is stored without its offset, and reading it back gives an `OffsetDateTime` with the offset of the jvm
  running the read. The value survives, the offset does not. This is the same as hsqldb, and for the same reason.

What Derby does **not** do is shift a `LocalTime` by the offset of the jvm on the way in, which the hsqldb driver does and
which is the reason `HsqlTypedParameterTest` has to leave that case out. `12:34:56` bound through plain jdbc, with no sql2o
involved, arrives as `12:34:56` here. That is why `DerbyTypedParameterTest` runs the whole matrix with nothing filtered
out.

## Column labels come back in upper case

`ResultSet.getColumnLabel()` answers with the alias a query gave, which is what sql2o maps by, so
`select id, val theVal` maps onto a property called `theVal` and nothing is lost here either.

What Derby does with an unquoted identifier is fold it to upper case, so that query answers with the label `THEVAL`. The
mapping still works, because name derivation is case insensitive by default and `THEVAL` is the same name as `theVal` in
that sense. Quote the alias, `val as "theVal"`, and it comes back spelled as written. `DerbyColumnLabelTest` asserts both
rather than only the one that happens to work.

## Generated keys

Derby behaves like h2, not like hsqldb:

- a batch of three rows hands back **one** key, the last;
- a multi row insert written with a comma hands back **one** key, and it is **null**.

A single insert hands back its own key correctly, which is what an identity column is for. Both the losing cases and the
working one are asserted in `DerbyGeneratedKeysTest`, because an assertion that fits one database says nothing about
another.

## Known gaps and small differences

`DerbyTypedParameterTest` runs 92 tests and **all of them pass**. Nothing is filtered out of the matrix, unlike hsqldb.
Everything the matrix covers elsewhere is covered here: `String`, the integral types, `Boolean`, `BigDecimal`,
`java.sql.Date`, `java.sql.Time`, `Timestamp`, `java.util.Date`, `LocalDate`, `LocalTime`, `LocalDateTime`, `Instant`,
`OffsetDateTime`, `OffsetTime`, `UUID` and enums, a null going in and coming back out for every one of them, a clob of
twenty thousand characters written four ways — as a String with its type named, without, as a typed reader and as an
untyped reader — a blob written as a byte array and as a stream, and both of the big columns read through a scalar and
through a field.

Three smaller things, none of which sql2o has to do anything about but all of which are worth knowing:

- **A `smallint` column answers `getObject` with an `Integer`, not a `Short`.** `ResultSet.getShort` gives a `Short` if a
  caller wants one, and the converters of core take either. This is a fact about the driver.
- **Derby has a real `boolean` column type**, which hsqldb does not. It reads back as a `Boolean` and compares against
  `true`, `false`, `1` and even the string `'true'`.
- **A `varchar(10)` given eleven characters is an error**, `A truncation error was encountered trying to shrink VARCHAR`,
  rather than a silent truncation. Derby also has no `limit`: paging is `offset n rows fetch next m rows only`, and both
  that and `fetch first n rows only` work.