package org.sql2o.bytecode;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.sql2o.NamingConvention;
import org.sql2o.Settings;
import org.sql2o.reflection2.ObjectBuildableFactory;
import org.sql2o.reflection2.PojoMetadata;
import org.sql2o.reflection2.PojoProperty;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

/**
 * Compiles one shape into a reader class.
 *
 * <p>A shape is a target class together with the names of the columns a result set will have, the naming convention and
 * the column mappings in force, and whether an unmapped column is an error. Everything that can be decided once is
 * decided here, once: which column goes to which member, what type that member has, and whether the column is wanted at
 * all. What is left in the reader is reading the column, converting it and putting it somewhere.
 *
 * <p>Two things are deliberately not compiled in. The quirks arrive per call, because {@code OracleQuirks} replaces the
 * timestamps oracle hands out and {@code PostgresQuirks} asks for an {@code OffsetTime} where the column has a zone, and
 * the reader has to go through whichever instance is current. The converters arrive per call too, so that one compiled
 * reader serves every {@code Sql2o} and a converter registered after compilation is still seen.
 *
 * <p>Refusing is a normal outcome rather than a failure, which is why {@link Outcome} carries the reason and not just a
 * refusal. A reader is only built for a shape it can produce exactly what the reflective path would.
 */
final class RowPlanCompiler {

    private static final String READER = Type.getInternalName(RowReader.class);
    private static final String SUPPORT = Type.getInternalName(ReaderSupport.class);
    private static final String QUIRKS = "org/sql2o/quirks/Quirks";
    private static final String CONVERTER = "org/sql2o/converters/Converter";
    private static final String RESULT_SET = "java/sql/ResultSet";
    private static final String OBJECT = "java/lang/Object";
    private static final String SQL_EXCEPTION = "java/sql/SQLException";
    private static final String ILLEGAL_ARGUMENT = "java/lang/IllegalArgumentException";
    private static final String MAPPING_EXCEPTION = "org/sql2o/Sql2oException";

/**
 * The descriptor of the reader's method, which has to declare the checked exception that reading a column through the
 * quirks throws. Left off it, the class would not verify.
 */
private static final String BUILD_DESCRIPTOR =
            "(" + objectType(RESULT_SET)
                    + objectType(QUIRKS)
                    + "[" + objectType(CONVERTER) + ")"
                    + objectType(OBJECT);

    /** The local holding the object being built: the POJO itself, or the argument array of a record. */
    private static final int LOCAL_TARGET = 4;
    /** The local holding one value between reading it and writing it. */
    private static final int LOCAL_VALUE = 5;
    /** The local holding the converter of the column being read. */
    private static final int LOCAL_CONVERTER = 6;

    private static final String READER_PREFIX = "Sql2oRowReader";

    /**
     * Either a plan or the reason there is none, so that a refusal can be read as well as acted on.
     *
     * <p>The bytes come out of a compilation with it because the one thing worth checking about generated code is the
     * code, and a class defined at runtime has no resource to read itself back from. What goes through the verifier is
     * therefore the very array that was defined rather than a second copy of it.
     */
    record Outcome(RowPlan plan, String refusal, byte[] bytes) {

        static Outcome compiled(RowPlan plan, byte[] bytes) {
            return new Outcome(plan, null, bytes);
        }

        static Outcome refused(String reason) {
            return new Outcome(null, reason, null);
        }

        boolean compiled() {
            return plan != null;
        }
    }

    /** What one column of the result set does when a row arrives. */
    private enum Action {
        /** Written through a setter, which the reflective path prefers over a field. */
        SETTER,
        /** Written straight into a field, since there is no setter, or into a component at construction. */
        FIELD,
        /** No property matches and that is not an error, so the column is not read at all. */
        SKIP,
        /** No property matches and that is an error, raised per row rather than at compile time. */
        UNMAPPED_PROPERTY,
        /** No record component matches, which the reflective path reports the same way. */
        UNMAPPED_COMPONENT
    }

    /** One column of the result set and where its value goes. */
    private static final class Column {
        Action action;
        Class<?> type;
        Field field;
        Method setter;
        /** Where the value is going, worded the way the reflective path words it. */
        String description;
        /** The column name, for a record whose wording differs from a property's. */
        String columnName;
        /** Which record component this is. Unused for a POJO. */
        int slot;
    }

