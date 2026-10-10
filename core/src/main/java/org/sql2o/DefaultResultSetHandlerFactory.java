package org.sql2o;

import org.sql2o.converters.Converter;
import org.sql2o.converters.ConverterException;
import org.sql2o.quirks.Quirks;
import org.sql2o.reflection2.ObjectBuildableFactory;
import org.sql2o.reflection2.ObjectBuildableFactoryDelegate;
import org.sql2o.reflection2.PojoMetadata;
import org.sql2o.reflection2.PojoProperty;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.lang.reflect.Constructor;
import java.lang.reflect.RecordComponent;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

public class DefaultResultSetHandlerFactory<T> implements ResultSetHandlerFactory<T> {
    private final Quirks quirks;
    private final ObjectBuildableFactoryDelegate<T> objectBuilderDelegate;
    private final Class<T> clazz;
    private final Settings settings;
    private final Supplier<Map<String, String>> columnMappings;

    public DefaultResultSetHandlerFactory(ObjectBuildableFactoryDelegate<T> objectBuilderDelegate, Quirks quirks) {
        this(objectBuilderDelegate, quirks, null, null, null);
    }

    /**
     * @param clazz the mapped class, or null when it is not known. With the class and the settings the handler
     *              resolves every column once, on the first row, instead of looking the property and the converter up
     *              on every row; without them every row goes through the delegate exactly as before.
     */
    public DefaultResultSetHandlerFactory(ObjectBuildableFactoryDelegate<T> objectBuilderDelegate, Quirks quirks,
                                          Class<T> clazz, Settings settings,
                                          Supplier<Map<String, String>> columnMappings) {
        this.objectBuilderDelegate = objectBuilderDelegate;
        this.quirks = quirks;
        this.clazz = clazz;
        this.settings = settings;
        this.columnMappings = columnMappings;
    }


    @SuppressWarnings("unchecked")
    public ResultSetHandler<T> newResultSetHandler(final ResultSetMetaData meta) throws SQLException {
        // The names of the columns are a property of the result set and not of the row being read, so they are asked
        // for once here rather than on every row, where this used to make a metadata call per column per row.
        final int columnCount = meta.getColumnCount();
        final String[] columnNames = new String[columnCount];
        for (int i = 0; i < columnCount; i++) {
            columnNames[i] = quirks.getColumnName(meta, i + 1);
        }

        if (clazz == null) {
            return legacyHandler(columnNames, columnCount);
        }
        return new PlannedHandler(columnNames, columnCount);
    }

    private ResultSetHandler<T> legacyHandler(final String[] columnNames, final int columnCount) {
        return resultSet -> {

            final var objectBuilder = objectBuilderDelegate.newObjectBuilder();

            for (int i = 0; i < columnCount; i++) {
                final var colName = columnNames[i];
                try {
                    objectBuilder.withValue(colName, quirks.getRSVal(resultSet, i + 1));
                } catch (ReflectiveOperationException e) {
                    throw new Sql2oException("Error when trying to set value for column [" + colName + "]", e);
                }

            }
            try {
                return objectBuilder.build();
            } catch (ReflectiveOperationException e) {
                throw new Sql2oException("Error occurred while creating object from ResultSet", e);
            }
        };
    }

    /**
     * The same mapping, with the per-row resolution hoisted to the first row.
     *
     * <p>Every column of a result set maps the same way on every row — the metadata does not change between rows — so
     * deriving the name, looking the property up and asking the quirks for a converter is work worth doing once. What
     * is deferred matters: a missing property still fails on the row, with the same message, and anything the
     * resolution itself refuses — a dotted column, a record, a converter the quirks cannot give — hands the whole
     * result set back to the delegate, which maps it exactly as before.
     */
    private final class PlannedHandler implements ResultSetHandler<T> {
        private final String[] columnNames;
        private final int columnCount;
        private volatile boolean planned;
        private volatile boolean legacy;
        private volatile Slot[] slots;
        private volatile PojoMetadata<T> metadata;
        private volatile RecordSlot[] recordSlots;
        private volatile Constructor<T> recordConstructor;
        private volatile int recordArity;

        PlannedHandler(String[] columnNames, int columnCount) {
            this.columnNames = columnNames;
            this.columnCount = columnCount;
        }

