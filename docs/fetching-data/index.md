---
title: Fetching data from database
layout: docs
topmenu: docs
leftmenu: fetchdata
---


### Retrieve Result as Model

When our sql2o instance is set up in our DAO class, we are ready to run some queries. The first thing we want to do,
is fetching some data from the database. Sql2o is designed to parse data into POJO obects, so we will first have to
create out POJO class. In this example, it is assumed that we are developing a task-list application, Therefore we need
to create a Task class with id, description and dueDate properties. In a real life application we would probably create
a lot more properties, but as this is only a demonstration we will stick with these 3 properties.

{% highlight java %}
public class Task {

    private Long id;
    private String description;
    private Date dueDate;

    // getters and setters here
}
{% endhighlight %}

Note that the getters and setters above are voluntary. If you only want to create public fields, and omit the getters
and setters, that is for you to decide. Sql2o works for both schemes.

If we assume that we have a database table called TASKS, that has an ID, DESCRIPTION and DUEDATE column, we can fetch
all columns and parse the resulting data into a list Task objects with the following code.

{% highlight java %}
public List<Task> getAllTasks(){
    String sql =
        "SELECT id, description, duedate " +
        "FROM tasks";

    return sql2o.createQuery(sql).executeAndFetch(Task.class);
}
{% endhighlight %}

It is of course possible to add parameters to our queries. Lets say we want to fetch all tasks, where dueDate is between
2 given dates.

{% highlight java %}
public List<Task> getTasksBetweenDates(Date fromDate, Date toDate){
    String sql =
        "SELECT id, description, duedate " +
        "FROM tasks " +
        "WHERE duedate >= :fromDate AND duedate < :toDate";

    return sql2o.createQuery(sql).addParameter("fromDate", fromDate).addParameter("toDate", toDate).executeAndFetch(Task.class);
}
{% endhighlight %}


### Retrieve Result as Scalar


### Retrieve Result from Arbitrary Query