    private final Class<?> targetClass;
    private final String targetClassName;
    private final Settings settings;
    private final Access access;
    private final String[] columnNames;

    RowPlanCompiler(Class<?> targetClass, Settings settings, String[] columnNames) {
        this.targetClass = targetClass;
        this.targetClassName = Type.getInternalName(targetClass);
        this.settings = settings;
        this.columnNames = columnNames;
        this.access = LOOKUPS.get(targetClass);
    }

    /**
     * A lookup with full privilege access to the package of a class, or the reason there is none.
     *
     * <p>This is what lets generated code reach a private field or a package private constructor without asking anyone
     * for {@code --add-opens}: defining the reader into the same package makes it the same runtime package, so the access
     * rules of the language apply as they always did.
     *
     * <p>Class by class rather than in a map of our own, so that nothing here outlives the class it belongs to. A class
     * in a named module that does not open its package refuses here, and that shape goes back to the reflective path.
     */
    private record Access(MethodHandles.Lookup lookup, String refusal) {
    }

    private static final ClassValue<Access> LOOKUPS = new ClassValue<>() {
        @Override
        protected Access computeValue(Class<?> type) {
            try {
                return new Access(MethodHandles.privateLookupIn(type, MethodHandles.lookup()), null);
            } catch (IllegalAccessException e) {
                return new Access(null, "no access to the package of " + type.getName()
                        + "; a named module would have to open it");
            }
        }
    };

    /** Compiles a shape, or says why it cannot be compiled. */
    Outcome compile(Map<String, String> columnMappings) {
        if (access.lookup() == null) {
            return Outcome.refused(access.refusal());
        }
        if (targetClass.getPackageName().isEmpty()) {
            return Outcome.refused(binaryName() + " is in the default package, which has no package to define a reader into");
        }

        try {
            Column[] columns = targetClass.isRecord()
                    ? columnsOfRecord()
                    : columnsOfPojo(columnMappings);

            String digest = digestOf(columnMappings);
            final byte[] bytes = generate(columns, digest);
            RowReader reader = define(readersNameFor(digest).replace('/', '.'), bytes, digest);

            Class<?>[] types = new Class<?>[columns.length];
            String[] descriptions = new String[columns.length];
            for (int i = 0; i < columns.length; i++) {
                types[i] = columns[i].type;
                descriptions[i] = columns[i].description;
            }
            return Outcome.compiled(new RowPlan(reader, types, descriptions), bytes);
        } catch (NotCompilable e) {
            return Outcome.refused(e.getMessage());
        } catch (ReflectiveOperationException e) {
            // A class the reflective path cannot build either, such as one with no no-argument constructor. Refusing
            // sends it back to core, which reports it exactly as it always did.
            return Outcome.refused("cannot build " + binaryName() + ": " + e);
        }
    }

    private String binaryName() {
        return targetClass.getName();
    }

    private static final class NotCompilable extends Exception {
        NotCompilable(String message) {
            super(message);
        }
    }

    /** The cause the cache wrapped, which is what the message has to name rather than the wrapping. */
    private static Throwable rootCause(RuntimeException e) {
        Throwable cause = e;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause;
    }

    // ---------------------------------------------------------------- resolving what each column does

