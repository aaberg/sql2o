# sql2o-mysql

MySQL and MariaDB support for [sql2o](../../README.md): the quirks and the service registration that let sql2o talk
to either through the MySQL jdbc driver.

```xml
<dependency>
    <groupId>org.sql2o.extensions</groupId>
    <artifactId>sql2o-mysql</artifactId>
    <version>1.9.0-SNAPSHOT</version>
</dependency>
```

The quirks are found by `ServiceLoader`, so nothing has to be configured:

```java
Sql2o sql2o = new Sql2o("jdbc:mysql://localhost:3306/mydb", "user", "password");
```

The tests run against MariaDB rather than MySQL itself, on the grounds that it answers the same wire protocol: the
driver is still the MySQL one, and the provider claims `jdbc:mysql:` urls and `com.mysql.cj.jdbc` classes only. A
`jdbc:mariadb:` url belongs to the MariaDB driver and is left alone.

Most of what follows is not guesswork. `MysqlTypedParameterTest` binds every type a caller can name in
`Query.addParameter(String, Class, Object)` against a real MariaDB 11.4 through Connector/J 9.7.0 and reads it back,
and the behaviours described here are what that test observes. Start the database with
`docker compose up -d mariadb-db` (host port 13306, `testuser`/`testpassword`, database `testdb`) and run the whole
extension with `mvn -pl extensions/mysql -am test`.

## UUID

Neither MySQL nor MariaDB has a uuid type. A uuid is kept as the **sixteen bytes** of a `binary(16)` column, which
is the two `long`s a uuid is made of laid end to end.

| | |
| --- | --- |
| Stored as | `binary(16)` |
| Written by | `MysqlQuirks.setParameter(PreparedStatement, int, UUID)`, via `setBytes` |
| Written as | `MysqlUUIDConverter.toDatabaseParam` — two `long`s into a 16 byte buffer |
| Read by | `MysqlUUIDConverter.convert` — the bytes read back as two `long`s |

The converter is registered for `UUID.class` in the quirks, and `NoQuirks.converterOf` looks in the quirks before it
looks in the global `Convert` registry, so it covers every read path there is: `executeScalar(UUID.class)`, a POJO field
of type `UUID`, and a record component of type `UUID`. It has to be there: the `UUIDConverter` of core reads a `UUID`
and the text form of one and **throws** on a `byte[]`, which is what the driver hands back.

### Which column to use

Measured against the test database rather than reasoned about, because it decides whether the bytes come back as
bytes at all:

| type | `getObject` on real bytes |
| --- | --- |
| `binary(16)` | `byte[]`, exactly sixteen bytes |
| `varbinary(16)` | `byte[]` |
| `char(36)` | `String`, the dashed text form |

`binary(16)` is the one to use: a uuid is always exactly sixteen bytes, so there is nothing to pad, and the text
form would mean storing the dashed string and reading it back as text.

### How to bind a uuid

Either way works, because `MysqlQuirks.setParameter(PreparedStatement, int, Object)` routes a uuid to the overload that
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

`new MysqlQuirks(myConverters)` uses `myConverters` and nothing else, so a uuid stops working unless the map has a
`UUID.class` entry of your own. The same is true of `OracleQuirks` and `Db2Quirks`. Register `new MysqlUUIDConverter()`
in the map if you pass one.

## Unsigned integers

An `integer unsigned` comes back as a `Long` and a `bigint unsigned` as a `BigInteger`: the values do not fit the
signed type of the same width, so the driver widens them. Both are asserted in `MysqlUnsignedTest`, as scalars and as
fields, since H2 has no unsigned types at all and there is nowhere else to pin this.

## Booleans

The boolean column is a `tinyint(1)`, which is the idiomatic MySQL boolean. The driver reports it as `BIT` and hands
back a `Boolean` both ways, with no converter involved beyond the one core already has.

## Temporal types

`date` gives a `java.sql.Date`, `time` a `java.sql.Time`, and both round trip without shifting: unlike HSQLDB, nothing
here moves a `LocalTime` on the way in, so the matrix needs no filter for it. `MysqlTypedParameterTest` spells the
timestamp column `timestamp(3)` — a plain `timestamp` keeps whole seconds only, and the `Timestamp` case carries
milliseconds the test expects back.

`datetime` arrives as a `java.time.LocalDateTime` straight from the driver, while `timestamp` arrives as a
`java.sql.Timestamp`; either converts. There is no column type carrying a time zone, so an `OffsetDateTime` is stored
without its offset and read back with the offset of the jvm, and the matrix uses the jvm offset on both ends.

A `timestamp` starts at `1970-01-01 00:00:01`: anything earlier, the epoch included, is not storable and the server
refuses it. The shared matrix therefore takes its "other" date and instant values from 1999, like every other
temporal case there, rather than from the epoch.

No connection properties are needed: the driver connects with a bare url and raises no complaint about the server
time zone.

## Column labels

`ResultSet.getColumnName()` answers with the name of the column underneath, even when the query gave an alias, while
`getColumnLabel()` answers with the alias, which it keeps exactly as written. sql2o reads the label, so
`select id, val theVal` maps onto a property called `theVal`. `MysqlColumnLabelTest` covers this.

## Generated keys

A multi row insert hands back every generated identity, and so does a batch. Both facts are asserted in
`MysqlGeneratedKeysTest`. A single key arrives as a `BigInteger`, which the typed `getKeys(Integer.class)` converts
on the way out.

## Wide columns and reserved words

The typed parameter matrix keeps a `blob` and a `text` column, and both stay inline: a six byte blob comes back as a
`byte[]`, and twenty thousand characters of text as a `String`, with no locator involved.

`blob` is a reserved word in MySQL and MariaDB, so a column actually called that has to go quoted wherever it appears
in a statement. The base matrix this extension inherits names its wide columns `blob` and `clob` outright, which is
why `AbstractTypedParameterTest` asks the subclass for their names through `blobColumn()` and `clobColumn()`: every
other database answers with the names as they were, and this one answers with them in backticks. The labels come back
without the quotes, so the fields they land in keep theirs.

## Known gaps

`MysqlTypedParameterTest` runs 92 tests and **all of them pass**, with no case filtered out. Everything is covered
that the matrix covers elsewhere: `String`, the integral types, `Boolean`, `BigDecimal`, `java.sql.Date`,
`java.sql.Time`, `Timestamp`, `java.util.Date`, `LocalDate`, `LocalTime`, `LocalDateTime`, `Instant`,
`OffsetDateTime`, `OffsetTime`, `UUID` and enums, a null going in and coming back out for every one of them, a clob of
twenty thousand characters written three ways — as a String with its type named, without, and as a reader — a blob
written as a byte array and as a stream, and both of the big columns read through a scalar and through a field.
