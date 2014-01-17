---
title: Transactions
layout: docs
topmenu: docs
leftmenu: transactions
---

You can open a transaction by calling the beginTransaction() method on the Sql2o instance; this returns a Connection
instance. All queries created with the createQuery() method of the returned Connection object, is run in a transaction.
Call commit() or rollback() to either commit or rollback the transaction and close the connection.

In the example below, a few queries are run in a transaction.


{% highlight java %}
String sql1 = "INSERT INTO SomeTable(id, value) VALUES (:id, :value)";
String sql2 = "UPDATE SomeOtherTable SET value = :val WHERE id = :id";

Connection connection = null;
try{
    connection = sql2o.beginTransaction();
    connection.createQuery(sql1).addParameter("id", idVariable1).addParameter("val", valueVariable1).executeUpdate();
    connection.createQuery(sql2).addParameter("id", idVariable2).addParameter("val", valueVariable2).executeUpdate();
    connection.commit();
} catch(Throwable t){
    if (connection != null){
        connection.rollback();
    }
    throw t;
}
{% endhighlight %}

Be very careful with your try-catch logic when executing sql in transactions. If an exception is thrown, and your code
doesn't call either rollback() or commit(), the transaction will not be closed. Most relational databases use row-locks
or even table-locks when sql is run in transactions, which might cause some pretty nasty deadlocks if your transactions
are not closed.

To make things a bit easier, Sql2o offers another way of running sql in a transaction. With this method Sql2o ensures
that the transaction is always closed without the need of try-catch logic. Simply create a class that implements the
StatementRunnable or StatementRunnableWithResult interface, and use it as a parameter to the runInTransaction() method.

If an exception is thrown within the run() method of the StatementRunnable, the transaction is automatically rolled back.
If everything goes well and without exceptions, the transaction is automatically committed.

{% highlight java %}
public void doSomething() {

    MyStatemenRunnable statemenRunnable = new MyStatemenRunnable();
    sql2o.runInTransaction(statemenRunnable);
}

public static class MyStatemenRunnable implements StatementRunnable{

    public void run(Connection connection, Object argument) throws Throwable {
        connection.createQuery("insert into...").executeUpdate();
        // all queries that are created with the connection.createQuery(..) method, are executed in the transaction.
    }
}
{% endhighlight %}

With an anonomous inner class it looks something like this:

{% highlight java %}
public void doSomething() {
    sql2o.runInTransaction(new StatementRunnable() {
        public void run(Connection connection, Object argument) throws Throwable {

            connection.createQuery("insert into...").executeUpdate();
            // all queries that are created with the connection.createQuery(..) method, are executed in the transaction.

        }
    });
}
{% endhighlight %}