    private Column[] columnsOfPojo(Map<String, String> columnMappings)
            throws ReflectiveOperationException, NotCompilable {

        final PojoMetadata<?> metadata;
        try {
            metadata = ObjectBuildableFactory.pojoMetadata(targetClass, settings);
        } catch (RuntimeException e) {
            // The metadata cache wraps what the reflective path would have thrown, such as the missing no-argument
            // constructor. Refusing sends the shape back to core, which reports it exactly as it always did.
            throw new NotCompilable("cannot build " + binaryName() + ": " + rootCause(e));
        }
        final NamingConvention convention = settings.getNamingConvention();

        Column[] columns = new Column[columnNames.length];
        for (int i = 0; i < columnNames.length; i++) {
            final String columnName = columnNames[i];
            final Column column = new Column();

            if (columnName.indexOf('.') >= 0) {
                throw new NotCompilable("the column " + columnName + " is dotted, which a reader does not walk into");
            }

            final PojoProperty property = metadata.getPojoProperty(convention.deriveName(columnName), columnMappings);
            if (property == null) {
                // Both of these are read per row rather than refused, so that an error appears at the moment the
                // reflective path raises it: on the first row too.
                column.action = settings.isThrowOnMappingError() ? Action.UNMAPPED_PROPERTY : Action.SKIP;
                column.columnName = columnName;
                columns[i] = column;
                continue;
            }

            final Method setter = property.getSetter();
            final Field field = property.getField();
            if (setter == null && field == null) {
                throw new NotCompilable("the property " + property.getName() + " has neither a setter nor a field");
            }

            // A private member is out of reach from generated code no matter where the reader is defined. Declaring the
            // reader a nest member does not help: the jvm requires the class being reached to list the reader as a nest
            // member, and that list is fixed in the bytes of a class which is already loaded. The reflective path has no
            // such limit because it goes through setAccessible, so a POJO that uses private fields is not slower here, it
            // is simply not compiled.
            final int modifiers = setter != null ? setter.getModifiers() : field.getModifiers();
            if (Modifier.isPrivate(modifiers)) {
                throw new NotCompilable("the property " + property.getName() + " is only reachable through a private "
                        + (setter != null ? "setter" : "field")
                        + ", which generated code cannot reach");
            }

            column.action = setter != null ? Action.SETTER : Action.FIELD;
            column.type = property.getType();
            column.setter = setter;
            column.field = setter == null ? field : null;
            column.description = setter != null
                    ? "property " + property.getName() + " [" + setter.getName() + "] of type "
                            + setter.getDeclaringClass()
                    : "field " + property.getName() + " of type " + field.getDeclaringClass();
            columns[i] = column;
        }
        return columns;
    }

    private Column[] columnsOfRecord() {
        final RecordComponent[] components = targetClass.getRecordComponents();
        final NamingConvention convention = settings.getNamingConvention();

        Column[] columns = new Column[columnNames.length];
        for (int i = 0; i < columnNames.length; i++) {
            final String columnName = columnNames[i];
            final Column column = new Column();
            column.columnName = columnName;

            // A record has no setter and no field to reach for. The reflective path builds one from the component types
            // in declaration order, so the slot of a component is its index there.
            final String derived = convention.deriveName(columnName);
            int slot = -1;
            for (int j = 0; j < components.length && slot < 0; j++) {
                if (convention.deriveName(components[j].getName()).equals(derived)) {
                    slot = j;
                }
            }

            if (slot < 0) {
                // Not a refusal: the reflective path raises this on the row, and it is worth raising identically.
                column.action = Action.UNMAPPED_COMPONENT;
            } else {
                column.action = Action.FIELD;
                column.slot = slot;
                column.type = components[slot].getType();
                column.description = columnName;
            }
            columns[i] = column;
        }
        return columns;
    }

    // ---------------------------------------------------------------- generating

    private byte[] generate(Column[] columns, String digest) throws NotCompilable {
        final ClassWriter writer = new LoaderAwareClassWriter(ClassWriter.COMPUTE_FRAMES, targetClass.getClassLoader());
        writer.visit(Opcodes.V17,
                Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL | Opcodes.ACC_SUPER,
                readersNameFor(digest), null, OBJECT, new String[]{READER});

        final MethodVisitor constructor = writer.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        constructor.visitCode();
        constructor.visitVarInsn(Opcodes.ALOAD, 0);
        constructor.visitMethodInsn(Opcodes.INVOKESPECIAL, OBJECT, "<init>", "()V", false);
        constructor.visitInsn(Opcodes.RETURN);
        constructor.visitMaxs(0, 0);
        constructor.visitEnd();

        final MethodVisitor build = writer.visitMethod(Opcodes.ACC_PUBLIC, "build", BUILD_DESCRIPTOR, null,
                new String[]{SQL_EXCEPTION});
        build.visitCode();

        if (targetClass.isRecord()) {
            generateRecordBody(build, columns);
        } else {
            generatePojoBody(build, columns);
        }

        build.visitMaxs(0, 0);
        build.visitEnd();
        writer.visitEnd();
        return writer.toByteArray();
    }

