package org.sql2o.reflection2;

import org.sql2o.Settings;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// Helper class for the PojoBuilder class. This class contains cacheable metadata about the POJO class.
public class PojoMetadata<T> {

    private final Constructor<T> constructor;
    private final Map<String, PojoProperty> pojoProperties;
    private final Settings settings;

    public PojoMetadata(Class<T> clazz, Settings settings) throws ReflectiveOperationException {
        this.constructor = clazz.getDeclaredConstructor();
        this.constructor.setAccessible(true);
        this.settings = settings;

        final var pojoPropertyBuilders = getPojoPropertyBuilders(clazz);

        final var pojoPropertiesStream = pojoPropertyBuilders.stream().map(PojoPropertyBuilder::build);
        pojoProperties =
            pojoPropertiesStream.collect(Collectors.toMap(PojoProperty::getName, p -> p));

        // also associate the annotated name with the property
        final var pojoPropertiesWithAnnotatedName = pojoProperties.values().stream()
            .filter(p -> p.getAnnotatedName() != null).toList();
        pojoPropertiesWithAnnotatedName.forEach(p -> pojoProperties.put(p.getAnnotatedName(), p));
    }

    public Constructor<T> getConstructor() {
        return constructor;
    }

    public PojoProperty getPojoProperty(String name) {
        return pojoProperties.get(name);
    }

    private List<PojoPropertyBuilder> getPojoPropertyBuilders(Class<?> clazz) {
        final var pojoPropertyBuilders = new HashMap<String, PojoPropertyBuilder>();
        initializeForClassRecursive(clazz, pojoPropertyBuilders);
        return new ArrayList<>(pojoPropertyBuilders.values());
    }

    /***
     * This method fills the pojoPropertyBuilders parameter with instances of the PojoPropertyBuilder class, for each property in the class and its subclasses.
     * @param clazz
     * @param pojoPropertyBuilders
     */
    private void initializeForClassRecursive(Class<?> clazz, HashMap<String, PojoPropertyBuilder> pojoPropertyBuilders) {
        for (final var method : clazz.getDeclaredMethods()) {
            if (Modifier.isStatic(method.getModifiers())) {
                continue;
            }
            method.setAccessible(true);
            final var methodName = method.getName();
            if (methodName.startsWith("get") && method.getParameterCount() == 0) {
                final var propertyName = settings.getNamingConvention().deriveName(methodName.substring(3));
                pojoPropertyBuilders.computeIfAbsent(propertyName, name -> new PojoPropertyBuilder(name, settings)).withGetter(method);
            } else if (methodName.startsWith("is") && method.getParameterCount() == 0) {
                final var propertyName = settings.getNamingConvention().deriveName(methodName.substring(2));
                pojoPropertyBuilders.computeIfAbsent(propertyName, name -> new PojoPropertyBuilder(name, settings)).withGetter(method);
            } else if (methodName.startsWith("set") && method.getParameterCount() == 1) {
                final var propertyName = settings.getNamingConvention().deriveName(methodName.substring(3));
                pojoPropertyBuilders.computeIfAbsent(propertyName, name -> new PojoPropertyBuilder(name, settings)).withSetter(method);
            }

            if (pojoPropertyBuilders.containsKey(methodName)){
                pojoPropertyBuilders.get(methodName).withAnnotatedName(deriveAnnotatedName(method));
            }
        }

        for (final var field : clazz.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            final var fieldName = settings.getNamingConvention().deriveName(field.getName());
            pojoPropertyBuilders.computeIfAbsent(fieldName, name -> new PojoPropertyBuilder(name, settings)).withField(field);

            if (pojoPropertyBuilders.containsKey(fieldName)){
                pojoPropertyBuilders.get(fieldName).withAnnotatedName(deriveAnnotatedName(field));
            }
        }

        // Recursively initialize for the superclass, except if the superclass is 'Object'.
        final var superClass = clazz.getSuperclass();
        if (!superClass.equals(Object.class)) {
            initializeForClassRecursive(superClass, pojoPropertyBuilders);
        }
    }

    private static String deriveAnnotatedName(Executable method) {
        if (!isJavaxPersistenceColumnPresent())
            return null;

        final var columnAnnotation = method.getAnnotation(javax.persistence.Column.class);
        if (columnAnnotation == null) return null;
        return columnAnnotation.name();
    }

    private static String deriveAnnotatedName(Field field) {
        if (!isJavaxPersistenceColumnPresent())
            return null;

        final var columnAnnotation = field.getAnnotation(javax.persistence.Column.class);
        if (columnAnnotation == null) return null;
        return columnAnnotation.name();
    }


    private static Boolean javaxPersistenceColumnPresence = null;
    private static boolean isJavaxPersistenceColumnPresent() {
        if (javaxPersistenceColumnPresence != null)
            return javaxPersistenceColumnPresence;

        try {
            Class.forName("javax.persistence.Column");
            javaxPersistenceColumnPresence = true;
        } catch (ClassNotFoundException e) {
            javaxPersistenceColumnPresence = false;
        }

        return javaxPersistenceColumnPresence;
    }
}
