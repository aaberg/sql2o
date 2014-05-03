---
title: Fetching data lazily
layout: docs
topmenu: docs
leftmenu: fetchdatalazy
---

If you need to read a large amount of data, you may run into memory issues using executeAndFetch(). You can use executeAndFetchLazy() to iterate through a result set and ensure you do not run out of memory. 

As an example, let's say you have millions of tasks in your db and you want to read them in batches of 1,000,
flushing each batch to a file as you go:

{% highlight java %}
public void readAndFlushAllTasks() {

    String sql = "SELECT id, description, duedate " +
                 "FROM tasks";

    final int BATCH_SIZE = 1000;

    List<Task> batch = new ArrayList<Task>(BATCH_SIZE);

    try (Connection con = sql2o.open()) {
        try (ResultSetIterable<Task> tasks = con.createQuery(sql).executeAndFetchLazy(Task.class)) {
            for (Task task : tasks) {
                if (batch.size() == BATCH_SIZE) {
                    // here is where you flush your batch to file

                    batch.clear();
                }
                batch.add(task);
            }
        }
    }
}
{% endhighlight %}

**Note:** The ResultSetIterable class is autoClosable, so you can wrap it in a try-with-resource statement. This makes sure that the underlaying ResultSet is closed when you are done.
