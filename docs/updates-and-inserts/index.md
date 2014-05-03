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

try (Connection con = sql2o.open()) {
    con.createQuery(updateSql)
	    .addParameter("valParam", foo)
	    .addParameter("idParam", bar)
	    .executeUpdate();
}

{% endhighlight %}

**Insert example:**
{% highlight java %}

String insertSql = 
	"insert into myTable(id, value) " +
	"values (:idParam, :valParam)";

try (Connection con = sql2o.open()) {
    con.createQuery(insertSql)
	    .addParameter("idParam", bar)
	    .addParameter("valParam", foo)
	    .executeUpdate();
}

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

try (Connection con = sql2o.open()) {
    int insertedId = con.createQuery(sql, true)
	    .addParameter("val", someValue)
	    .executeUpdate()
	    .getKey();
}

{% endhighlight %}

### The bind() method.

If you need to add many parameters from a POJO class, you can use the Query.bind(Object) method.

**Example:**
{% highlight java %}

public class MyModel {
	private int prop1;
	private String prop2;
	private String prop3;
	private Date prop4;
	// and so on..

	// Getters and settes
}

{% endhighlight %}

{% highlight java %}

MyModel model = getAnInstanceOfMyModel();

// Give the parameters the same names as the corresponding properties in your model class
String sql = 
	"insert into MYTABLE(col1, col2, col3, col4 ...) "
	"values (:prop1, :prop2, :prop3, :prop4 ...)";

try (Connection con = sql2o.open()) {
    con.createQuery(sql).bind(model).executeUpdate();
}

{% endhighlight %}
