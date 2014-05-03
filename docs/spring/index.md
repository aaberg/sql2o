---
title: Integration With Spring Framework
layout: docs
topmenu: docs
leftmenu: spring
---

Sql2o instance is thread-safe. This means that Sql2o can be configured as a singleton component in Spring runtime. Here are few steps in order to use Sql2o with Spring.


- Configure Sql2o in Spring context. There are multiple ways to create DataSource object. The method shown below is chosen for it's simplicity.

{% highlight java %}
<bean id="mysqlDS" class="org.apache.commons.dbcp.BasicDataSource">
	<property name="driverClassName">com.mysql.jdbc.Driver</property>
	<property name="url">jdbc:mysql://localhost:3306/testDB</property>
	<property name="username">user</property>
	<property name="password">pass</property>
</bean>
<bean id="sql2o" class="org.sql2o.Sql2o">
	<property name="dataSource" ref="mysqlDS"></property>
</bean>
{% endhighlight %}

- Define Model Class

{% highlight java %}
package org.sql2o.domain;

public class Student{
	private int studentId;
	private String firstName;
	private String lastName;
	...
}
{% endhighlight %}

- Define Spring DAO contract

{% highlight java %}
package org.sql2o.test;

import org.sql2o.domain.Student;

public interface TestRepository {
	
	public int getStudentCount();
	public Student getStudent(int studentId);
}
{% endhighlight %}

- Autowire Sql2o in Spring DAO Layer

{% highlight java %}
package org.sql2o.test.impl;

import org.sql2o.Sql2o;
import org.sql2o.domain.Student;
import org.sql2o.test.TestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class TestRepositoryImpl implements TestRepository {
	@Autowired
	private Sql2o sql2o;
	
	@Override
	public int getStudentCount(){
		String sql = "SELECT count(id) FROM students";
                
		try (Connection con = sql2o.open()) {
			return con.createQuery(sql).executeScalar(Integer.class);
		}
	}
	
	@Override
	public Student getStudent(int studentId){
		String sql = "SELECT * FROM students where id=:id";

		try (Connection con = sql2o.open()) {
			return con.createQuery(sql)
				.addParameter("id", studentId)
				.executeAndFetchFirst(Student.class);;
		}
	}
}
{% endhighlight %}

And That's it!!

Sql2o is very simple and high-performance alternative to Spring JdbcTemplate, JPA and many other ORM frameworks. As shown above it also gels perfectly with one of the most popular dependency injection framework.