    private void generatePojoBody(MethodVisitor mv, Column[] columns) throws NotCompilable {
        // Taken from the metadata rather than looked up again, so that this is the very constructor the reflective path
        // would have called, and checked for the same reason a private field is refused.
        final Constructor<?> constructor = ObjectBuildableFactory.pojoMetadata(targetClass, settings).getConstructor();
        if (Modifier.isPrivate(constructor.getModifiers())) {
            throw new NotCompilable("the constructor of " + binaryName() + " is private, which generated code cannot reach");
        }

        mv.visitTypeInsn(Opcodes.NEW, targetClassName);
        mv.visitInsn(Opcodes.DUP);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, targetClassName, "<init>", "()V", false);
        mv.visitVarInsn(Opcodes.ASTORE, LOCAL_TARGET);

        for (int i = 0; i < columns.length; i++) {
            switch (columns[i].action) {
                case SKIP:
                    continue;
                case UNMAPPED_PROPERTY:
                    throwUnmappedProperty(mv, columns[i].columnName);
                    continue;
                default:
                    break;
            }

            readAndConvert(mv, columns[i], i);
            write(mv, columns[i]);
        }

        mv.visitVarInsn(Opcodes.ALOAD, LOCAL_TARGET);
        mv.visitInsn(Opcodes.ARETURN);
    }

    private void generateRecordBody(MethodVisitor mv, Column[] columns) {
        final Constructor<?> canonical = targetClass.getDeclaredConstructors()[0];
        final Class<?>[] parameters = canonical.getParameterTypes();

        push(mv, parameters.length);
        mv.visitTypeInsn(Opcodes.ANEWARRAY, OBJECT);
        mv.visitVarInsn(Opcodes.ASTORE, LOCAL_TARGET);

        for (int i = 0; i < columns.length; i++) {
            if (columns[i].action == Action.UNMAPPED_COMPONENT) {
                throwUnmappedComponent(mv, columns[i].columnName);
                continue;
            }

mv.visitVarInsn(Opcodes.ALOAD, LOCAL_TARGET);
            push(mv, columns[i].slot);
            readAndConvert(mv, columns[i], i);
            mv.visitVarInsn(Opcodes.ALOAD, LOCAL_VALUE);
            mv.visitInsn(Opcodes.AASTORE);
        }

        mv.visitTypeInsn(Opcodes.NEW, targetClassName);
        mv.visitInsn(Opcodes.DUP);
        for (int j = 0; j < parameters.length; j++) {
            mv.visitVarInsn(Opcodes.ALOAD, LOCAL_TARGET);
            push(mv, j);
            mv.visitInsn(Opcodes.AALOAD);
            unboxForConstructor(mv, parameters[j]);
        }
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, targetClassName, "<init>",
                voidDescriptorOf(parameters), false);
        mv.visitInsn(Opcodes.ARETURN);
    }

    /**
     * Reads one column through the quirks, converts it, and leaves the result in {@link #LOCAL_VALUE}.
     *
     * <p>Through the quirks and not straight from the result set, which is the part that cannot be left to the obvious
     * thing to write: {@code OracleQuirks} replaces the timestamps oracle hands out and {@code PostgresQuirks} asks for an
     * {@code OffsetTime} where the column carries a zone. Reading through the result set would map those columns wrongly
     * and say nothing about having done so.
     *
     * <p>Both halves go through a local rather than staying on the stack, because the two calls want their arguments in
     * opposite positions: the converter is under the value for the one in {@code ReaderSupport} and nothing at all for the
     * read through the quirks, where the quirks are the argument. Nothing can be kept under anything else on a stack.
     */
    private void readAndConvert(MethodVisitor mv, Column column, int index) {
        mv.visitVarInsn(Opcodes.ALOAD, 3);
        push(mv, index);
        mv.visitInsn(Opcodes.AALOAD);
        mv.visitVarInsn(Opcodes.ASTORE, LOCAL_CONVERTER);

        // The quirks are the receiver of the call and the result set and the index its arguments, so they are pushed in
        // that order: a call reads its arguments off the top of the stack backwards, and the last one pushed is the one
        // it expects first.
        mv.visitVarInsn(Opcodes.ALOAD, 2);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        push(mv, index + 1);
        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, QUIRKS, "getRSVal",
                "(" + objectType(RESULT_SET) + "I)" + objectType(OBJECT), true);
        mv.visitVarInsn(Opcodes.ASTORE, LOCAL_VALUE);

        mv.visitVarInsn(Opcodes.ALOAD, LOCAL_CONVERTER);
        mv.visitVarInsn(Opcodes.ALOAD, LOCAL_VALUE);
        if (targetClass.isRecord()) {
            mv.visitLdcInsn(column.columnName);
            // The rendering of Class.toString rather than the name: the reflective path concatenates the Class
            // itself, and "class java.lang.String" is not "java.lang.String". Computed here, so the row pays nothing.
            mv.visitLdcInsn(column.type.toString());
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, SUPPORT, "convertRecordComponent",
                    "(" + objectType(CONVERTER) + objectType(OBJECT) + objectType("java/lang/String")
                            + objectType("java/lang/String") + ")" + objectType(OBJECT), false);
        } else {
            mv.visitLdcInsn(column.description);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, SUPPORT, "convertProperty",
                    "(" + objectType(CONVERTER) + objectType(OBJECT) + objectType("java/lang/String") + ")"
                            + objectType(OBJECT), false);
        }
        mv.visitVarInsn(Opcodes.ASTORE, LOCAL_VALUE);
    }

    /** Puts the value in {@link #LOCAL_VALUE} into the field or setter this column resolved to. */
    private void write(MethodVisitor mv, Column column) {
        mv.visitVarInsn(Opcodes.ALOAD, LOCAL_TARGET);
        mv.visitVarInsn(Opcodes.ALOAD, LOCAL_VALUE);

        final Class<?> type = column.type;
        if (type.isPrimitive()) {
            // A null must not reach a primitive: the reflective path returns before assigning rather than unboxing one,
            // which would leave the field at whatever it already was. The write is inside the branch rather than after
            // the jump, so that the null path reaches the end label with nothing left to do.
            final Label writeHere = new Label();
            final Label end = new Label();
            mv.visitInsn(Opcodes.DUP);
            mv.visitJumpInsn(Opcodes.IFNONNULL, writeHere);
            mv.visitInsn(Opcodes.POP);
            mv.visitInsn(Opcodes.POP);
            mv.visitJumpInsn(Opcodes.GOTO, end);

            mv.visitLabel(writeHere);
            unbox(mv, type);
            assign(mv, column, type);

            mv.visitLabel(end);
            return;
        }

        mv.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(type));
        assign(mv, column, type);
    }

    private void assign(MethodVisitor mv, Column column, Class<?> type) {
        if (column.action == Action.SETTER) {
            final Method setter = column.setter;
            // Always a virtual call: the metadata only ever hands over a method declared by a class — core reads
            // declared methods, never inherited ones — so the owner here is a class and not an interface.
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                    Type.getInternalName(setter.getDeclaringClass()),
                    setter.getName(),
                    voidDescriptorOf(setter.getParameterTypes()),
                    false);
        } else {
            final Field field = column.field;
            mv.visitFieldInsn(Opcodes.PUTFIELD,
                    Type.getInternalName(field.getDeclaringClass()),
                    field.getName(),
                    Type.getDescriptor(field.getType()));
        }
    }

    /**
     * An argument of a record constructor, where a null for a primitive is what the reflective path refuses.
     *
     * <p>{@code Constructor.newInstance} answers a null for a primitive with an {@link IllegalArgumentException} saying
     * the argument does not match, and raising that here rather than unboxing it is what keeps a caller from seeing an
     * {@link NullPointerException} where the reflective path would have said so plainly.
     */
    private void unboxForConstructor(MethodVisitor mv, Class<?> type) {
        if (!type.isPrimitive()) {
            mv.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(type));
            return;
        }
        final Label notNull = new Label();
        final Label end = new Label();
        mv.visitInsn(Opcodes.DUP);
        mv.visitJumpInsn(Opcodes.IFNONNULL, notNull);
        mv.visitInsn(Opcodes.POP);
        mv.visitLdcInsn("argument type mismatch");
        mv.visitTypeInsn(Opcodes.NEW, ILLEGAL_ARGUMENT);
        mv.visitInsn(Opcodes.DUP);
        mv.visitLdcInsn("argument type mismatch");
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, ILLEGAL_ARGUMENT, "<init>", "(Ljava/lang/String;)V", false);
        mv.visitInsn(Opcodes.ATHROW);
        mv.visitLabel(notNull);
        unbox(mv, type);
        mv.visitLabel(end);
    }

    private static void unbox(MethodVisitor mv, Class<?> type) {
        final Class<?> wrapper = wrapperOf(type);
        mv.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(wrapper));
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                Type.getInternalName(wrapper),
                primitiveAccessor(type),
                "()" + Type.getDescriptor(type),
                false);
    }

    private void throwUnmappedProperty(MethodVisitor mv, String columnName) {
        final String message = "Could not map " + columnName + " to any property.";
        mv.visitLdcInsn(message);
        mv.visitTypeInsn(Opcodes.NEW, MAPPING_EXCEPTION);
        mv.visitInsn(Opcodes.DUP);
        mv.visitLdcInsn(message);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, MAPPING_EXCEPTION, "<init>", "(Ljava/lang/String;)V", false);
        mv.visitInsn(Opcodes.ATHROW);
    }

    private void throwUnmappedComponent(MethodVisitor mv, String columnName) {
        final String message = "No such field in record: " + columnName;
        mv.visitLdcInsn(message);
        mv.visitTypeInsn(Opcodes.NEW, ILLEGAL_ARGUMENT);
        mv.visitInsn(Opcodes.DUP);
        mv.visitLdcInsn(message);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, ILLEGAL_ARGUMENT, "<init>", "(Ljava/lang/String;)V", false);
        mv.visitInsn(Opcodes.ATHROW);
    }

    private static String primitiveAccessor(Class<?> type) {
        if (type == boolean.class) {
            return "booleanValue";
        }
        if (type == byte.class) {
            return "byteValue";
        }
        if (type == char.class) {
            return "charValue";
        }
        if (type == short.class) {
            return "shortValue";
        }
        if (type == int.class) {
            return "intValue";
        }
        if (type == long.class) {
            return "longValue";
        }
        if (type == float.class) {
            return "floatValue";
        }
        return "doubleValue";
    }

    private static Class<?> wrapperOf(Class<?> type) {
        if (type == boolean.class) {
            return Boolean.class;
        }
        if (type == byte.class) {
            return Byte.class;
        }
        if (type == char.class) {
            return Character.class;
        }
        if (type == short.class) {
            return Short.class;
        }
        if (type == int.class) {
            return Integer.class;
        }
        if (type == long.class) {
            return Long.class;
        }
        if (type == float.class) {
            return Float.class;
        }
        return Double.class;
    }

    // ---------------------------------------------------------------- defining


    private String readersNameFor(String digest) {
        return targetClass.getPackageName().replace('.', '/') + "/" + READER_PREFIX + "$" + digest;
    }

    /**
     * A digest of the shape, so that the same shape is recognised rather than compiled twice and a failure to define says
     * something about the name rather than about the machinery.
     *
     * <p>The class is the first thing in it, and it has to be: two classes in the same package would otherwise produce the
     * same name, and defining the second would collide with the first and be answered with a reader built for the wrong
     * class. The settings that shape it are those which change what a column is bound to. The quirks are not among them:
     * they are consulted per call, so a shape compiled under one quirks serves every other.
     */
    private String digestOf(Map<String, String> columnMappings) {
        final StringBuilder shape = new StringBuilder();
        shape.append(binaryName().length()).append(':').append(binaryName()).append('|');
        for (String columnName : columnNames) {
            shape.append(columnName.length()).append(':').append(columnName).append(';');
        }
        shape.append('|');
        shape.append(settings.getNamingConvention().hashCode()).append('|');
        shape.append(settings.isThrowOnMappingError()).append('|');
        columnMappings.keySet().stream().sorted()
                .forEach(key -> shape.append(key.length()).append(':').append(key)
                        .append('=').append(String.valueOf(columnMappings.get(key))).append(';'));
        try {
            final byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(shape.toString().getBytes(StandardCharsets.UTF_8));
            final StringBuilder hex = new StringBuilder(16);
            for (int i = 0; i < 8; i++) {
                hex.append(String.format("%02x", digest[i]));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("every jvm is required to have SHA-256", e);
        }
    }

    private RowReader define(String binaryName, byte[] bytes, String digest) throws NotCompilable {
        try {
            return (RowReader) access.lookup().defineClass(bytes).getDeclaredConstructor().newInstance();
        } catch (LinkageError alreadyDefined) {
            return adoptExisting(binaryName, alreadyDefined);
        } catch (ReflectiveOperationException | ClassCastException e) {
            throw new NotCompilable("cannot use the reader " + binaryName + ": " + e);
        }
    }

    /**
     * The reader for a shape that was compiled before and whose class outlived the cache entry for it.
     *
     * <p>The name carries a digest of the shape, class and columns and settings alike, so a clash is the same shape rather
     * than a different one and the class already loaded is the right one. A name held by something that is not a reader
     * would mean two shapes sharing a digest, which is refused rather than guessed at.
     */
    private RowReader adoptExisting(String binaryName, LinkageError clash) throws NotCompilable {
        try {
            final Class<?> existing = Class.forName(binaryName, false, targetClass.getClassLoader());
            return (RowReader) existing.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException | ClassCastException e) {
            throw new NotCompilable("the name " + binaryName + " is already taken by something else: " + clash);
        }
    }

    // ---------------------------------------------------------------- constants

/**
 * A reference type as it appears in a descriptor.
 *
 * <p>Every descriptor here goes through this rather than being written out by hand. An internal name has no leading
 * {@code L} and no trailing {@code ;}, and a descriptor missing either of them is not rejected until the class writer
 * parses it, several steps from the line that got it wrong.
 */
private static String objectType(String internalName) {
        return "L" + internalName + ";";
    }

    /** A method descriptor for a method that returns nothing, which is every write the reader makes. */
    private static String voidDescriptorOf(Class<?>[] parameterTypes) {
        final Type[] types = new Type[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; i++) {
            types[i] = Type.getType(parameterTypes[i]);
        }
        return Type.getMethodDescriptor(Type.VOID_TYPE, types);
    }

    private static void push(MethodVisitor mv, int value) {
        if (value >= -1 && value <= 5) {
            mv.visitInsn(Opcodes.ICONST_0 + value);
        } else if (value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE) {
            mv.visitIntInsn(Opcodes.BIPUSH, value);
        } else if (value >= Short.MIN_VALUE && value <= Short.MAX_VALUE) {
            mv.visitIntInsn(Opcodes.SIPUSH, value);
        } else {
            mv.visitLdcInsn(value);
        }
    }

    /**
     * A class writer that resolves a common superclass through the loader of the target class.
     *
     * <p>The one in asm resolves through its own loader, which is right for the library and wrong here: the POJO is
     * frequently loaded by something else entirely, a container or a plugin, and a lookup that cannot find it is a
     * compilation failure over a class this extension never mentions.
     */
    private static final class LoaderAwareClassWriter extends ClassWriter {

        private final ClassLoader loader;

        LoaderAwareClassWriter(int flags, ClassLoader loader) {
            super(flags);
            this.loader = loader != null ? loader : LoaderAwareClassWriter.class.getClassLoader();
        }

        @Override
        protected String getCommonSuperClass(String type1, String type2) {
            try {
                Class<?> first = classFor(type1);
                Class<?> second = classFor(type2);
                if (first.isAssignableFrom(second)) {
                    return type1;
                }
                if (second.isAssignableFrom(first)) {
                    return type2;
                }
                if (first.isInterface() || second.isInterface()) {
                    return OBJECT;
                }
                do {
                    first = first.getSuperclass();
                } while (first != null && !first.isAssignableFrom(second));
                return first == null ? OBJECT : Type.getInternalName(first);
            } catch (ClassNotFoundException | RuntimeException e) {
                return OBJECT;
            }
        }

        private Class<?> classFor(String internalName) throws ClassNotFoundException {
            return Class.forName(Type.getObjectType(internalName).getClassName(), false, loader);
        }
    }

}
