package org.sql2o.converters;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.sql2o.Connection;
import org.sql2o.Query;
import org.sql2o.Sql2o;
import org.sql2o.quirks.NoQuirks;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * {@code LocalDateConverterTest} is a JUnit test class for validating the functionality
 * of the custom converter {@code MockLocalDateConverter} within the context of Sql2o.
 * This class includes tests for inserting and retrieving LocalDate instances from a database
 * using the converter, as well as ensuring the proper handling of conversion exceptions.
 *
 * <p>The tests cover scenarios such as creating tables, inserting initial data, and verifying
 * the expected behavior when working with LocalDate instances through Sql2o.</p>
 *
 * <p>Note: This class assumes the existence of the {@code MockLocalDate} class and the
 * corresponding converter {@code MockLocalDateConverter} for testing purposes.</p>
 *
 * @since 13/2/2024
 * @author Agit Rubar Demir | @agitrubard
 * @version 1.8.0
 */
public class LocalDateConverterTest {

    private Sql2o sql2o;

    @Before
    public void setUp() {

        NoQuirks noQuirks = new NoQuirks() {
            {
                this.converters.put(MockLocalDate.class, new MockLocalDateConverter());
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
        String sqlScriptOfCreateTable = "create table local_date_converter(created_at date)";
        try (Connection connection = sql2o.open(); Query query = connection.createQuery(sqlScriptOfCreateTable)) {

            query.executeUpdate();
        }
    }

    private void insertInitialData() {
        String sqlScriptOfInsertData = "insert into local_date_converter(created_at) values (:created_at)";
        try (Connection connection = sql2o.open(); Query query = connection.createQuery(sqlScriptOfInsertData)) {

            query.addParameter("created_at", LocalDate.now());
            query.executeUpdate();
        }
    }


    @After
    public void tearDown() {
        this.dropTable();
    }

    private void dropTable() {
        String sqlScriptOfDropTable = "drop table local_date_converter";
        try (Connection connection = sql2o.open(); Query query = connection.createQuery(sqlScriptOfDropTable)) {

            query.executeUpdate();
        }
    }


    @Test
    public void whenFindAllLocalDates_thenTableHas1InitialRecordsOfLocalDateInstance() {

        // When
        List<LocalDate> localDates;
        String sqlScriptOfFindAll = "select created_at from local_date_converter";
        try (Connection connection = sql2o.open(); Query query = connection.createQuery(sqlScriptOfFindAll)) {

            List<MockLocalDate> mockLocalDates = query.executeAndFetch(MockLocalDate.class);
            localDates = mockLocalDates.stream()
                .map(MockLocalDate::getLocalDate)
                .collect(Collectors.toList());
        }

        // Then
        Assert.assertNotNull(localDates);
        Assert.assertEquals(1, localDates.size());
        Assert.assertEquals(LocalDate.class, localDates.get(0).getClass());
    }


    @Test
    public void givenValidLocalDate_whenLocalDateSavedSuccessfully_thenTableHas2RecordsOfLocalDateInstance() {

        // Given
        LocalDate localDate = LocalDate.now();
        String sqlScriptOfInsertData = "insert into local_date_converter(created_at) values (:created_at)";
        try (Connection connection = sql2o.open(); Query query = connection.createQuery(sqlScriptOfInsertData)) {
            query
                .addParameter("created_at", localDate)
                .executeUpdate();
        }

        // When
        List<LocalDate> localDates;
        String sqlScriptOfFindAll = "select created_at from local_date_converter";
        try (Connection connection = sql2o.open(); Query query = connection.createQuery(sqlScriptOfFindAll)) {

            List<MockLocalDate> mockLocalDates = query.executeAndFetch(MockLocalDate.class);

            localDates = mockLocalDates.stream()
                .map(MockLocalDate::getLocalDate)
                .collect(Collectors.toList());
        }

        // Then
        Assert.assertNotNull(localDates);
        Assert.assertEquals(2, localDates.size());
        Assert.assertEquals(LocalDate.class, localDates.get(0).getClass());
        Assert.assertEquals(LocalDate.class, localDates.get(1).getClass());
    }


    private static class MockLocalDate {

        private LocalDate localDate;

        @SuppressWarnings("It's seems unused but It's used by sql2o internally.")
        public MockLocalDate() {
        }

        public MockLocalDate(LocalDate localDate) {
            this.localDate = localDate;
        }

        public LocalDate getLocalDate() {
            return localDate;
        }

    }

    private static class MockLocalDateConverter implements Converter<MockLocalDate> {

        @Override
        public MockLocalDate convert(Object dateObject) throws ConverterException {

            if (dateObject == null) {
                return null;
            }

            try {
                LocalDate localDate = LocalDate.parse(dateObject.toString().split(" ")[0]);
                return new MockLocalDate(localDate);
            } catch (IllegalArgumentException exception) {
                String dateObjectClassName = dateObject.getClass().getName();
                String localDateClassName = LocalDate.class.getName();
                throw new ConverterException(
                    String.format("Don't know how to convert from type '%s' to type '%s'", dateObjectClassName, localDateClassName),
                    exception
                );
            }
        }

        @Override
        public Object toDatabaseParam(MockLocalDate MockLocalDate) {
            LocalDate localDate = MockLocalDate.getLocalDate();
            return Date.valueOf(localDate);
        }

    }

}
