package org.sql2o.reflection;

import org.sql2o.Sql2oException;
import org.sql2o.converters.Convert;
import org.sql2o.converters.Converter;
import org.sql2o.converters.ConverterException;

import java.util.HashMap;
import java.util.Map;

/**
 * Used internally to represent a plain old java object.
 */
public class Pojo {

    private PojoMetadata metadata;
    private boolean caseSensitive;
    private Object object;
    
    public Pojo(PojoMetadata metadata, boolean caseSensitive, Object object){
        this.caseSensitive = caseSensitive;
        this.metadata = metadata;
        this.object = object;
    }
    
    public Pojo(PojoMetadata metadata, boolean caseSensitive){
        this.caseSensitive = caseSensitive;
        this.metadata = metadata;
        Class clazz = metadata.getType();

        try {
            object = clazz.newInstance();
        } catch (InstantiationException e) {
            throw new Sql2oException("Could not create a new instance of class " + clazz.toString(), e);
        } catch (IllegalAccessException e) {
            throw new Sql2oException("Could not create a new instance of class " + clazz.toString(), e);
        }
    }
    
    Map<String, Object> instantiatedObjects = new HashMap<String, Object>();
    
    public void setProperty(String propertyPath, Object value){
        
        String[] pathArr = propertyPath.split("\\.");
        Setter setter = metadata.getPropertySetter(pathArr[0]);
        
        if (pathArr.length > 1){
            int dotIdx = propertyPath.indexOf('.');
            String newPath = propertyPath.substring(dotIdx + 1);
            
            Object subValue = this.metadata.getValueOfProperty(pathArr[0], this.object);
            if (subValue == null){
                try {
                    subValue = setter.getType().newInstance();
                } catch (InstantiationException e) {
                    throw new Sql2oException("Could not instantiate a new instance of class "+ setter.getType().toString(), e);
                } catch (IllegalAccessException e) {
                    throw new Sql2oException("Could not instantiate a new instance of class "+ setter.getType().toString(), e);
                }
                setter.setProperty(this.object, subValue);
            }
            
            PojoMetadata subMetadata = new PojoMetadata(setter.getType(), this.caseSensitive, this.metadata.getColumnMappings());
            Pojo subPojo = new Pojo(subMetadata, this.caseSensitive, subValue);
            subPojo.setProperty(newPath, value);
        }
        else{
            
         
            Converter converter;
            try {
                converter = Convert.getConverter(setter.getType());
            } catch (ConverterException e) {
                throw new Sql2oException("Cannot convert column " + propertyPath + " to type " + setter.getType(), e);
            }
    
            try {
                setter.setProperty(this.object, converter.convert( value ));
            } catch (ConverterException e) {
                throw new Sql2oException("Error trying to convert column " + propertyPath + " to type " + setter.getType(), e);
            }
        }
        
        
    }
   
    
    public Object getObject(){
        return this.object;
    }
    
}
