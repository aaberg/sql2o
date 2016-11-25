---
title: Transactions
layout: docs
topmenu: docs
leftmenu: transactions
base_url: ../../
---

You can open a transaction by calling the beginTransaction() method on the Sql2o instance; this returns a Connection
instance. All queries created with the createQuery() method of the returned Connection object, is run in a transaction.
Call commit() or rollback() to either commit or rollback the transaction and close the connection.

In the example below, a few queries are run in a transaction.


{% highlight java %}
String sql1 = "INSERT INTO SomeTable(id, value) VALUES (:id, :value)";
String sql2 = "UPDATE SomeOtherTable SET value = :val WHERE id = :id";

try (Connection con = sql2o.beginTransaction()) {
    con.createQuery(sql1).addParameter("id", idVariable1).addParameter("val", valueVariable1).executeUpdate();
    con.createQuery(sql2).addParameter("id", idVariable2).addParameter("val", valueVariable2).executeUpdate();
    con.commit();
} 
{% endhighlight %}

**Note:** If you don't explisitely call commit() or rollback() on the Connection object, the transaction will automatically be rolled back when exiting
the try-with-resources block.
