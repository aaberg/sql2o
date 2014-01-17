---
title: Inserts and updates
layout: docs
topmenu: docs
leftmenu: insertsAndUpdates
---

With sql2o updates and inserts are executed with the executeUpdate() method.

**Update example:** 
{% highlight java %}

String updateSql = "update myTable set value = :valParam where id = :idParam";
sql2o.createQuery(updateSql)
	.addParameter("valParam", foo)
	.addParameter("idParam", bar)
	.executeUpdate();

{% endhighlight %}

**Insert example:**
{% highlight java %}

String insertSql = 
	"insert into myTable(id, value) " +
	"values (:idParam, :valParam)";

sql2o.createQuery(insertSql)
	.addParameter("idParam", bar)
	.addParameter("valParam", foo)
	.executeUpdate();

{% endhighlight %}

### Autoincremented values and identity columns

Sometimes you need to insert data into a database table with an autoincremented id (Called identity column in some databases). For this to work, you have to tell sql2o to fetch keys after insert. This is done with an overload of the createQuery method:  
[createQuery(String sql, boolean returnGeneratedKeys)](http://api.sql2o.org/1.2.0/org/sql2o/Sql2o.html#createQuery%28java.lang.String,%20boolean%29)  
To fetch the actual inserted value, call the getKey() method after executing the statement.

Example:
{% highlight java %}

// assuming a table called MYTABLE with two colums. 
// - id integer primary key autoincrement, and
// - value varchar(10)
String sql = "insert into MYTABLE ( value ) values ( :val )";

int insertedId = sql2o.createQuery(sql, true)
	.addParameter("val", someValue)
	.executeUpdate()
	.getKey();

{% endhighlight %}
