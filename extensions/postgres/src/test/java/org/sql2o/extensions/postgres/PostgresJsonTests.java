package org.sql2o.extensions.postgres;

import com.google.gson.Gson;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.postgresql.util.PGobject;
import org.sql2o.Connection;
import org.sql2o.extensions.postgres.converters.JSONConverter;

import java.sql.SQLException;

@RunWith(Parameterized.class)
public class PostgresJsonTests extends PostgresTestSupport {

    public PostgresJsonTests(String url, String user, String pass, String testName) {
        super(url, user, pass, testName);
    }

    @Test
    public void whenColumnIsJson_thenDataInsertedIsSerialized() throws SQLException {

        // given
        try (Connection connection = sql2o.open()) {
            connection.createQuery("create table test_json_table(id SERIAL, val jsonb)").executeUpdate();
        }

        // when
        try (Connection connection = sql2o.open()) {

            JsonTestPojo data = new JsonTestPojo("John", 30);
            String jsonVal = new Gson().toJson(data);

            PGobject jsonObject = new PGobject();
            jsonObject.setType("jsonb");
            jsonObject.setValue(jsonVal);

            connection.createQuery("insert into test_json_table (val) values(:val)")
                      .addParameter("val", jsonObject)
                      .executeUpdate();
        }

        try (Connection connection = sql2o.open()) {
            connection.createQuery("drop table test_json_table;").executeUpdate();
        }
    }
}
