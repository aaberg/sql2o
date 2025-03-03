# sql2o  &#x20;

Sql2o is a lightweight Java library designed to simplify database interaction. It automatically maps query results into POJO objects, providing an easy-to-use alternative to ORMs, without SQL generation capabilities.

Sql2o is compatible with **Java 8 and later versions**, including Java 11 and 17.

## Announcements

- *2024-03-12* | [Sql2o 1.7.0 was released](https://github.com/aaberg/sql2o/discussions/365)

## Quick Start Example

Here's a basic example demonstrating how to use Sql2o to interact with a database:

```java
import org.sql2o.*;

public class Main {
    public static void main(String[] args) {
        String url = "jdbc:h2:mem:test"; // Example using H2 in-memory database
        try (Sql2o sql2o = new Sql2o(url, "username", "password");
             Connection con = sql2o.open()) {
            
            con.createQuery("CREATE TABLE users (id INTEGER PRIMARY KEY, name VARCHAR(50))").executeUpdate();
            con.createQuery("INSERT INTO users (id, name) VALUES (:id, :name)")
                .addParameter("id", 1)
                .addParameter("name", "Alice")
                .executeUpdate();
            
            User user = con.createQuery("SELECT * FROM users WHERE id = :id")
                            .addParameter("id", 1)
                            .executeAndFetchFirst(User.class);
            
            System.out.println("User: " + user.name);
        }
    }
}

class User {
    public int id;
    public String name;
}
```

## Coding Guidelines

When contributing to Sql2o, please follow [these coding guidelines](https://github.com/aaberg/sql2o/wiki/Coding-guidelines).

## Documentation

For more details and examples, visit the [Sql2o GitHub repository](https://github.com/aaberg/sql2o).

## Wiki

For additional information, check out the [Sql2o Wiki](https://github.com/aaberg/sql2o/wiki).

