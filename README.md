# sql2o

Sql2o is a small java library, with the purpose of making database interaction easy.
When fetching data from the database, the ResultSet will automatically be filled into you POJO objects.
Kind of like an ORM, but without the sql generation capabilities.

### Examples

Check out the [sql2o website](http://www.sql2o.org) for examples.

### Performance

A key feature of sql2o is performance. Following [Dapper.NET](https://github.com/SamSaffron/dapper-dot-net#performance),
the metrics below show how long it takes to execute 500 SELECT statements against a DB and map the data returned to objects.

The performance tests are broken in to 2 lists:

1. POCO serialization for frameworks that support pulling static typed objects from the DB. Using raw SQL.
2. Typical framework usage. Often typical framework usage differs from the optimal usage performance wise. Often it will not involve writing SQL.

#### Performance of SELECT mapping over 1000 iterations - POCO serialization

TODO

#### Performance of SELECT mapping over 1000 iterations - typical usage

<table>
	<tr>
		<th>Method</th>
		<th>Duration</th>
	</tr>
	<tr>
		<td>Hand coded (using a <code>ResultSet</code>)</td>
		<td>143ms</td>
	</tr>
	<tr>
		<td>sql2o</td>
		<td>388ms</td>
	</tr>
	<tr>
		<td>Hibernate</td>
		<td>836ms</td>
	</tr>
</table>

Performance benchmarks are available [here](/src/test/java/performance/PerformanceTests.cs).

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
