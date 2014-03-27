# sql2o

Sql2o is a small java library, with the purpose of making database interaction easy.
When fetching data from the database, the ResultSet will automatically be filled into you POJO objects.
Kind of like an ORM, but without the sql generation capabilities.

### Examples

Check out the [sql2o website](http://www.sql2o.org) for examples.

### Performance

A key feature of sql2o is performance. The following metrics were based off the
[Dapper.NET metrics](https://github.com/SamSaffron/dapper-dot-net#performance).
Note that *typical usage* can differ from *optimal usage* for many frameworks. Depending on the framework,
typical usage may not involve writing any SQL, or it may map underscore case to camel case, etc.

#### Performance of SELECT

Execute 1000 SELECT statements against a DB and map the data returned to a POJO.
Benchmarks are available [here](/src/test/java/org/sql2o/performance/PerformanceTests.java).

Method | Duration - Typical | Duration - Optimal
-------------  | ------------- | -------------
Hand coded <code>ResultSet</code> | 65ms | 65ms
sql2o | 79ms (21.5% slower) | 68ms (4.6% slower)
[Hibernate](http://hibernate.org/) | 393ms (505% slower) | TODO
[JDBI](http://jdbi.org/) | TODO | TODO
[jOOQ](http://www.jooq.org) | TODO | TODO
[Apache DbUtils](http://commons.apache.org/proper/commons-dbutils/) | TODO | TODO
[MyBatis](http://mybatis.github.io/mybatis-3/) | TODO | TODO
[Spring JdbcTemplate](http://docs.spring.io/spring/docs/current/spring-framework-reference/html/jdbc.html) | TODO | TODO

## Contributing

Want to contribute? Here's how to set up.

#### Download oracle driver

To run the oracle database tests it is necessary to download the oracle jdbc driver manually, and register it with maven.
Oracle does not have a public repository where maven can download the driver automatically.
Note that public repositories do exist, but they are all technically illegal.

* Download the ojdbc6.jar version 11.2.0.3 from [here](http://www.oracle.com/technetwork/database/enterprise-edition/jdbc-112010-090769.html)
* Install it into your local maven repo by running this command:
```
mvn install:install-file -DgroupId=com.oracle -DartifactId=ojdbc6 -Dversion=11.2.0.3 -Dpackaging=jar -Dfile=ojdbc6.jar -DgeneratePom=true
```
