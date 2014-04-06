package org.sql2o.reflection;

import org.sql2o.Sql2oException;
import org.sql2o.converters.Convert;
import org.sql2o.converters.Converter;
import org.sql2o.converters.ConverterException;

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
        ObjectConstructor objectConstructor = metadata.getObjectConstructor();
        object = objectConstructor.newInstance();
    }

    public void setProperty(String propertyPath, Object value){
        // String.split uses RegularExpression
        // this is overkill for every column for every row
        int index = propertyPath.indexOf('.');
        Setter setter;
        if (index > 0){
            final String substring = propertyPath.substring(0, index);
            setter = metadata.getPropertySetter(substring);
            String newPath = propertyPath.substring(index+1);
            
            Object subValue = this.metadata.getValueOfProperty(substring, this.object);
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
            setter = metadata.getPropertySetter(propertyPath);
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
