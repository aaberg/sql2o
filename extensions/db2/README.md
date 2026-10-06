# sql2o-db2

DB2 support for [sql2o](../../README.md): the quirks and the service registration that let sql2o talk to DB2 LUW
through the IBM JCC driver.

```xml
<dependency>
    <groupId>org.sql2o.extensions</groupId>
    <artifactId>sql2o-db2</artifactId>
    <version>1.9.0-SNAPSHOT</version>
</dependency>
```

The quirks are found by `ServiceLoader`, so nothing has to be configured:

```java
Sql2o sql2o = new Sql2o("jdbc:db2://localhost:50000/testdb", "db2inst1", "testpassword");
```

Most of what follows is not guesswork. `Db2TypedParameterTest` binds every type a caller can name in
`Query.addParameter(String, Class, Object)` against a real DB2 and reads it back, and the behaviours described here are
what that test observes. Run it with `docker compose up -d db2-ce` followed by
`mvn -pl extensions/db2 -am test`.

## UUID

DB2 has no uuid type. A uuid is kept as the **sixteen bytes** of a `VARCHAR(16) FOR BIT DATA` column, which is the two
`long`s a uuid is made of laid end to end.

| | |
| --- | --- |
| Stored as | `VARCHAR(16) FOR BIT DATA` |
| Written by | `Db2Quirks.setParameter(PreparedStatement, int, UUID)`, via `setBytes` |
| Written as | `Db2UUIDConverter.toDatabaseParam` — two `long`s into a 16 byte buffer |
| Read by | `Db2UUIDConverter.convert` — the bytes read back as two `long`s |

The converter is registered for `UUID.class` in the quirks, and `NoQuirks.converterOf` looks in the quirks before it
looks in the global `Convert` registry, so it covers every read path there is: `executeScalar(UUID.class)`, a POJO field
of type `UUID`, and a record component of type `UUID`. It has to be there: the `UUIDConverter` of core reads a `UUID`
and the text form of one and **throws** on a `byte[]`, which is what DB2 hands back.

### Why that column and not another

Three DB2 types can hold sixteen bytes of binary data, and the choice between them was measured against DB2 11.5 rather
than reasoned about, because it decides whether the bytes come back as bytes at all:

| type | `getObject` on real bytes | padding |
| --- | --- | --- |
| `CHAR(16) FOR BIT DATA` | `byte[]` | padded to 16 with blanks |
| `VARCHAR(16) FOR BIT DATA` | `byte[]` | none |
| `BINARY(16)` | `byte[]` | padded to 16 |

All three work for a uuid, since a uuid is always exactly sixteen bytes and the padding never comes into play.
`VARCHAR(n) FOR BIT DATA` is the one to use: it is free of the blank padding that `CHAR` brings along, and it does not
tie the schema to a server version the way `BINARY` does, which is a DB2 11.5 type.

### How to bind a uuid

Either way works, because `Db2Quirks.setParameter(PreparedStatement, int, Object)` routes a uuid to the overload that
has one for it before it goes anywhere near the driver:

```java
query.addParameter("id", UUID.class, id);   // named
query.addParameter("id", id);               // or simply handed over
```

That routing is there for a reason. The typed overload in core (`Query.addParameter(String, Class, T)`) has branches for
`InputStream`, `Integer`, `Long`, `String`, `Timestamp`, `Time`, arrays and collections, and **no branch for `UUID`**,
so a uuid would fall through to `setParameter(Object)` with the uuid still in it — and DB2 refuses the conversion:

```
[jcc][1091][14266][4.32.28] Invalid data conversion: Parameter instance 6ba7b810-9dad-11d1-80b4-00c04fd430c8 is not
valid for the requested conversion. ERRORCODE=-4461, SQLSTATE=42815
```

The untyped `addParameter(String, Object)` hands the value's own class straight to the typed overload, so it lands in
the same place and needs the same routing. `Boolean`, `Short`, `Byte`, `Float`, `Double`, `BigDecimal`,
`java.sql.Date` and every `java.time` type fall through that gap as well; the ones that reach a driver anyway are on
their own below.

### A caveat about your own converter map

`new Db2Quirks(myConverters)` uses `myConverters` and nothing else, so a uuid stops working unless the map has a
`UUID.class` entry of your own. The same is true of `OracleQuirks`. Register `new Db2UUIDConverter()` in the map if you
pass one.