        @Override
        public T handle(ResultSet resultSet) throws SQLException {
            if (!planned) {
                planFirstRow();
                planned = true;
            }
            if (legacy) {
                return legacyRow(resultSet);
            }
            if (recordSlots != null) {
                return plannedRecordRow(resultSet);
            }
            return plannedRow(resultSet, slots);
        }

        /**
         * Decides, on the first row, how this result set is mapped. Everything resolution-like happens here and only
         * here, so the first row fails exactly as it did when the same work happened on every row — and a result set
         * the planning cannot describe goes to the delegate, which maps it exactly as before. Records and dotted
         * columns take their own branches; see below.
         */
        private void planFirstRow() {
            if (clazz.isRecord()) {
                final RecordComponent[] components = clazz.getRecordComponents();
                // Resolved like RecordBuilder resolves them: derived names into a map, so a duplicate derived name
                // keeps the last component exactly as the builder's puts do.
                final NamingConvention convention = settings.getNamingConvention();
                final Map<String, Integer> indexByName = new LinkedHashMap<>();
                for (int j = 0; j < components.length; j++) {
                    indexByName.put(convention.deriveName(components[j].getName()), j);
                }
                @SuppressWarnings("unchecked")
                final Constructor<T> constructor = (Constructor<T>) clazz.getDeclaredConstructors()[0];
                final RecordSlot[] resolved = new RecordSlot[columnCount];
                for (int i = 0; i < columnCount; i++) {
                    final Integer slot = indexByName.get(convention.deriveName(columnNames[i]));
                    if (slot == null) {
                        resolved[i] = RecordSlot.unmapped(columnNames[i]);
                        continue;
                    }
                    final Class<?> type = components[slot].getType();
                    resolved[i] = RecordSlot.mapped(slot, quirks.converterOf(type), columnNames[i], type);
                }
                recordSlots = resolved;
                recordConstructor = constructor;
                recordArity = components.length;
                return;
            }
            for (String columnName : columnNames) {
                if (columnName.indexOf('.') >= 0) {
                    // A dotted column walks into a nested object, which needs the builder and its runtime metadata.
                    legacy = true;
                    return;
                }
            }
            final Slot[] planned = planPojo();
            if (planned == null) {
                legacy = true;
                return;
            }
            slots = planned;
        }

        /**
         * Resolves every column of a POJO result set once.
         *
         * @return the plan, or null when this result set is mapped the legacy way.
         */
        private Slot[] planPojo() {
            final PojoMetadata<T> resolved = ObjectBuildableFactory.pojoMetadata(clazz, settings);
            metadata = resolved;
            final Map<String, String> mappings = columnMappings.get();
            final Slot[] resolvedSlots = new Slot[columnCount];
            for (int i = 0; i < columnCount; i++) {
                final String columnName = columnNames[i];
                final PojoProperty property = resolved.getPojoProperty(
                        settings.getNamingConvention().deriveName(columnName), mappings);
                if (property == null) {
                    resolvedSlots[i] = Slot.missing(columnName);
                    continue;
                }
                if (property.getSetter() == null && property.getField() == null) {
                    resolvedSlots[i] = Slot.unwritable(property.getName());
                    continue;
                }
                final Class<?> type = property.getType();
                final Converter<?> converter;
                try {
                    converter = quirks.converterOf(type);
                } catch (RuntimeException e) {
                    // A converter the quirks cannot give fails the row the legacy way, with its own exception.
                    return null;
                }
                resolvedSlots[i] = Slot.mapped(columnName, property, type, converter);
            }
            return resolvedSlots;
        }

        private T plannedRow(ResultSet resultSet, Slot[] plan) throws SQLException {
            final T pojo;
            try {
                pojo = metadata.getConstructor().newInstance();
            } catch (ReflectiveOperationException e) {
                throw new Sql2oException("Error while trying to construct object from class " + clazz, e);
            }
            for (int i = 0; i < columnCount; i++) {
                final Slot slot = plan[i];
                final Object value = quirks.getRSVal(resultSet, i + 1);
                if (slot.missing != null) {
                    if (settings.isThrowOnMappingError()) {
                        throw new Sql2oException("Could not map " + slot.missing + " to any property.");
                    }
                    continue;
                }
                if (slot.unwritable != null) {
                    throw new Sql2oException("No setter or field found for property " + slot.unwritable);
                }
                try {
                    final Object converted = slot.converter.convert(value);
                    slot.property.setConverted(pojo, converted);
                } catch (ConverterException e) {
                    throw new Sql2oException("Error trying to convert value of type " + value.getClass().getName()
                            + " to " + slot.description, e);
                } catch (ReflectiveOperationException e) {
                    throw new Sql2oException("Error when trying to set value for column [" + slot.columnName + "]", e);
                }
            }
            return pojo;
        }

