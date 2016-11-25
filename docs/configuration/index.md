---
title: Configuration
layout: docs
topmenu: docs
leftmenu: configuration
base_url: ../../
---

The only configuration needed is the connection url, username and password for your database. This information is
specified in the constructor of the Sql2o class. In the example below, sql2o is set up to connect to a local mysql database.

{% highlight java %}
Sql2o sql2o = new Sql2o("jdbc:mysql://localhost:3306/myDB", "myUsername", "topSecretPassword");
{% endhighlight %}

Alternatively you can specify a DataSource.

{% highlight java %}
Sql2o sql2o = new Sql2o(myDataSource);
{% endhighlight %}

This is typically information you only want to specify once, and not for every time you access the database. So if you
access your database from a DAO class, it is a good idea to initialize your sql2o instance in the constructor of your DAO.
This is illustrated in the example below.

{% highlight java %}
public class myDao {

    private Sql2o sql2o;

    public myDao() {
        this.sql2o = new Sql2o("jdbc:mysql://localhost:3306/myDB", "myUsername", "topSecretPassword");
    }
}
{% endhighlight %}

Another pattern is to make the sql2o instance static and initialize it in the static constructor of the dao. This is
the preferred pattern, if your are developing a website, and you create a new instance of your dao for every http
request. This way, your sql2o instance is only created once, when the application launches. This is illustrated in the
example below.

{% highlight java %}
public class MyDao {

    private static Sql2o sql2o;

    static{
        sql2o = new Sql2o("jdbc:mysql://localhost:3306/myDB", "myUsername", "topSecretPassword");
    }
}
{% endhighlight %}
