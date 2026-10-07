package org.sql2o.bytecode;

import org.sql2o.Sql2oException;
import org.sql2o.converters.Converter;
import org.sql2o.converters.ConverterException;

/**
 * What generated code calls into, rather than implementing conversion itself.
 *
 * <p>Every call from a generated reader into this extension goes through here, and there are exactly two kinds: reading a
 * column through the quirks, and converting what came out of it. The conversion is left in the {@link Converter} api on
 * purpose — a converter may do anything at all, and having generated code call it keeps one implementation of "what does
 * this column mean" instead of two that have to agree.
 *
 * <p>The error messages are worded exactly as the reflective path words them, in
 * {@link org.sql2o.reflection2.PojoProperty#SetProperty} and {@link org.sql2o.reflection2.RecordBuilder}, because a
 * caller should not be able to tell which path mapped its row by the wording of an error.
 */
public final class ReaderSupport {

    private ReaderSupport() {
    }

    /**
     * Converts a value on its way into a property of a POJO.
     *
     * @param converter   the converter for the type of the property, never null
     * @param value       what the column gave, which may be null
     * @param description where the value is going, worded the way the reflective path words it, for instance
     *                    {@code property id [setId] of type com.example.Person} or
     *                    {@code field id of type com.example.Person}
     * @return the converted value, possibly null
     */
    public static Object convertProperty(Converter<?> converter, Object value, String description) {
        try {
            return converter.convert(value);
        } catch (ConverterException e) {
            throw new Sql2oException("Error trying to convert value of type " + typeOf(value) + " to " + description, e);
        }
    }

    /**
     * Converts a value on its way into a component of a record, which the reflective path words differently from a POJO
     * property and so cannot share the method above.
     *
     * @param converter the converter for the type of the component, never null
     * @param value     what the column gave, which may be null
     * @param column    the name of the column, as the reflective path names it
     * @param typeName  the name of the type of the component
     * @return the converted value, possibly null
     */
    public static Object convertRecordComponent(Converter<?> converter, Object value, String column, String typeName) {
        try {
            return converter.convert(value);
        } catch (ConverterException e) {
            throw new Sql2oException("Error trying to convert column " + column + " to type " + typeName, e);
        }
    }

    /**
     * The name of the type of a value for an error message.
     *
     * <p>The reflective path writes {@code value.getClass().getName()} and so throws a {@link NullPointerException} out of
     * the catch block when a converter fails on a null. That is not reproduced here: a message about a null saying
     * {@code null} is better than a stack trace pointing at the message.
     */
    private static String typeOf(Object value) {
        return value == null ? "null" : value.getClass().getName();
    }
}