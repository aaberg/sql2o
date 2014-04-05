package org.sql2o.data;

import org.sql2o.Sql2oException;
import org.sql2o.converters.*;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a result set row.
 */
public class Row {
    
    private Map<Integer, Object> values;

    private boolean isCaseSensitive;
    private Map<String, Integer> columnNameToIdxMap;

    public Row(Map<String, Integer> columnNameToIdxMap, boolean isCaseSensitive) {
        this.columnNameToIdxMap = columnNameToIdxMap;
        this.isCaseSensitive = isCaseSensitive;
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

    public <V> V getObject(int columnIndex, Class clazz){
        try{
            return (V)Convert.getConverter(clazz).convert(getObject(columnIndex));
        } catch (ConverterException ex){
            throw new Sql2oException("Error converting value", ex);
        }
    }

    public <V> V getObject(String columnName, Class clazz) {
        try{
            return (V)Convert.getConverter(clazz).convert(getObject(columnName));
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
            return new DateConverter().convert(getObject(columnIndex));
        } catch (ConverterException e) {
            throw new Sql2oException("Could not convert column with index " + columnIndex + " to " + Date.class.toString());
        }
    }
    
    public Date getDate(String columnName){
        try {
            return new DateConverter().convert(getObject(columnName));
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

    public Map<String, Object> asMap()
    {
        Map<String, Object> map = new HashMap<String, Object>();
        for (Map.Entry<String, Integer> nameIdx : columnNameToIdxMap.entrySet())
        {
            map.put(nameIdx.getKey(), values.get(nameIdx.getValue()));
        }
        return map;
    }
}