        /**
         * One row of a record result set, through the plan: every value converted into its component slot, then the
         * single canonical-constructor call the builder would have made — including its failures, which is why this
         * does not catch anything the builder would not have.
         */
        private T plannedRecordRow(ResultSet resultSet) throws SQLException {
            final Object[] arguments = new Object[recordArity];
            for (int i = 0; i < columnCount; i++) {
                final RecordSlot slot = recordSlots[i];
                final Object value = quirks.getRSVal(resultSet, i + 1);
                if (slot.index < 0) {
                    throw new IllegalArgumentException("No such field in record: " + slot.columnName);
                }
                try {
                    arguments[slot.index] = slot.converter.convert(value);
                } catch (ConverterException e) {
                    throw new Sql2oException("Error trying to convert column " + slot.columnName
                            + " to type " + slot.type, e);
                }
            }
            try {
                return recordConstructor.newInstance(arguments);
            } catch (ReflectiveOperationException e) {
                throw new Sql2oException("Error occurred while creating object from ResultSet", e);
            }
        }

        private T legacyRow(ResultSet resultSet) throws SQLException {
            final var objectBuilder = objectBuilderDelegate.newObjectBuilder();
            for (int i = 0; i < columnCount; i++) {
                final var colName = columnNames[i];
                try {
                    objectBuilder.withValue(colName, quirks.getRSVal(resultSet, i + 1));
                } catch (ReflectiveOperationException e) {
                    throw new Sql2oException("Error when trying to set value for column [" + colName + "]", e);
                }
            }
            try {
                return objectBuilder.build();
            } catch (ReflectiveOperationException e) {
                throw new Sql2oException("Error occurred while creating object from ResultSet", e);
            }
        }
    }

    /** One column of a record result set, resolved once: which component slot it fills and how. */
    private static final class RecordSlot {
        private final int index;
        private final Converter<?> converter;
        private final String columnName;
        private final Class<?> type;

        private RecordSlot(int index, Converter<?> converter, String columnName, Class<?> type) {
            this.index = index;
            this.converter = converter;
            this.columnName = columnName;
            this.type = type;
        }

        static RecordSlot unmapped(String columnName) {
            return new RecordSlot(-1, null, columnName, null);
        }

        static RecordSlot mapped(int index, Converter<?> converter, String columnName, Class<?> type) {
            return new RecordSlot(index, converter, columnName, type);
        }
    }

    /** One column, resolved once: what it maps to, how its value is converted, and how failures are worded. */
    private static final class Slot {
        private final String columnName;
        private final String missing;
        private final String unwritable;
        private final PojoProperty property;
        private final Converter<?> converter;
        private final String description;

        private Slot(String columnName, String missing, String unwritable,
                     PojoProperty property, Converter<?> converter, String description) {
            this.columnName = columnName;
            this.missing = missing;
            this.unwritable = unwritable;
            this.property = property;
            this.converter = converter;
            this.description = description;
        }

        static Slot missing(String columnName) {
            return new Slot(columnName, columnName, null, null, null, null);
        }

        static Slot unwritable(String propertyName) {
            return new Slot(null, null, propertyName, null, null, null);
        }

        static Slot mapped(String columnName, PojoProperty property, Class<?> type, Converter<?> converter) {
            final String description;
            if (property.getSetter() != null) {
                description = "property " + property.getName() + " [" + property.getSetter().getName() + "] of type "
                        + property.getSetter().getDeclaringClass();
            } else {
                description = "field " + property.getName() + " of type " + property.getField().getDeclaringClass();
            }
            return new Slot(columnName, null, null, property, converter, description);
        }
    }
}
