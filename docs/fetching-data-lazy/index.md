---
title: Fetching data lazily
layout: docs
topmenu: docs
leftmenu: fetchdatalazy
---

If you need to read a large amount of data, you may run into memory issues using executeAndFetch(), which reads all results 
into an in-memory list. You can use executeAndFetchLazy() to iterate through a result set lazily and ensure you do not run out
of memory. However when using this, be careful to close the returned iterable when you are done, or you will leak a database
connection. As additional caution, be careful exposing the returned iterable outside of your data layer, because a connection
is open during the life of the iterable.

As an example, let's say you have millions of tasks in your task table, and you want to read them all in batches of 1,000,
flushing each batch to a file as you go:

{% highlight java %}
public class Task {

    private Long id;
    private String description;
    private Date dueDate;

    // getters and setters here
}
{% endhighlight %}

{% highlight java %}
public void readAndFlushAllTasks() {

    String sql =
        "SELECT id, description, duedate " +
        "FROM tasks";

    final int BATCH_SIZE = 1000;

    List<Task> batch = new ArrayList<Task>(BATCH_SIZE);

    ResultSetIterable<T> tasks = null;
    try {
        tasks = sql2o.createQuery(sql).executeAndFetchLazy(Task.class);
        for (Task task : tasks) {
            if (batch.size() == BATCH_SIZE) {

                // here is where you flush your batch to file

                batch.clear();
            }
            batch.add(task);
        }
    }
    finally {
        if (tasks != null) {
            tasks.close();
        }
    }
}
{% endhighlight %}

If you are using Java 7 or higher, you can use try-with-resources for equivalent and much easier to read functionality: 

{% highlight java %}
public void readAndFlushAllTasks() {

    String sql =
        "SELECT id, description, duedate " +
        "FROM tasks";

    final int BATCH_SIZE = 1000;

    List<Task> batch = new ArrayList<Task>(BATCH_SIZE);

    try (ResultSetIterable<Task> tasks = sql2o.createQuery(sql).executeAndFetchLazy(Task.class)) 
    {
        for (Task task : tasks) {
            if (batch.size() == BATCH_SIZE) {

                // here is where you flush your batch to file

                batch.clear();
            }
            batch.add(task);
        }
    }
}
{% endhighlight %}

**Do not forget:** you MUST close the iterable in a finally block or you will leak a database connection.
