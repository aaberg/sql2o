package org.sql2o;

import org.junit.jupiter.api.Test;
import org.sql2o.quirks.NoQuirks;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link DefaultResultSetHandlerFactoryBuilder}, the settings holder behind every pojo mapping.
 *
 * <p>Note that column mappings have no default, which is the one piece of configuration a caller must supply.
 */
public class DefaultResultSetHandlerFactoryBuilderTest {

    public static class Point {

        private int x;

        public int getX() {
            return x;
        }

        public void setX(int x) {
            this.x = x;
        }
    }

    /** Nothing to build from: the builder needs a no-arg constructor. */
    public static class NoDefaultConstructor {

        public NoDefaultConstructor(int x) {
            // nothing
        }
    }

    private static ResultSetMetaData metaWithOneColumn() throws SQLException {
        final ResultSetMetaData meta = mock(ResultSetMetaData.class);
        when(meta.getColumnCount()).thenReturn(1);
        when(meta.getColumnLabel(1)).thenReturn("x");
        return meta;
    }

    private static DefaultResultSetHandlerFactoryBuilder builder() {
        final DefaultResultSetHandlerFactoryBuilder builder = new DefaultResultSetHandlerFactoryBuilder();
        builder.setQuirks(new NoQuirks());
        return builder;
    }

    @Test
    public void everySettingIsRemembered() {
        final DefaultResultSetHandlerFactoryBuilder builder = new DefaultResultSetHandlerFactoryBuilder();
        final Map<String, String> mappings = Map.of("column", "property");
        final NoQuirks quirks = new NoQuirks();

        assertFalse(builder.isCaseSensitive());
        assertFalse(builder.isAutoDeriveColumnNames());
        assertFalse(builder.isThrowOnMappingError());
        assertEquals(Map.of(), builder.getColumnMappings());
        assertNull(builder.getQuirks());

        builder.setCaseSensitive(true);
        builder.setAutoDeriveColumnNames(true);
        builder.throwOnMappingError(true);
        builder.setColumnMappings(mappings);
        builder.setQuirks(quirks);

        assertTrue(builder.isCaseSensitive());
        assertTrue(builder.isAutoDeriveColumnNames());
        assertTrue(builder.isThrowOnMappingError());
        assertSame(mappings, builder.getColumnMappings());
        assertSame(quirks, builder.getQuirks());
    }

    @Test
    public void aFactoryFromTheBuilderFillsAnObject() throws Exception {
        final ResultSet rs = mock(ResultSet.class);
        when(rs.getObject(1)).thenReturn(5);

        final Point point = builder().<Point>newFactory(Point.class).newResultSetHandler(metaWithOneColumn())
                .handle(rs);

        assertEquals(5, point.getX());
    }

    /** Column mappings are what a caller supplies to rename a column onto a differently named property. */
    @Test
    public void aColumnMappingIsAppliedWhenTheObjectIsBuilt() throws Exception {
        final ResultSetMetaData meta = mock(ResultSetMetaData.class);
        when(meta.getColumnCount()).thenReturn(1);
        when(meta.getColumnLabel(1)).thenReturn("COLUMN");
        final ResultSet rs = mock(ResultSet.class);
        when(rs.getObject(1)).thenReturn(9);

        final DefaultResultSetHandlerFactoryBuilder builder = new DefaultResultSetHandlerFactoryBuilder();
        builder.setQuirks(new NoQuirks());
        builder.setCaseSensitive(true);
        builder.setColumnMappings(Map.of("COLUMN", "x"));

        final Point point = builder.<Point>newFactory(Point.class).newResultSetHandler(meta).handle(rs);

        assertEquals(9, point.getX());
    }

/**
 * Column mappings are optional. A builder that was never given any used to blow up with a bare
 * NullPointerException out of the first row, because the property lookup dereferenced the map without a check.
 */
@Test
    public void aBuilderWithoutColumnMappingsStillFillsAnObject() throws Exception {
        final DefaultResultSetHandlerFactoryBuilder builder = new DefaultResultSetHandlerFactoryBuilder();
        builder.setQuirks(new NoQuirks());
        assertEquals(Map.of(), builder.getColumnMappings());

        final ResultSet rs = mock(ResultSet.class);
        when(rs.getObject(1)).thenReturn(5);

        final Point point = builder.<Point>newFactory(Point.class).newResultSetHandler(metaWithOneColumn())
                .handle(rs);

        assertEquals(5, point.getX());
    }

    /**
     * The builder hands out a lazy delegate, so an unusable class is only noticed when the first row is read, not when
     * the factory is asked for.
     *
     * <p>The message the builder intends to produce never reaches the caller at all. ObjectBuildableFactory builds its
     * metadata through a Cache, and Cache wraps anything its delegate throws, so the NoSuchMethodException gets wrapped
     * first and the builder's own catch (ReflectiveOperationException) never runs. What surfaces is the cache message
     * with the reflection failure as its cause.
     */
    @Test
    public void aClassThatCannotBeInstantiatedIsReportedWhenTheFirstRowArrives() throws Exception {
        final ResultSetHandler<NoDefaultConstructor> handler =
                builder().<NoDefaultConstructor>newFactory(NoDefaultConstructor.class)
                        .newResultSetHandler(metaWithOneColumn());

        final ResultSet rs = mock(ResultSet.class);
        when(rs.getObject(1)).thenReturn(1);

        final RuntimeException ex = assertThrows(RuntimeException.class, () -> handler.handle(rs));

        assertEquals("Error while getting value from cache", ex.getMessage());
        assertInstanceOf(NoSuchMethodException.class, ex.getCause());
    }
}