package org.sql2o.converters;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.sql2o.Connection;
import org.sql2o.Query;
import org.sql2o.Sql2o;
import org.sql2o.quirks.NoQuirks;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * {@code LocalDateTimeConverterTest} is a JUnit test class designed to validate the functionality
 * of the custom converter {@code MockLocalDateTimeConverter} within the context of Sql2o.
 * The class includes tests for inserting and retrieving LocalDateTime instances from a database
 * using the converter, as well as ensuring proper handling of conversion exceptions.
 *
 * <p>The tests cover various scenarios, such as creating tables, inserting initial data, and verifying
 * the expected behavior when working with LocalDateTime instances through Sql2o.</p>
 *
 * <p>Note: This class assumes the existence of the {@code MockLocalDateTime} class and the corresponding
 * converter {@code MockLocalDateTimeConverter} for testing purposes.</p>
 *
 * @since 13/2/2024
 * @author Agit Rubar Demir | @agitrubard
 * @version 1.8.0
 */
public class LocalDateTimeConverterTest {

    private Sql2o sql2o;

    @Before
    public void setUp() {

        NoQuirks noQuirks = new NoQuirks() {
            {
                this.converters.put(MockLocalDateTime.class, new MockLocalDateTimeConverter());
            }
        };
        this.sql2o = new Sql2o(
            "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1",
            "sa",
            "",
            noQuirks
        );

        this.createTable();
        this.insertInitialData();
    }

    private void createTable() {
        String sqlScriptOfCreateTable = "create table local_date_time_converter(created_at timestamp(0))";
        try (Connection connection = sql2o.open(); Query query = connection.createQuery(sqlScriptOfCreateTable)) {

            query.executeUpdate();
        }
    }

    private void insertInitialData() {
        String sqlScriptOfInsertData = "insert into local_date_time_converter(created_at) values (:created_at)";
        try (Connection connection = sql2o.open(); Query query = connection.createQuery(sqlScriptOfInsertData)) {

            query.addParameter("created_at", LocalDateTime.now());
            query.executeUpdate();
        }
    }


    @After
    public void tearDown() {
        this.dropTable();
    }

    private void dropTable() {
        String sqlScriptOfDropTable = "drop table local_date_time_converter";
        try (Connection connection = sql2o.open(); Query query = connection.createQuery(sqlScriptOfDropTable)) {

            query.executeUpdate();
        }
    }


    @Test
    public void whenFindAllLocalDateTimes_thenTableHas1InitialRecordsOfLocalDateTimeInstance() {

        // When
        List<LocalDateTime> localDateTimes;
        String sqlScriptOfFindAll = "select created_at from local_date_time_converter";
        try (Connection connection = sql2o.open(); Query query = connection.createQuery(sqlScriptOfFindAll)) {

            List<MockLocalDateTime> mockLocalDateTimes = query.executeAndFetch(MockLocalDateTime.class);
            localDateTimes = mockLocalDateTimes.stream()
                .map(MockLocalDateTime::getLocalDateTime)
                .collect(Collectors.toList());
        }

        // Then
        Assert.assertNotNull(localDateTimes);
        Assert.assertEquals(1, localDateTimes.size());
        Assert.assertEquals(LocalDateTime.class, localDateTimes.get(0).getClass());
    }


    @Test
    public void givenValidLocalDateTime_whenLocalDateTimeSavedSuccessfully_thenTableHas2RecordsOfLocalDateTimeInstance() {

        // Given
        LocalDateTime localDateTime = LocalDateTime.now();
        String sqlScriptOfInsertData = "insert into local_date_time_converter(created_at) values (:created_at)";
        try (Connection connection = sql2o.open(); Query query = connection.createQuery(sqlScriptOfInsertData)) {
            query
                .addParameter("created_at", localDateTime)
                .executeUpdate();
        }

        // When
        List<LocalDateTime> localDateTimes;
        String sqlScriptOfFindAll = "select created_at from local_date_time_converter";
        try (Connection connection = sql2o.open(); Query query = connection.createQuery(sqlScriptOfFindAll)) {

            List<MockLocalDateTime> mockLocalDateTimes = query.executeAndFetch(MockLocalDateTime.class);

            localDateTimes = mockLocalDateTimes.stream()
                .map(MockLocalDateTime::getLocalDateTime)
                .collect(Collectors.toList());
        }

        // Then
        Assert.assertNotNull(localDateTimes);
        Assert.assertEquals(2, localDateTimes.size());
        Assert.assertEquals(LocalDateTime.class, localDateTimes.get(0).getClass());
        Assert.assertEquals(LocalDateTime.class, localDateTimes.get(1).getClass());
    }


    private static class MockLocalDateTime {

        private LocalDateTime localDateTime;

        @SuppressWarnings("It's seems unused but It's used by sql2o internally.")
        public MockLocalDateTime() {
        }

        public MockLocalDateTime(LocalDateTime localDateTime) {
            this.localDateTime = localDateTime;
        }

        public LocalDateTime getLocalDateTime() {
            return localDateTime;
        }

    }

    private static class MockLocalDateTimeConverter implements Converter<MockLocalDateTime> {

        @Override
        public MockLocalDateTime convert(Object dateObject) throws ConverterException {

            if (dateObject == null) {
                return null;
            }

            try {
                LocalDateTime localDateTime = Timestamp.valueOf(dateObject.toString()).toLocalDateTime();
                return new MockLocalDateTime(localDateTime);
            } catch (IllegalArgumentException exception) {
                String dateObjectClassName = dateObject.getClass().getName();
                String localDateTimeClassName = LocalDateTime.class.getName();
                throw new ConverterException(
                    String.format("Don't know how to convert from type '%s' to type '%s'", dateObjectClassName, localDateTimeClassName),
                    exception
                );
            }
        }

        @Override
        public Object toDatabaseParam(MockLocalDateTime mockLocalDateTime) {
            LocalDateTime localDateTime = mockLocalDateTime.getLocalDateTime();
            return Timestamp.valueOf(localDateTime);
        }

    }

}
