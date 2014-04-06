package org.sql2o.reflection;

import org.sql2o.Sql2oException;
import org.sql2o.tools.FeatureDetector;
import org.sql2o.tools.UnderscoreToCamelCase;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

/**
 * Stores metadata for a POJO.
 */
public class PojoMetadata {

    private final PropertyAndFieldInfo propertyInfo;
    private final boolean caseSensitive;
    private final boolean autoDeriveColumnNames;
    private final Class clazz;
    
    private final Map<String,String> columnMappings;

    public PojoMetadata(Class clazz, boolean caseSensitive, Map<String,String> columnMappings) {
        this(clazz, caseSensitive, false, columnMappings);
    }
    
    public PojoMetadata(Class clazz, boolean caseSensitive, boolean autoDeriveColumnNames, Map<String,String> columnMappings) {
        this.caseSensitive = caseSensitive;
        this.autoDeriveColumnNames = autoDeriveColumnNames;
        this.clazz = clazz;
        this.columnMappings = columnMappings == null ? new HashMap<String, String>() : columnMappings;

        if (FeatureDetector.isCachePojoMetaDataEnabled()) {
            this.propertyInfo = getPropertyInfoThroughCache();
        } else {
            this.propertyInfo = initializePropertyInfo();
        }
    }

    private PropertyAndFieldInfo getPropertyInfoThroughCache() {
        final CacheKey key = new CacheKey(clazz, caseSensitive);
        synchronized (cache) {
            if (!cache.contains(key)) {
                cache.put(key, initializePropertyInfo());
            }
            return cache.get(key);
        }
    }

    private PropertyAndFieldInfo initializePropertyInfo() {

        HashMap<String, Setter> propertySetters = new HashMap<String, Setter>();
        HashMap<String, Field> fields = new HashMap<String, Field>();

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

        return new PropertyAndFieldInfo(propertySetters, fields);

    }

    public Map<String, String> getColumnMappings() {
        return columnMappings;
    }

    public Setter getPropertySetter(String propertyName){

        Setter setter = getPropertySetterIfExists(propertyName);

        if (setter != null) {
            return setter;
        }
        else{
            String errorMsg = "Property with name '" + propertyName + "' not found on class " + this.clazz.toString();
            if (this.caseSensitive){
                errorMsg += " (You have turned on case sensitive property search. Is this intentional?)";
            }
            throw new Sql2oException(errorMsg);
        }
    }

    public Setter getPropertySetterIfExists(String propertyName){

        String name = this.caseSensitive ? propertyName : propertyName.toLowerCase();

        if (this.columnMappings.containsKey(name)){
            name = this.columnMappings.get(name);
        }

        if(autoDeriveColumnNames) {
            name = UnderscoreToCamelCase.convert(name);
            if(!this.caseSensitive) name = name.toLowerCase();
        }

        if (propertyInfo.getPropertySetters().containsKey(name)){
            return propertyInfo.getPropertySetters().get(name);
        }

        return null;
    }
    
    public Class getType(){
        return this.clazz;
    }
    
    public Object getValueOfProperty(String propertyName, Object object){
        String name = this.caseSensitive ? propertyName : propertyName.toLowerCase();
        
        Field field = this.propertyInfo.getFields().get(name);
        try {
            return field.get(object);
        } catch (IllegalAccessException e) {
            throw new Sql2oException("could not read value of field " + field.getName() + " on class "+ object.getClass().toString(), e);
        }
    }

    // CACHING

    private final static Hashtable<CacheKey, PropertyAndFieldInfo> cache = new Hashtable<CacheKey, PropertyAndFieldInfo>();

    private class CacheKey {
        private Class clazz;
        private boolean caseSensitive;

        private CacheKey(Class clazz, boolean caseSensitive) {
            this.clazz = clazz;
            this.caseSensitive = caseSensitive;
        }

        // generated by Intellij
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof CacheKey)) return false;
            CacheKey cacheKey = (CacheKey) o;
            return caseSensitive == cacheKey.caseSensitive && clazz.equals(cacheKey.clazz);
        }

        // generated by Intellij
        @Override
        public int hashCode() {
            int result = clazz.hashCode();
            result = 31 * result + (caseSensitive ? 1 : 0);
            return result;
        }
    }


    private class PropertyAndFieldInfo {
        private final Map<String, Setter> propertySetters;
        private final Map<String, Field> fields;

        private PropertyAndFieldInfo(Map<String, Setter> propertySetters, Map<String, Field> fields) {
            this.propertySetters = propertySetters;
            this.fields = fields;
        }

        public Map<String, Setter> getPropertySetters() {
            return propertySetters;
        }

        public Map<String, Field> getFields() {
            return fields;
        }
    }
}

