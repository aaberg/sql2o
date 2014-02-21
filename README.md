# sql2o

Sql2o is a small java library, with the purpose of making database interaction easy.
When fetching data from the database, the ResultSet will automatically be filled into you POJO objects.
Kind of like an ORM, but without the sql generation capabilities.

### Examples

Check out the [sql2o website](http://www.sql2o.org) for examples.

### Performance

A key feature of sql2o is performance. Following [Dapper.NET](https://github.com/SamSaffron/dapper-dot-net#performance),
the metrics below show how long it takes to execute 500 SELECT statements against a DB and map the data returned to objects.

The performance tests are broken in to 3 lists:

* POCO serialization for frameworks that support pulling static typed objects from the DB. Using raw SQL.
* Dynamic serialization for frameworks that support returning dynamic lists of objects.
* Typical framework usage. Often typical framework usage differs from the optimal usage performance wise. Often it will not involve writing SQL.

#### Performance of SELECT mapping over 500 iterations - POCO serialization

<table>
  <tr>
  	<th>Method</th>
		<th>Duration</th>
		<th>Remarks</th>
	</tr>
	<tr>
		<td>Hand coded (using a <code>SqlDataReader</code>)</td>
		<td>47ms</td>
		<td rowspan="9"><a href="http://www.toptensoftware.com/Articles/94/PetaPoco-More-Speed">Can be faster</a></td>
	</tr>
	<tr>
		<td>Dapper <code>ExecuteMapperQuery<Post></code></td>
		<td>49ms</td>
	</tr>
	<tr>
		<td><a href="https://github.com/ServiceStack/ServiceStack.OrmLite">ServiceStack.OrmLite</a> (QueryById)</td>
		<td>50ms</td>
	</tr>
	<tr>
		<td><a href="http://www.toptensoftware.com/petapoco/">PetaPoco</a></td>
		<td>52ms</td>
	</tr>
	<tr>
		<td>BLToolkit</td>
		<td>80ms</td>
	</tr>
	<tr>
		<td>SubSonic CodingHorror</td>
		<td>107ms</td>
	</tr>
	<tr>
		<td>NHibernate SQL</td>
		<td>104ms</td>
	</tr>
	<tr>
		<td>Linq 2 SQL <code>ExecuteQuery</code></td>
		<td>181ms</td>
	</tr>
	<tr>
		<td>Entity framework <code>ExecuteStoreQuery</code></td>
		<td>631ms</td>
	</tr>
</table>

#### Performance of SELECT mapping over 500 iterations - dynamic serialization

<table>
	<tr>
		<th>Method</th>
		<th>Duration</th>
		<th>Remarks</th>
	</tr>
	<tr>
		<td>Dapper <code>ExecuteMapperQuery</code> (dynamic)</td>
		<td>48ms</td>
		<td rowspan="3">&nbsp;</td>
	</tr>
	<tr>
		<td><a href="http://blog.wekeroad.com/helpy-stuff/and-i-shall-call-it-massive">Massive</a></td>
		<td>52ms</td>
	</tr>
	<tr>
		<td><a href="https://github.com/markrendle/Simple.Data">Simple.Data</a></td>
		<td>95ms</td>
	</tr>
</table>


#### Performance of SELECT mapping over 500 iterations - typical usage

<table>
	<tr>
		<th>Method</th>
		<th>Duration</th>
		<th>Remarks</th>
	</tr>
	<tr>
		<td>Linq 2 SQL CompiledQuery</td>
		<td>81ms</td>
		<td>Not super typical involves complex code</td>
	</tr>
	<tr>
		<td>NHibernate HQL</td>
		<td>118ms</td>
		<td>&nbsp;</td>
	</tr>
	<tr>
		<td>Linq 2 SQL</td>
		<td>559ms</td>
		<td>&nbsp;</td>
	</tr>
	<tr>
		<td>Entity framework</td>
		<td>859ms</td>
		<td>&nbsp;</td>
	</tr>
	<tr>
		<td>SubSonic ActiveRecord.SingleOrDefault</td>
		<td>3619ms</td>
		<td>&nbsp;</td>
	</tr>
</table>

Performance benchmarks are available [here](https://github.com/SamSaffron/dapper-dot-net/blob/master/Tests/PerformanceTests.cs)

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
