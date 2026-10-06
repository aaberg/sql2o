# sql2o-hsqldb

HSQLDB support for [sql2o](../../README.md): the quirks and the service registration that let sql2o talk to HSQLDB
through its jdbc driver.

```xml
<dependency>
    <groupId>org.sql2o.extensions</groupId>
    <artifactId>sql2o-hsqldb</artifactId>
    <version>1.9.0-SNAPSHOT</version>
</dependency>
```

The quirks are found by `ServiceLoader`, so nothing has to be configured:

```java
Sql2o sql2o = new Sql2o("jdbc:hsqldb:mem:mydb", "SA", "");
```

This used to live in the test classpath of core rather than in an extension of its own. It is a separate artifact now, so
that a project which does not use HSQLDB does not have the driver on its classpath at all, and so that the quirks of a
database live next to the quirks of every other database rather than inside the core tests.

Most of what follows is not guesswork. `HsqlTypedParameterTest` binds every type a caller can name in
`Query.addParameter(String, Class, Object)` against a real HSQLDB and reads it back, and the behaviours described here
are what that test observes. Run the whole extension with `mvn -pl extensions/hsqldb -am test`; unlike the other
extensions it needs nothing running, since the database is in memory and the driver is on the test classpath.

## UUID

HSQLDB has no uuid type. A uuid is kept as the **sixteen bytes** of a `binary(16)` column, which is the two `long`s a uuid
is made of laid end to end.

| | |
| --- | --- |
| Stored as | `binary(16)` |
| Written by | `HsqlQuirks.setParameter(PreparedStatement, int, UUID)`, via `setBytes` |
| Written as | `HsqlUUIDConverter.toDatabaseParam` — two `long`s into a 16 byte buffer |
| Read by | `HsqlUUIDConverter.convert` — the bytes read back as two `long`s |

The converter is registered for `UUID.class` in the quirks, and `NoQuirks.converterOf` looks in the quirks before it
looks in the global `Convert` registry, so it covers every read path there is: `executeScalar(UUID.class)`, a POJO field
of type `UUID`, and a record component of type `UUID`. It has to be there: the `UUIDConverter` of core reads a `UUID`
and the text form of one and **throws** on a `byte[]`, which is what HSQLDB hands back.

### Which column to use

Three types can hold sixteen bytes, and the choice was measured against HSQLDB 2.7 rather than reasoned about, because
it decides whether the bytes come back as bytes at all:

| type | `getObject` on real bytes | padding |
| --- | --- | --- |
| `binary(16)` | `byte[]` | padded to 16 |
| `varbinary(16)` | `byte[]` | none |
| `longvarbinary` | `byte[]` | none |
| `varchar(36)`, `char(36)` | `String` | |

All three of the first ones work for a uuid, since a uuid is always exactly sixteen bytes. `binary(16)` is the one to
use: `varchar` and `char` arrive as text, which would mean storing the dashed form as text and reading it back as text,
and `longvarbinary` is a large object, which brings the trouble described further down.

### How to bind a uuid

Either way works, because `HsqlQuirks.setParameter(PreparedStatement, int, Object)` routes a uuid to the overload that
has one for it before it goes anywhere near the driver:

```java
query.addParameter("id", UUID.class, id);   // named
query.addParameter("id", id);               // or simply handed over
```

That routing is there for a reason. The typed overload in core (`Query.addParameter(String, Class, T)`) has branches for
`InputStream`, `Integer`, `Long`, `String`, `Timestamp`, `Time`, arrays and collections, and **no branch for `UUID`**,
so a uuid would fall through to `setParameter(Object)` with the uuid still in it, and the driver would have to place the
object itself. The untyped `addParameter(String, Object)` hands the value's own class straight to the typed overload, so
it lands in the same place and needs the same routing.

### A caveat about your own converter map

`new HsqlQuirks(myConverters)` uses `myConverters` and nothing else, so a uuid stops working unless the map has a
`UUID.class` entry of your own. The same is true of `OracleQuirks` and `Db2Quirks`. Register `new HsqlUUIDConverter()`
in the map if you pass one.

## Time zones: there is nowhere to put one

HSQLDB has **no column type carrying a time zone at all**. `cast(... as timestamp with time zone)` is refused, and so is
`time with time zone`. That has two consequences, both measured:

- An `OffsetDateTime` is stored without its offset, and reading it back gives an `OffsetDateTime` with the offset of the
  jvm running the read. The value survives, the offset does not.
- The driver shifts a `LocalTime` by the offset of the jvm on the way **in**, so 12:34:56 arrives as 15:34:56 on a
  machine set to Europe/Moscow. Binding it through plain jdbc, with no sql2o involved, stores the shifted value as well,
  so this happens before sql2o sees anything. `LocalTimeConverterTest` in core notes the same thing about HSQLDB
  converting a local time before storing it, and skips its own assertions for the same reason.

## Column labels

`ResultSet.getColumnName()` answers with the name of the column underneath, even when the query gave an alias, while
`getColumnLabel()` answers with the alias. sql2o reads the label, so `select id, val theVal` maps onto a property
called `theVal`. `HsqlColumnLabelTest` covers this, which is where it used to be tested: as one case among many in
`IssuesTest` of core, running against this database as well as against H2, where the statement about HSQLDB meant nothing.

## Generated keys

H2 answers a multi row insert with only the last generated identity; HSQLDB hands back every one of them, for a plain
insert of several rows and for a batch alike. Both facts are asserted where the database they are about can run them:
the weaker assertion in `Sql2oTest` of core, the strict one in `HsqlGeneratedKeysTest`.

## Known gaps

`HsqlTypedParameterTest` runs 86 tests and **all of them pass**. Everything is covered that the matrix covers elsewhere:
`String`, the integral types, `Boolean`, `BigDecimal`, `java.sql.Date`, `java.sql.Time`, `Timestamp`, `java.util.Date`,
`LocalDate`, `LocalDateTime`, `OffsetDateTime`, `UUID` and enums, a null going in and coming back out for every one of
them, a clob of twenty thousand characters and a blob, and both of the big columns read through a scalar and through a
field. `LocalTime` is left out for the reason given above.