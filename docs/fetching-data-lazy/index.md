---
title: Fetching data lazily
layout: docs
topmenu: docs
leftmenu: fetchdatalazy
---

If you need to read a large amount of data, you may run into memory issues using executeAndFetch(). You can use executeAndFetchLazy() to iterate through a result set and ensure you do not run out of memory. 

**Warning:** When using executeAndFetchLazy() you MUST close the returned iterable in a finally block, 
or you will leak a database connection.

As an example, let's say you have millions of tasks in your db and you want to read them in batches of 1,000,
flushing each batch to a file as you go:

{% highlight java %}
public void readAndFlushAllTasks() {

    String sql = "SELECT id, description, duedate " +
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

If you are using Java 7 or higher, you can use try-with-resources to make this easier to read: 

{% highlight java %}
public void readAndFlushAllTasks() {

    String sql = "SELECT id, description, duedate " +
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

**Caution:** Generally speaking, you do not want to expose the returned iterable outside of your data
layer, because a connection to the database remains open until the iterable is closed.