## Dates and times

DB2 hands out `DATE`, `TIME` and `TIMESTAMP` columns as `java.sql.Date`, `java.sql.Time` and `java.sql.Timestamp`, which
the converters of core already read, so there is nothing to do about them here. `Db2DateReadingApiTest` reads all three
through the public API against a real database rather than asserting it in a comment.

Three DB2 specifics that will otherwise cost you an afternoon:

- **A null cannot be selected.** `select cast(null as date)` fails with `SQLCODE=-4472`. Read the null out of a column
  instead.
- **Aliases are folded to upper case.** `select the_day as the_day` comes back as `THE_DAY`, so write
  `select the_day as theDay` — or turn name derivation on. An alias that does not match a property name means the value
  is never mapped, and nothing says so.
- **There is no `drop table if exists`.** A drop has to either succeed or have its failure swallowed, which is why the
  tests carry an empty catch around it.

## Clobs and blobs

A clob bound as a `String` and a blob bound as a `byte[]` are both stored and read back correctly, through a scalar or
through a field.

Reading them used to fail, and the reason was in core rather than here, so it is worth writing down. DB2 hands a clob
and a blob back as a **locator**, which is only valid as long as the statement that produced it. `Query.executeScalar()`
fetches the value and closes the `ResultSet` and the `PreparedStatement` in a try-with-resources block, and
`executeScalar(Class)` used to convert afterwards:

```
try (final PreparedStatement ps = buildPreparedStatement();
     final ResultSet rs = ps.executeQuery()) {
    if (rs.next()) {
        Object o = getQuirks().getRSVal(rs, 1);
        return o;                      // the locator leaves the block and is closed with it
    }
}
```

By the time the converter read it, DB2 had invalidated it and answered `SQLCODE=-4470, Lob object is closed`. That
showed up as:

```
org.sql2o.Sql2oException: Error occured while converting value from database
  caused by com.ibm.db2.jcc.am.SqlException: Invalid operation: Lob object is closed.
    ERRORCODE=-4470, SQLSTATE=null
    at org.sql2o.converters.ByteArrayConverter.convert(ByteArrayConverter.java:34)
```

`executeScalar(Class)` now converts while the `ResultSet` is still open, by way of the handler `executeScalarList`
already used, so every read path in the table below works:

| read | |
| --- | --- |
| `executeScalar(String.class)` on a clob | works |
| `executeScalar(byte[].class)` on a blob | works |
| a `String` field mapped from a clob column | works |
| a `byte[]` field mapped from a blob column | works |

`docs/converter-exceptions.md` in the root of the repository covers the other half of the blob story, which is what
`ByteArrayConverter` does when it is handed something it cannot read.

## Other DB2 specifics

- **Column naming is left to `NoQuirks`**, which returns the label of a column and therefore the alias a query gave it.
  This used to ask the metadata for the underlying name instead, so `select id as my_id` was mapped by `ID` and the
  value silently never reached the property it was written for. `Db2QuirksTest` guards that.
- **The test database.** `docker compose up -d db2-ce` starts `ibmcom/db2:11.5.8.0` on port 50000 as `db2inst1`. It
  needs `--privileged` and a `shm_size`, and it takes several minutes to initialise the first time: readiness is the
  `Setup has completed` line in the logs, not the container being up. The equivalent image on the IBM registry,
  `icr.io/db2_ce/db2`, wants an IBM Cloud entitlement; the Docker Hub path is the anonymous one.

## Known gaps

`Db2TypedParameterTest` runs 89 tests and **9 fail** against DB2 11.5.8, all of them the same defect in sql2o rather
than something DB2 declines:

| cases | failure |
| --- | --- |
| `Instant`, `OffsetDateTime`, `OffsetTime` — bound, filtered and nulled | `ERRORCODE=-4461`, the value reaches the driver as a `java.time` object it cannot place in a `TIMESTAMP` column. The same gap shows up on Oracle and Postgres |

Everything else passes: `String`, the integral types, `Boolean`, `BigDecimal`, `java.sql.Date`, `java.sql.Time`,
`Timestamp`, `java.util.Date`, `LocalDate`, `LocalTime`, `LocalDateTime`, `UUID` and enums, a null going in and coming
back out for every one of them, and both big columns through a scalar and through a field.
