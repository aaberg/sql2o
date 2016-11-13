package org.sql2o.data;

import org.sql2o.Sql2oException;
import org.sql2o.converters.*;
import org.sql2o.quirks.Quirks;

import java.math.BigDecimal;
import java.util.*;

import static java.util.Arrays.asList;
import static org.sql2o.converters.Convert.throwIfNull;

/**
 * Represents a result set row.
 */
public class Row {
    
    private final Object[] values;
    private final boolean isCaseSensitive;
    private final Quirks quirks;
    private final Map<String, Integer> columnNameToIdxMap;

    public Row(Map<String, Integer> columnNameToIdxMap, int columnCnt, boolean isCaseSensitive, Quirks quirks) {
        this.columnNameToIdxMap = columnNameToIdxMap;
        this.isCaseSensitive = isCaseSensitive;
        this.quirks = quirks;
        // lol. array works better
        this.values = new Object[columnCnt];
    }

    void addValue(int columnIndex, Object value){
        values[columnIndex]=value;
    }

    public Object getObject(int columnIndex){
        return values[columnIndex];
    }
    
    public Object getObject(String columnName){
        Integer index = columnNameToIdxMap.get(
                isCaseSensitive?columnName
                :columnName.toLowerCase());
        if(index!=null) return getObject(index);
        throw new Sql2oException(String.format("Column with name '%s' does not exist", columnName));
    }

    public <V> V getObject(int columnIndex, Class<V> clazz){
        try{
            return (V) throwIfNull(clazz, quirks.converterOf(clazz)).convert(getObject(columnIndex));
        } catch (ConverterException ex){
            throw new Sql2oException("Error converting value", ex);
        }
    }

    public <V> V getObject(String columnName, Class<V> clazz) {
        try{
            return (V) throwIfNull(clazz, quirks.converterOf(clazz)).convert(getObject(columnName));
        } catch (ConverterException ex){
            throw new Sql2oException("Error converting value", ex);
        }
    }

    public BigDecimal getBigDecimal(int columnIndex){
        return this.getObject(columnIndex, BigDecimal.class);
    }
    
    public BigDecimal getBigDecimal(String columnName){
        return this.getObject(columnName, BigDecimal.class);
    }
    
    public Boolean getBoolean(int columnIndex){
        return this.getObject(columnIndex, Boolean.class);
    }
    
    public Boolean getBoolean(String columnName){
        return this.getObject(columnName, Boolean.class);
    }
    
    public Double getDouble(int columnIndex){
        return this.getObject(columnIndex, Double.class);
    }
    
    public Double getDouble(String columnName){
        return this.getObject(columnName, Double.class);
    }
    
    public Float getFloat(int columnIndex){
        return this.getObject(columnIndex, Float.class);
    }
    
    public Float getFloat(String columnName){
        return this.getObject(columnName, Float.class);
    }
    
    public Long getLong(int columnIndex){
        return this.getObject(columnIndex, Long.class);
    }
    
    public Long getLong(String columnName){
        return this.getObject(columnName, Long.class);
    }
    
    public Integer getInteger(int columnIndex){
        return this.getObject(columnIndex, Integer.class);
    }
    
    public Integer getInteger(String columnName){
        return this.getObject(columnName, Integer.class);
    }
    
    public Short getShort(int columnIndex){
        return this.getObject(columnIndex, Short.class);
    }
    
    public Short getShort(String columnName){
        return this.getObject(columnName, Short.class);
    }
    
    public Byte getByte(int columnIndex){
        return this.getObject(columnIndex, Byte.class);
    }
    
    public Byte getByte(String columnName){
        return this.getObject(columnName, Byte.class);
    }
    
    public Date getDate(int columnIndex){
        return this.getObject(columnIndex, Date.class);
    }
    
    public Date getDate(String columnName){
        return this.getObject(columnName, Date.class);
    }

    public String getString(int columnIndex){
        return this.getObject(columnIndex, String.class);
    }
    
    public String getString(String columnName){
        return this.getObject(columnName, String.class);
    }

    /**
     * View row as a simple map.
     */
    public Map<String, Object> asMap() {
        Map map = new HashMap();
        Set<String> keys = columnNameToIdxMap.keySet();
        Iterator iterator = keys.iterator();
        while (iterator.hasNext()) {
            String colum = iterator.next().toString();
            int index = columnNameToIdxMap.get(colum);
            map.put(colum, values[index]);
        }
        return map;
    }
}
