package org.sql2o.reflection2;

import org.sql2o.NamingConvention;
import org.sql2o.Settings;
import org.sql2o.Sql2oException;
import org.sql2o.converters.ConverterException;
import org.sql2o.quirks.Quirks;

import java.lang.reflect.Constructor;
import java.util.LinkedHashMap;

public class RecordBuilder<T> implements ObjectBuildable<T>{

    private final Constructor<T> constructor;
    private final LinkedHashMap<String, RecordValue> values = new LinkedHashMap<>();
    private final Settings settings;

    @SuppressWarnings("unchecked")
    public RecordBuilder(Class<T> recordClass, Settings settings) {

        this.constructor = (Constructor<T>)recordClass.getDeclaredConstructors()[0];
        this.settings = settings;

        for (final var recordComponent : recordClass.getRecordComponents()) {
            values.put(settings.getNamingConvention().deriveName(recordComponent.getName()), new RecordValue(recordComponent.getType()));
        }
    }

    @Override
    public void withValue(String columnName, Object value) {
        final var derivedName = settings.getNamingConvention().deriveName(columnName);
        if (!values.containsKey(derivedName)) {
            throw new IllegalArgumentException("No such field in record: " + columnName);
        }
        final var convVal = values.get(derivedName);
        try {
            convVal.value = settings.getQuirks().converterOf(convVal.type).convert(value);
        } catch (ConverterException e) {
            throw new Sql2oException("Error trying to convert column " + columnName + " to type " + convVal.type, e);
        }
    }

    @Override
    public T build() throws ReflectiveOperationException {
        return constructor.newInstance(values.values().stream().map(recordValue -> recordValue.value).toArray());
    }

    private static class RecordValue {
        public Object value = null;
        public final Class<?> type;

        public RecordValue(Class<?> type) {
            this.type = type;
        }
    }
}
