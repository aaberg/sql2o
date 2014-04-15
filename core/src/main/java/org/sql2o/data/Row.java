package org.sql2o.data;

import org.sql2o.Sql2oException;
import org.sql2o.converters.*;
import org.sql2o.quirks.Quirks;

import java.math.BigDecimal;
import java.util.*;

import static org.sql2o.converters.Convert.throwIfNull;

/**
 * Represents a result set row.
 */
public class Row {
    
    private Map<Integer, Object> values;

    private boolean isCaseSensitive;
    private final Quirks quirks;
    private Map<String, Integer> columnNameToIdxMap;

    public Row(Map<String, Integer> columnNameToIdxMap, boolean isCaseSensitive, Quirks quirks) {
        this.columnNameToIdxMap = columnNameToIdxMap;
        this.isCaseSensitive = isCaseSensitive;
        this.quirks = quirks;
        this.values = new HashMap<Integer, Object>();
    }

    void addValue(int columnIndex, Object value){
        values.put(columnIndex, value);
    }

    public Object getObject(int columnIndex){
        return values.get(columnIndex);
    }
    
    public Object getObject(String columnName){
        String col = isCaseSensitive ? columnName : columnName.toLowerCase();

        Object obj;
        try{
            obj = getObject(columnNameToIdxMap.get(col));
        }
        catch (NullPointerException ex){
            throw new Sql2oException(String.format("Column with name '%s' does not exist", columnName), ex);
        }
        
        return obj;
    }

    @SuppressWarnings("unchecked")
    public <V> V getObject(int columnIndex, Class clazz){
        try{
            return (V) throwIfNull(clazz, quirks.converterOf(clazz)).convert(getObject(columnIndex));
        } catch (ConverterException ex){
            throw new Sql2oException("Error converting value", ex);
        }
    }

    @SuppressWarnings("unchecked")
    public <V> V getObject(String columnName, Class clazz) {
        try{
            return (V) throwIfNull(clazz, quirks.converterOf(clazz)).convert(getObject(columnName));
        } catch (ConverterException ex){
            throw new Sql2oException("Error converting value", ex);
        }
    }

    public BigDecimal getBigDecimal(int columnIndex){
        return new BigDecimalConverter().convert(getObject(columnIndex));
    }
    
    public BigDecimal getBigDecimal(String columnName){
        return new BigDecimalConverter().convert(getObject(columnName));
    }
    
    public Double getDouble(int columnIndex){
        return new DoubleConverter(false).convert(getObject(columnIndex));
    }
    
    public Double getDouble(String columnName){
        return new DoubleConverter(false).convert(getObject(columnName));
    }
    
    public Float getFloat(int columnIndex){
        return new FloatConverter(false).convert(getObject(columnIndex));
    }
    
    public Float getFloat(String columnName){
        return new FloatConverter(false).convert(getObject(columnName));
    }
    
    public Long getLong(int columnIndex){
        return new LongConverter(false).convert(getObject(columnIndex));
    }
    
    public Long getLong(String columnName){
        return new LongConverter(false).convert(getObject(columnName));
    }
    
    public Integer getInteger(int columnIndex){
        return new IntegerConverter(false).convert(getObject(columnIndex));
    }
    
    public Integer getInteger(String columnName){
        return new IntegerConverter(false).convert(getObject(columnName));
    }
    
    public Short getShort(int columnIndex){
        return new ShortConverter(false).convert(getObject(columnIndex));
    }
    
    public Short getShort(String columnName){
        return new ShortConverter(false).convert(getObject(columnName));
    }
    
    public Byte getByte(int columnIndex){
        return new ByteConverter(false).convert(getObject(columnIndex));
    }
    
    public Byte getByte(String columnName){
        return new ByteConverter(false).convert(getObject(columnName));
    }
    
    public Date getDate(int columnIndex){
        try {
            return DateConverter.instance.convert(getObject(columnIndex));
        } catch (ConverterException e) {
            throw new Sql2oException("Could not convert column with index " + columnIndex + " to " + Date.class.toString());
        }
    }
    
    public Date getDate(String columnName){
        try {
            return DateConverter.instance.convert(getObject(columnName));
        } catch (ConverterException e) {
            throw new Sql2oException("Could not convert column with name " + columnName + " to " + Date.class.toString());
        }
    }

    public String getString(int columnIndex){
        try {
            return new StringConverter().convert(getObject(columnIndex));
        } catch (ConverterException e) {
            throw new Sql2oException("Could not convert column with index " + columnIndex + " to " + String.class.getName());
        }
    }
    
    public String getString(String columnName){
        try {
            return new StringConverter().convert(getObject(columnName));
        } catch (ConverterException e) {
            throw new Sql2oException("Could not convert column with name " + columnName+ " to " + String.class.getName());
        }
    }

    /**
     * View row as a simple map.
     */
    public Map<String, Object> asMap()
    {
        return new Map<String, Object>() {
            public int size() {
                return values.size();
            }

            public boolean isEmpty() {
                return values.isEmpty();
            }

            public boolean containsKey(Object key) {
                return columnNameToIdxMap.containsKey(key);
            }

            public boolean containsValue(Object value) {
                return values.containsValue(value);
            }

            public Object get(Object key) {
                return values.get(columnNameToIdxMap.get(key));
            }

            public Object put(String key, Object value) {
                throw new UnsupportedOperationException("Row map is immutable.");
            }

            public Object remove(Object key) {
                throw new UnsupportedOperationException("Row map is immutable.");
            }

            public void putAll(Map<? extends String, ?> m) {
                throw new UnsupportedOperationException("Row map is immutable.");
            }

            public void clear() {
                throw new UnsupportedOperationException("Row map is immutable.");
            }

            public Set<String> keySet() {
                return columnNameToIdxMap.keySet();
            }

            public Collection<Object> values() {
                return values.values();
            }

            public Set<Entry<String, Object>> entrySet() {
                throw new UnsupportedOperationException("Row map does not support entrySet.");
            }
        };
    }
}
