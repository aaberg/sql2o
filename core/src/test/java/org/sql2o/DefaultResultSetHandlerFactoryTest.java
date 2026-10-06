package org.sql2o;

import org.junit.jupiter.api.Test;
import org.sql2o.quirks.NoQuirks;
import org.sql2o.reflection2.ObjectBuildable;
import org.sql2o.reflection2.ObjectBuildableFactoryDelegate;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link DefaultResultSetHandlerFactory}, which turns a result set row into an object.
 *
 * <p>Both failure paths matter: a column that cannot be put into the object and an object that cannot be built.
 */
public class DefaultResultSetHandlerFactoryTest {

    public static class Thing {

        private String name;
        private int size;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getSize() {
            return size;
        }

        public void setSize(int size) {
            this.size = size;
        }
    }

    /** An object builder that records what it was given and can fail on demand. */
    private static class RecordingBuildable implements ObjectBuildable<Thing> {

        private final boolean failOnWithValue;
        private final boolean failOnBuild;
        private final Map<String, Object> received = new LinkedHashMap<>();

        private RecordingBuildable(boolean failOnWithValue, boolean failOnBuild) {
            this.failOnWithValue = failOnWithValue;
            this.failOnBuild = failOnBuild;
        }

        @Override
        public void withValue(String columnName, Object value) throws ReflectiveOperationException {
            if (failOnWithValue) {
                throw new NoSuchMethodException("no such setter");
            }
            received.put(columnName, value);
        }

        @Override
        public Thing build() throws ReflectiveOperationException {
            if (failOnBuild) {
                throw new NoSuchMethodException("no such constructor");
            }
            return new Thing();
        }
    }

    private static ResultSetMetaData metaWithTwoColumns() throws SQLException {
        final ResultSetMetaData meta = mock(ResultSetMetaData.class);
        when(meta.getColumnCount()).thenReturn(2);
        when(meta.getColumnLabel(1)).thenReturn("name");
        when(meta.getColumnLabel(2)).thenReturn("size");
        return meta;
    }

    private static ResultSet resultSetWith(String name, int size) throws SQLException {
        final ResultSet rs = mock(ResultSet.class);
        when(rs.getObject(1)).thenReturn(name);
        when(rs.getObject(2)).thenReturn(size);
        return rs;
    }

    private static ResultSetHandler<Thing> handlerFor(ObjectBuildable<Thing> buildable) throws SQLException {
        return new DefaultResultSetHandlerFactory<Thing>(
                (ObjectBuildableFactoryDelegate<Thing>) () -> buildable, new NoQuirks())
                .newResultSetHandler(metaWithTwoColumns());
    }

    @Test
    public void everyColumnIsOfferedToTheObjectBuilderUnderItsLabel() throws Exception {
        final RecordingBuildable buildable = new RecordingBuildable(false, false);

        handlerFor(buildable).handle(resultSetWith("widget", 7));

        assertEquals(Map.of("name", "widget", "size", 7), buildable.received);
    }

    @Test
    public void aColumnThatCannotBeSetIsReportedWithItsName() throws Exception {
        final ResultSetHandler<Thing> handler = handlerFor(new RecordingBuildable(true, false));

        final Sql2oException ex = assertThrows(Sql2oException.class,
                () -> handler.handle(resultSetWith("widget", 7)));

        assertEquals("Error when trying to set value for column [name]", ex.getMessage());
        assertTrue(ex.getCause() instanceof ReflectiveOperationException);
    }

    @Test
    public void anObjectThatCannotBeBuiltIsReported() throws Exception {
        final ResultSetHandler<Thing> handler = handlerFor(new RecordingBuildable(false, true));

        final Sql2oException ex = assertThrows(Sql2oException.class,
                () -> handler.handle(resultSetWith("widget", 7)));

        assertEquals("Error occurred while creating object from ResultSet", ex.getMessage());
        assertTrue(ex.getCause() instanceof ReflectiveOperationException);
    }

    /**
 * The names of the columns are a property of the result set and not of the row, so they are read once when the
 * handler is built. This used to be a metadata call per column per row, which for a thousand rows of two columns was
 * two thousand calls where two would do.
 */
@Test
    public void theColumnNamesAreReadOnceWhenTheHandlerIsBuilt() throws Exception {
        final ResultSetMetaData meta = mock(ResultSetMetaData.class);
        when(meta.getColumnCount()).thenReturn(2);
        when(meta.getColumnLabel(1)).thenReturn("name");
        when(meta.getColumnLabel(2)).thenReturn("size");

        final RecordingBuildable buildable = new RecordingBuildable(false, false);
        final ResultSetHandler<Thing> handler = new DefaultResultSetHandlerFactory<Thing>(
                (ObjectBuildableFactoryDelegate<Thing>) () -> buildable, new NoQuirks())
                .newResultSetHandler(meta);

        handler.handle(resultSetWith("widget", 7));
        handler.handle(resultSetWith("gadget", 9));

        verify(meta, times(1)).getColumnLabel(1);
        verify(meta, times(1)).getColumnLabel(2);
        verify(meta, times(1)).getColumnCount();
        // Both rows still arrived, so reading the names early did not skip anything.
        assertEquals(Map.of("name", "gadget", "size", 9), buildable.received);
    }

    /** The happy path, using the real reflection machinery rather than a stand-in. */
    @Test
    public void aRealObjectIsFilledFromTheRow() throws Exception {
        final var builder = new DefaultResultSetHandlerFactoryBuilder();
        builder.setQuirks(new NoQuirks());
        builder.setColumnMappings(Map.of());
        final ResultSetHandler<Thing> handler = builder.<Thing>newFactory(Thing.class)
                .newResultSetHandler(metaWithTwoColumns());

        final Thing thing = handler.handle(resultSetWith("widget", 7));

        assertEquals("widget", thing.getName());
        assertEquals(7, thing.getSize());
    }

/**
 * Column mappings are optional, so a builder that was never given any has to work: an empty map is the default,
 * rather than a null the property lookup would trip over.
 */
@Test
    public void aBuilderWithoutColumnMappingsStillFillsAnObject() throws Exception {
        final var builder = new DefaultResultSetHandlerFactoryBuilder();
        builder.setQuirks(new NoQuirks());

        final ResultSetHandler<Thing> handler = builder.<Thing>newFactory(Thing.class)
                .newResultSetHandler(metaWithTwoColumns());

        final Thing thing = handler.handle(resultSetWith("widget", 7));

        assertEquals("widget", thing.getName());
        assertEquals(7, thing.getSize());
    }
}