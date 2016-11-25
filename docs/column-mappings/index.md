---
title: Column mappings
layout: docs
topmenu: docs
leftmenu: columnmappings
base_url: ../../
---

In many many cases it is not desired to call our java properties exact the same name as the corresponding column in the
database table. The database designers would probably have called dueDate for DUE_DATE. After all, having a getter
called getDue_date() is pretty ugly. The easiest solution for this, is to use aliases in sql queries as illustrated in
the following example.

{% highlight java %}
public List<Task> getTasksBetweenDates(Date fromDate, Date toDate){
    String sql =
        "SELECT id, description, due_date duedate " +
        "FROM tasks " +
        "WHERE duedate > :fromDate AND duedate < :toDate";

    try (Connection con = sql2o.open()) {
        return con.createQuery(sql)
            .addParameter("fromDate", fromDate)
            .addParameter("toDate", toDate)
            .executeAndFetch(Task.class);
    }
    
}
{% endhighlight %}

Another approach is to add column mappings to the sql2o query. This can be accomplished by calling the addColumnMapping
method as illustrated below.

{% highlight java %}
public List<Task> getTasksBetweenDates(Date fromDate, Date toDate){
    String sql =
        "SELECT id, description, due_date " +
        "FROM tasks " +
        "WHERE duedate > :fromDate AND duedate < :toDate";

    try (Connection con = sql2o.open()) {
        return con.createQuery(sql)
            .addParameter("fromDate", fromDate).addParameter("toDate", toDate)
            .addColumnMapping("DUE_DATE", "dueDate")
            .executeAndFetch(Task.class);
    }
}
{% endhighlight %}

If you have some common column names, that needs to be mapped in many of your sql queries, it is also possible to add
default column mappings to the Sql2o instance. These column mappings will automatically be applied to all queries.

{% highlight java %}
Map<String, String> colMaps = new HashMap<String,String>();
colMaps.put("DUE_DATE", "dueDate");
colMaps.put("DESC", "description");
colMaps.put("E_MAIL", "email");
colMaps.put("SHORT_DESC", "shortDescription");

sql2o.setDefaultColumnMappings(colMaps);
{% endhighlight %}
