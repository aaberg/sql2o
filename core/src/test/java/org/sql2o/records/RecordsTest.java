package org.sql2o.records;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.sql2o.H2ArgumentsSourceProvider;
import org.sql2o.Sql2o;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RecordsTest {

    @ParameterizedTest(name = "{0}")
    @ArgumentsSource(H2ArgumentsSourceProvider.class)
    void deserializing_a_record_with_constructor_should_work(String dbName, String url, String user, String pass) {
        // Arrange
        final var sql2o = new Sql2o(url, user, pass);
        createATableWithSomeData(sql2o);

        // Act
        List<RecordEntity> recordResult;
        try (final var con = sql2o.open()) {
            recordResult = con.createQuery("SELECT id, name, is_ok as \"isOk\" FROM record_entity")
                    .executeAndFetch(RecordEntity.class);
        }

        // Assert
        assertEquals(2, recordResult.size());
        final var firstRecord = recordResult.get(0);
        assertEquals(1, firstRecord.id());
        assertEquals("name1", firstRecord.name());
        assertTrue(firstRecord.isOk());
    }

    private void createATableWithSomeData(Sql2o sql2o){
        try (var con = sql2o.open()) {
            con.createQuery("CREATE TABLE record_entity (id INT PRIMARY KEY, name VARCHAR(255), is_ok BOOLEAN)")
                    .executeUpdate();
            con.createQuery("INSERT INTO record_entity (id, name, is_ok) VALUES (:id, :name, :is_ok)")
                    .addParameter("id", 1)
                    .addParameter("name", "name1")
                    .addParameter("is_ok", true)
                    .executeUpdate();
            con.createQuery("INSERT INTO record_entity (id, name, is_ok) VALUES (:id, :name, :is_ok)")
                    .addParameter("id", 2)
                    .addParameter("name", "name2")
                    .addParameter("is_ok", false)
                    .executeUpdate();
        }

    }

    public record RecordEntity(int id, String name, boolean isOk) {}
}
