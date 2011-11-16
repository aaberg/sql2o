package org.sql2o.reflection;

import org.sql2o.Sql2oException;
import org.sql2o.converters.Convert;
import org.sql2o.converters.Converter;
import org.sql2o.converters.ConverterException;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: lars
 * Date: 11/16/11
 * Time: 9:34 PM
 * To change this template use File | Settings | File Templates.
 */
public class Pojo {

    // todo column mappings

    private PojoMetadata metadata;
    private boolean caseSensitive;
    private Object object;
    
    public Pojo(Class clazz, boolean caseSensitive){
        this.caseSensitive = caseSensitive;
        this.metadata = new PojoMetadata(clazz, caseSensitive);

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
            // todo
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
