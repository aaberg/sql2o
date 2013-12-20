package org.sql2o.reflection;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.sql2o.Sql2oException;

import com.google.common.base.CaseFormat;

/**
 * Stores metadata for a POJO.
 */
public class PojoMetadata {
    
    private Map<String, Setter> propertySetters;
    private Map<String, Field> fields;
    private boolean caseSensitive;
    private boolean autoDeriveColumnNames;
    private Class clazz;
    
    private Map<String,String> columnMappings;

    public Map<String, String> getColumnMappings() {
        return columnMappings;
    }

    public PojoMetadata(Class clazz, boolean caseSensitive, Map<String,String> columnMappings){
    	this(clazz, caseSensitive, false, columnMappings);
    }
    
    public PojoMetadata(Class clazz, boolean caseSensitive, boolean autoDeriveColumnNames, Map<String,String> columnMappings){
        
        this.caseSensitive = caseSensitive;
        this.autoDeriveColumnNames = autoDeriveColumnNames;
        this.clazz = clazz;
        this.columnMappings = columnMappings == null ? new HashMap<String, String>() : columnMappings;

        propertySetters = new HashMap<String, Setter>();
        fields = new HashMap<String, Field>();

        // prepare fields
        Class theClass = clazz;
        do{
            for (Field f : theClass.getDeclaredFields()){
                String propertyName = f.getName();
                propertyName = caseSensitive ? propertyName : propertyName.toLowerCase();
                propertySetters.put(propertyName, new FieldSetter(f));
                fields.put(propertyName, f);
            }
            
            // prepare methods. Methods will override fields, if both exists.
            for (Method m : theClass.getDeclaredMethods()){
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
            theClass = theClass.getSuperclass();
        }while(!theClass.equals(Object.class));
    }
    
    public Setter getPropertySetter(String propertyName){
        
        String name = this.caseSensitive ? propertyName : propertyName.toLowerCase();

        if(autoDeriveColumnNames) {
        	name = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, name);
        	if(!this.caseSensitive) name = name.toLowerCase();
        }
        
        if (this.columnMappings.containsKey(name)){
            name = this.columnMappings.get(name);
        }
        
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
    
    public Class getType(){
        return this.clazz;
    }
    
    public Object getValueOfProperty(String propertyName, Object object){
        String name = this.caseSensitive ? propertyName : propertyName.toLowerCase();
        
        Field field = this.fields.get(name);
        try {
            return field.get(object);
        } catch (IllegalAccessException e) {
            throw new Sql2oException("could not read value of field " + field.getName() + " on class "+ object.getClass().toString(), e);
        }
    }
    
    // Caching
//    public static Map<Class, PojoMetadata> cachedMetadata = new HashMap<Class, PojoMetadata>();
//
//    public static PojoMetadata getForType(Class clazz, boolean caseSensitive){
//        if (!cachedMetadata.containsKey(clazz)){
//            cachedMetadata.put(clazz, new PojoMetadata(clazz, caseSensitive));
//        }
//
//        return cachedMetadata.get(clazz);
//    }
}

