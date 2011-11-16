package org.sql2o.reflection;

import org.sql2o.Sql2oException;
import org.sql2o.converters.Convert;
import org.sql2o.converters.Converter;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: lars
 * Date: 11/15/11
 * Time: 8:58 PM
 * To change this template use File | Settings | File Templates.
 */
public class PojoMetadata {
    
    private Map<String, Setter> propertySetters;
    private boolean caseSensitive;
    private Class clazz;
    
    public PojoMetadata(Class clazz, boolean caseSensitive){
        
        this.caseSensitive = caseSensitive;
        this.clazz = clazz;
        
        propertySetters = new HashMap<String, Setter>();

        // prepare fields
        for (Field f : clazz.getFields()){
            String propertyName = f.getName();
            propertyName = caseSensitive ? propertyName : propertyName.toLowerCase();
            propertySetters.put(propertyName, new FieldSetter(f));
        }
        
        // prepare methods. Methods will override fields, if both exists.
        for (Method m : clazz.getMethods()){
            if (m.getName().startsWith("set")){
                String propertyName = m.getName().substring(3);
                if (caseSensitive){
                    propertyName = propertyName.substring(0,1).toLowerCase() + propertyName.substring(1);
                }
                else{
                    propertyName = propertyName.toLowerCase();
                }
                
                propertySetters.put(propertyName, new MethodSetter(m));
            }
        }
    }
    
    public Setter getPropertySetter(String propertyName){
        
        String name = this.caseSensitive ? propertyName : propertyName.toLowerCase();
        
        if (propertySetters.containsKey(name)){
            return propertySetters.get(name);
        }
        else{
            String errorMsg = "Property with name '" + propertyName + "' not found on class " + this.clazz.toString();
            if (this.caseSensitive){
                errorMsg += " (You have turned on case sensitive property search. Is this intentional?)";
            }
            throw new Sql2oException(errorMsg);
        }
    }
}

