# sql2o [![Build Status](https://travis-ci.org/aaberg/sql2o.svg?branch=master)](https://travis-ci.org/aaberg/sql2o)

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
Code is available [here](https://github.com/aaberg/sql2o/blob/master/core/src/test/java/org/sql2o/performance/PojoPerformanceTest.java).

Method                                                              | Duration               |
------------------------------------------------------------------- | ---------------------- |
Hand coded <code>ResultSet</code>                                   | 60ms                   |
Sql2o                                                               | 75ms (25% slower)      |
[Apache DbUtils](http://commons.apache.org/proper/commons-dbutils/) | 98ms (63% slower)      |
[Yank](https://github.com/timmolter/Yank)                           | 144ms (144% slower)    |
[JDBI](http://jdbi.org/)                                            | 197ms (228% slower)    |
[MyBatis](http://mybatis.github.io/mybatis-3/)                      | 293ms (388% slower)    |
[jOOQ](http://www.jooq.org)                                         | 447ms (645% slower)    |
[Hibernate](http://hibernate.org/)                                  | 494ms (723% slower)    |
[Spring JdbcTemplate](http://docs.spring.io/spring/docs/current/spring-framework-reference/html/jdbc.html) | 636ms (960% slower) |

## Contributing

Want to contribute? Awesome! Here's how to set up.

#### Coding guidelines.

When hacking sql2o, please follow [these coding guidelines](https://github.com/aaberg/sql2o/wiki/Coding-guidelines).

#### Download oracle driver

To run the oracle database tests it is necessary to download the oracle jdbc driver manually, and register it with maven.
Oracle does not have a public repository where maven can download the driver automatically.
Note that public repositories do exist, but they are all technically illegal.

* Download the ojdbc6.jar version 11.2.0.3 from [here](http://www.oracle.com/technetwork/database/enterprise-edition/jdbc-112010-090769.html)
* Install it into your local maven repo by running this command:
```
mvn install:install-file -DgroupId=com.oracle -DartifactId=ojdbc6 -Dversion=11.2.0.3 -Dpackaging=jar -Dfile=ojdbc6.jar -DgeneratePom=true
```
