package org.sql2o.reflection;

import org.sql2o.Sql2oException;
import org.sql2o.tools.AbstractCache;
import org.sql2o.tools.UnderscoreToCamelCase;

import javax.persistence.Column;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Stores metadata for a POJO.
 */
public class PojoMetadata {

    private static final Cache caseSensitiveFalse = new Cache();
    private static final Cache caseSensitiveTrue = new Cache();
    private final PropertyAndFieldInfo propertyInfo;
    private final Map<String, String> columnMappings;
    private final FactoryFacade factoryFacade = FactoryFacade.getInstance();

    private boolean caseSensitive;
    private boolean autoDeriveColumnNames;
    public final boolean throwOnMappingFailure;
    private Class clazz;

    public boolean isCaseSensitive() {
        return caseSensitive;
    }

    public boolean isAutoDeriveColumnNames() {
        return autoDeriveColumnNames;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PojoMetadata that = (PojoMetadata) o;

        return autoDeriveColumnNames == that.autoDeriveColumnNames
                && caseSensitive == that.caseSensitive
                && clazz.equals(that.clazz)
                && columnMappings.equals(that.columnMappings)
                && propertyInfo.equals(that.propertyInfo);

    }

    @Override
    public int hashCode() {
        int result = (caseSensitive ? 1 : 0);
        result = 31 * result + clazz.hashCode();
        return result;
    }

    public PojoMetadata(Class clazz, boolean caseSensitive, boolean autoDeriveColumnNames, Map<String, String> columnMappings, boolean throwOnMappingError) {
        this.caseSensitive = caseSensitive;
        this.autoDeriveColumnNames = autoDeriveColumnNames;
        this.clazz = clazz;
        this.columnMappings = columnMappings == null ? Collections.<String,String>emptyMap() : columnMappings;

        this.propertyInfo = getPropertyInfoThroughCache();
        this.throwOnMappingFailure = throwOnMappingError;

    }

    public ObjectConstructor getObjectConstructor() {
        return propertyInfo.objectConstructor;
    }

    private PropertyAndFieldInfo getPropertyInfoThroughCache() {
        return (caseSensitive
                ? caseSensitiveTrue
                : caseSensitiveFalse)
                .get(clazz, this);
    }

    private PropertyAndFieldInfo initializePropertyInfo() {

        HashMap<String, Getter> propertyGetters = new HashMap<String, Getter>();
        HashMap<String, Setter> propertySetters = new HashMap<String, Setter>();
        HashMap<String, Field> fields = new HashMap<String, Field>();

        boolean isJpaColumnInClasspath = false;
        try {
            Class.forName("javax.persistence.Column");
            isJpaColumnInClasspath = true;
        } catch (ClassNotFoundException e) {
            // javax.persistence.Column is not in the classpath
        }
        
        Class theClass = clazz;
        ObjectConstructor objectConstructor = factoryFacade.newConstructor(theClass);
        do {
            for (Field f : theClass.getDeclaredFields()) {
                if(Modifier.isStatic(f.getModifiers())) {
                    continue;
                }
                String propertyName = readAnnotatedColumnName(f, isJpaColumnInClasspath);
                if(propertyName == null) {
                    propertyName = f.getName();
                }
                propertyName = caseSensitive ? propertyName : propertyName.toLowerCase();
                
                propertyGetters.put(propertyName, factoryFacade.newGetter(f));
                propertySetters.put(propertyName, factoryFacade.newSetter(f));
                fields.put(propertyName, f);
            }

            // prepare methods. Methods will override fields, if both exists.
            for (Method m : theClass.getDeclaredMethods()) {

                if (m.getName().startsWith("get")) {
                    String propertyName = m.getName().substring(3);
                    if (caseSensitive) {
                        propertyName = propertyName.substring(0, 1).toLowerCase() + propertyName.substring(1);
                    } else {
                        propertyName = propertyName.toLowerCase();
                    }

                    propertyGetters.put(propertyName, factoryFacade.newGetter(m));
                }

                if (m.getName().startsWith("set") && m.getParameterTypes().length == 1) {
                    String propertyName = readAnnotatedColumnName(m, isJpaColumnInClasspath);
                    if(propertyName == null) {
                        propertyName = m.getName().substring(3);
                    }
                    if (caseSensitive) {
                        propertyName = propertyName.substring(0, 1).toLowerCase() + propertyName.substring(1);
                    } else {
                        propertyName = propertyName.toLowerCase();
                    }

                    propertySetters.put(propertyName, factoryFacade.newSetter(m));
                }
            }
            theClass = theClass.getSuperclass();
        } while (!theClass.equals(Object.class));

        return new PropertyAndFieldInfo(propertyGetters, propertySetters, fields, objectConstructor);

    }

