package org.sql2o;

import org.sql2o.converters.Convert;
import org.sql2o.converters.Converter;
import org.sql2o.converters.ConverterException;
import org.sql2o.quirks.Quirks;
import org.sql2o.reflection.Pojo;
import org.sql2o.reflection.PojoMetadata;
import org.sql2o.reflection.Setter;
import org.sql2o.tools.ResultSetUtils;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

/**
 * Iterator for a {@link java.sql.ResultSet}. Tricky part here is getting {@link #hasNext()}
 * to work properly, meaning it can be called multiple times without calling {@link #next()}.
 *
 * @author aldenquimby@gmail.com
 */
public class PojoResultSetIterator<T> extends ResultSetIteratorBase<T> {
    private ResultSetHandler<T> handler;

    public PojoResultSetIterator(ResultSet rs, boolean isCaseSensitive, Quirks quirks, PojoMetadata metadata) {
        super(rs, isCaseSensitive, quirks);
        try {
            this.handler = newResultSetHandler(rs.getMetaData(), isCaseSensitive, metadata);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private static Setter getSetter(
            final String propertyPath,
            final PojoMetadata metadata,
            final boolean isCaseSensitive){
        int index = propertyPath.indexOf('.');
        if(index<=0){
            // Simple path - fast way
            final Setter setter = metadata.getPropertySetterIfExists(propertyPath);
            // behavior change: do not throw if POJO contains less properties
            if(setter==null) return null;
            final Converter converter=Convert.getConverterIfExists(setter.getType());
            // setter without converter
            if(converter==null) return setter;
            return new Setter() {
                public void setProperty(Object obj, Object value) {
                    try {
                        setter.setProperty(obj, converter.convert( value ));
                    } catch (ConverterException e) {
                        throw new Sql2oException("Error trying to convert column " + propertyPath + " to type " + setter.getType(), e);
                    }
                }
                public Class getType() {
                    return setter.getType();
                }
            };
        }
        // dot path - long way
        // i'm too lazy now to rewrite this case so I just call old unoptimized code...
        // TODO: rewrite, get rid of POJO class
        return new Setter() {
            public void setProperty(Object obj, Object value) {
                Pojo pojo = new Pojo(metadata, isCaseSensitive, obj);
                pojo.setProperty(propertyPath, value);
            }

            public Class getType() {
                // doesn't used anyway
                return Object.class;
            }
        };
    }

    private <T> ResultSetHandler<T> newResultSetHandler(final ResultSetMetaData meta, boolean isCaseSensitive, final PojoMetadata metadata){
        final Setter[] setters;
        final Converter converter;
        final boolean useExecuteScalar;
        //TODO: it's possible to cache converter/setters
        // cache key is ResultSetMetadata + Bean type

        converter = Convert.getConverterIfExists(metadata.getType());
        try {
            int colCount = meta.getColumnCount();
            setters=new Setter[colCount+1];   // setters[0] is always null
            for (int i = 1; i <= colCount; i++) {
                String colName = getColumnName(i);
                // behavior change: do not throw if POJO contains less properties
                setters[i] = getSetter(colName, metadata, isCaseSensitive);
            }
            /**
             * Fallback to executeScalar if converter exists,
             * we're selecting 1 column, and no property setter exists for the column.
             */
            useExecuteScalar = converter !=null && colCount==1 && setters[1]==null;
        } catch (SQLException ex) {
            throw new Sql2oException("Database error: " + ex.getMessage(), ex);
        }
        return new ResultSetHandler<T>() {
            @SuppressWarnings("unchecked")
            public T handle(ResultSet resultSet) throws SQLException {
                if (useExecuteScalar) {
                    try {
                        return (T)converter.convert(ResultSetUtils.getRSVal(rs, 1));
                    }
                    catch (ConverterException e) {
                        throw new Sql2oException("Error occurred while converting value from database to type " + metadata.getType(), e);
                    }
                }

                // otherwise we want executeAndFetch with object mapping
                Object pojo = metadata.getObjectConstructor().newInstance();
                for(int colIdx = 1; colIdx <= meta.getColumnCount(); colIdx++) {
                    Setter setter = setters[colIdx];
                    if(setter==null) continue;
                    setter.setProperty(pojo,ResultSetUtils.getRSVal(rs, colIdx));
                }

                return (T)pojo;
            }
        };

    }

    @Override
    protected T readNext() throws SQLException {
        return handler.handle(rs);
    }

}
