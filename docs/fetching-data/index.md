---
title: Fetching data from database
layout: docs
topmenu: docs
leftmenu: fetchdata
---


### Retrieve Result as Model

When our sql2o instance is set up in our DAO class, we are ready to run some queries. The first thing we want to do,
is fetching some data from the database. Sql2o is designed to parse data into POJO obects, however it can also retrieve result as a generic scalar object. Alternately if more control over the resultset is desired then result can be retrieved as Sql2o "Table" class that can be transformed as per the requirements.

This example focuses on returning result as a Model so we will first have to create out POJO class. In this example, it is assumed that we are developing a task-list application, Therefore we need to create a Task class with id, description and dueDate properties. In a real life application we would probably create a lot more properties, but as this is only a demonstration we will stick with these 3 properties.

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

    try(Connection con = sql2o.open()) {
        return con.createQuery(sql).executeAndFetch(Task.class);
    }
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

    try(Connection con = sql2o.open()) {
        return sql2o.createQuery(sql)
            .addParameter("fromDate", fromDate)
            .addParameter("toDate", toDate)
            .executeAndFetch(Task.class);
    }
}
{% endhighlight %}


### Retrieve Result as Scalar

Sometimes it is desirable to retrieve only one column from an SQL query, such as - next value of a sequence, count of records or just a primary key of a certain table. In those cases mapping a complete object to query is not only overkill but it may not even be feasible as we may not have a corresponding model class.

In such cases we can retrieve result as scalars - an arbitrary Java object mapping to a specific column that is being returned by the query.

Following are some of the examples:

- This returns count of all students in the database

{% highlight java %}
public Integer getStudentCount(){
    String sql = "SELECT count(id) FROM students";

    try (Connection con = sql2o.open()) {
        return con.createQuery(sql).executeScalar(Integer.class);
    }
}
{% endhighlight %}

- This returns List of all student Id in the database


{% highlight java %}
public List<Integer> getStudentIdList(){
    String sql = "SELECT id FROM students";

    try (Connection con = sql2o.open()) {
        return con.createQuery(sql).executeScalarList(Integer.class);
    }   
}
{% endhighlight %}


### Retrieve Result from Arbitrary Query

Often times in enterprise reporting we deal with very complex SQL query that spans multiple joins as well as aggregation functions. In such cases it may not be possible to map it to a single Java Model. It is desirable to have the database result returned as a List of Map object. Every element of List in this case is a Map that represents a virtual row of record. 

<div class="alert alert-info">NOTE: This is available in SNAPSHOT only and will be released with Sql2o 1.5.0 version.</div>

{% highlight java %}
public List<Map<String,Object>> getReportData(){
    String complexSql = "...";

    try (Connection con = sql2o.open()) {
        return con.createQuery(complexSql).executeAndFetchTable().asList();
    }
}
{% endhighlight %}
