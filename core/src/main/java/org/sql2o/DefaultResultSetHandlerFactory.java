package org.sql2o;

import org.sql2o.converters.Convert;
import org.sql2o.converters.Converter;
import org.sql2o.converters.ConverterException;
import org.sql2o.quirks.Quirks;
import org.sql2o.reflection.Pojo;
import org.sql2o.reflection.PojoMetadata;
import org.sql2o.reflection.Setter;
import org.sql2o.tools.AbstractCache;
import org.sql2o.tools.FeatureDetector;
import org.sql2o.tools.ResultSetUtils;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;


public class DefaultResultSetHandlerFactory<T> implements ResultSetHandlerFactory<T> {
    private final PojoMetadata metadata;
    private final Quirks quirks;

    public DefaultResultSetHandlerFactory(PojoMetadata pojoMetadata, Quirks quirks) {
        this.metadata = pojoMetadata;
        this.quirks = quirks;
    }

    private static Setter getSetter(
            final String propertyPath,
            final PojoMetadata metadata) {
        int index = propertyPath.indexOf('.');
        if (index <= 0) {
            // Simple path - fast way
            final Setter setter = metadata.getPropertySetterIfExists(propertyPath);
            // behavior change: do not throw if POJO contains less properties
            if (setter == null) return null;
            final Converter converter = Convert.getConverterIfExists(setter.getType());
            // setter without converter
            if (converter == null) return setter;
            return new Setter() {
                public void setProperty(Object obj, Object value) {
                    try {
                        setter.setProperty(obj, converter.convert(value));
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
                Pojo pojo = new Pojo(metadata, metadata.isCaseSensitive(), obj);
                pojo.setProperty(propertyPath, value);
            }

            public Class getType() {
                // doesn't used anyway
                return Object.class;
            }
        };
    }

    private static class Key {
        final String stringKey;
        final DefaultResultSetHandlerFactory f;

        DefaultResultSetHandlerFactory factory(){
            return f;
        }

        private PojoMetadata getMetadata() {
            return f.metadata;
        }

        private Quirks getQuirksMode() {
            return f.quirks;
        }

        private Key(String stringKey, DefaultResultSetHandlerFactory f) {
            this.stringKey = stringKey;
            this.f = f;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Key key = (Key) o;

            if (!f.metadata.equals(key.getMetadata())) return false;
            if (f.quirks != key.getQuirksMode()) return false;
            if (!stringKey.equals(key.stringKey)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = f.metadata.hashCode();
            result = 31 * result + f.quirks.hashCode();
            result = 31 * result + stringKey.hashCode();
            return result;
        }
    }


    private static final AbstractCache<Key,ResultSetHandler,ResultSetMetaData>
     c = new AbstractCache<Key, ResultSetHandler, ResultSetMetaData>() {
        @Override
        protected ResultSetHandler evaluate(Key key, ResultSetMetaData param) {
            try {
                return key.factory().newResultSetHandler0(param);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    };

    @SuppressWarnings("unchecked")
    public ResultSetHandler<T> newResultSetHandler(final ResultSetMetaData meta) throws SQLException {
        if(FeatureDetector.isCachePojoMetaDataEnabled()){
            StringBuilder stringBuilder = new StringBuilder();
            for (int i = 1; i <= meta.getColumnCount(); i++) {
                stringBuilder.append(quirks.getColumnName(meta,i)).append("\n");
            }
            return c.get(new Key(stringBuilder.toString(), this),meta);
        } else {
            return newResultSetHandler0(meta);
        }
    }


    private  <T> ResultSetHandler<T> newResultSetHandler0(final ResultSetMetaData meta) throws SQLException {
        final Setter[] setters;
        final Converter converter;
        final boolean useExecuteScalar;
        //TODO: it's possible to cache converter/setters
        // cache key is ResultSetMetadata + Bean type

        converter = Convert.getConverterIfExists(metadata.getType());
        final int columnCount = meta.getColumnCount();

        setters = new Setter[columnCount + 1];   // setters[0] is always null
        for (int i = 1; i <= columnCount; i++) {
            String colName = quirks.getColumnName(meta, i);
            // behavior change: do not throw if POJO contains less properties
            setters[i] = getSetter(colName, metadata);
        }
        /**
         * Fallback to executeScalar if converter exists,
         * we're selecting 1 column, and no property setter exists for the column.
         */
        useExecuteScalar = converter != null && columnCount == 1 && setters[1] == null;
        return new ResultSetHandler<T>() {
            @SuppressWarnings("unchecked")
            public T handle(ResultSet resultSet) throws SQLException {
                if (useExecuteScalar) {
                    try {
                        return (T) converter.convert(ResultSetUtils.getRSVal(resultSet, 1));
                    } catch (ConverterException e) {
                        throw new Sql2oException("Error occurred while converting value from database to type " + metadata.getType(), e);
                    }
                }

                // otherwise we want executeAndFetch with object mapping
                Object pojo = metadata.getObjectConstructor().newInstance();
                for (int colIdx = 1; colIdx <= columnCount; colIdx++) {
                    Setter setter = setters[colIdx];
                    if (setter == null) continue;
                    setter.setProperty(pojo, ResultSetUtils.getRSVal(resultSet, colIdx));
                }

                return (T) pojo;
            }
        };
    }
}