    public Map<String, String> getColumnMappings() {
        return columnMappings;
    }

    public Getter getPropertyGetter(String propertyName) {

        Getter getter = getPropertyGetterIfExists(propertyName);

        if (getter != null) {
            return getter;
        } else {
            String errorMsg = "Property with name '" + propertyName + "' not found on class " + this.clazz.toString();
            if (this.caseSensitive) {
                errorMsg += " (You have turned on case sensitive property search. Is this intentional?)";
            }
            throw new Sql2oException(errorMsg);
        }
    }

    public Getter getPropertyGetterIfExists(String propertyName) {

        String name = this.caseSensitive ? propertyName : propertyName.toLowerCase();

        if (this.columnMappings.containsKey(name)) {
            name = this.columnMappings.get(name);
        }

        if (autoDeriveColumnNames) {
            name = UnderscoreToCamelCase.convert(name);
            if (!this.caseSensitive) name = name.toLowerCase();
        }

        return propertyInfo.propertyGetters.get(name);
    }

    public Setter getPropertySetter(String propertyName) {

        Setter setter = getPropertySetterIfExists(propertyName);

        if (setter != null) {
            return setter;
        } else {
            String errorMsg = "Property with name '" + propertyName + "' not found on class " + this.clazz.toString();
            if (this.caseSensitive) {
                errorMsg += " (You have turned on case sensitive property search. Is this intentional?)";
            }
            throw new Sql2oException(errorMsg);
        }
    }

    public Setter getPropertySetterIfExists(String propertyName) {

        String name = this.caseSensitive ? propertyName : propertyName.toLowerCase();

        if (this.columnMappings.containsKey(name)) {
            name = this.columnMappings.get(name);
        }

        if (autoDeriveColumnNames) {
            name = UnderscoreToCamelCase.convert(name);
            if (!this.caseSensitive) name = name.toLowerCase();
        }

        return propertyInfo.propertySetters.get(name);
    }

    public Class getType() {
        return this.clazz;
    }

    public Object getValueOfProperty(String propertyName, Object object) {
        Getter getter = getPropertyGetter(propertyName);

        return getter.getProperty(object);
    }
    
    /**
     * Try to read the {@link javax.persistence.Column} annotation and return the name of the column.
     * Returns null if no {@link javax.persistence.Column} annotation is present or if the name of the column is empty
     */
    private String readAnnotatedColumnName(AnnotatedElement classMember, boolean isJpaColumnInClasspath) {
        if(isJpaColumnInClasspath) {
            Column columnInformation = classMember.getAnnotation(Column.class);
            if(columnInformation != null && columnInformation.name() != null && !columnInformation.name().isEmpty()) {
                return columnInformation.name();
            }
        }
        return null;
    }

    private static class Cache extends AbstractCache<Class, PropertyAndFieldInfo, PojoMetadata> {
        @Override
        protected PropertyAndFieldInfo evaluate(Class key, PojoMetadata param) {
            return param.initializePropertyInfo();
        }
    }

    private static class PropertyAndFieldInfo {
        // since this class is private we can just use field access
        // to make HotSpot a little less work for inlining
        public final Map<String, Getter> propertyGetters;
        public final Map<String, Setter> propertySetters;
        public final Map<String, Field> fields;
        public final ObjectConstructor objectConstructor;

        private PropertyAndFieldInfo(
            Map<String, Getter> propertyGetters, Map<String, Setter> propertySetters,
            Map<String, Field> fields, ObjectConstructor objectConstructor) {

            this.propertyGetters = propertyGetters;
            this.propertySetters = propertySetters;
            this.fields = fields;
            this.objectConstructor = objectConstructor;
        }
    }
}

