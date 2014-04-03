package org.sql2o;

import org.sql2o.converters.Convert;
import org.sql2o.converters.Converter;
import org.sql2o.converters.ConverterException;
import org.sql2o.reflection.Pojo;
import org.sql2o.reflection.PojoMetadata;
import org.sql2o.tools.ResultSetUtils;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Iterator for a {@link java.sql.ResultSet}. Tricky part here is getting {@link #hasNext()}
 * to work properly, meaning it can be called multiple times without calling {@link #next()}.
 *
 * @author aldenquimby@gmail.com
 */
public class PojoResultSetIterator<T> extends ResultSetIteratorBase<T> {
    private PojoMetadata metadata;
    private Converter converter;
    private boolean useExecuteScalar;

    public PojoResultSetIterator(ResultSet rs, boolean isCaseSensitive, QuirksMode quirksMode, PojoMetadata metadata) {
        super(rs, isCaseSensitive, quirksMode);

        this.metadata = metadata;
        this.converter = Convert.getConverterIfExists(metadata.getType());
        this.useExecuteScalar = shouldTryExecuteScalar();
    }

    @SuppressWarnings("unchecked") // Convert should be refactored so that this isn't needed
    @Override
    protected T readNext() throws SQLException {
        if (useExecuteScalar) {
            try {
                return (T)converter.convert(ResultSetUtils.getRSVal(rs, 1));
            }
            catch (ConverterException e) {
                throw new Sql2oException("Error occurred while converting value from database to type " + metadata.getType(), e);
            }
        }

        // otherwise we want executeAndFetch with object mapping
        Pojo pojo = new Pojo(metadata, isCaseSensitive);
        for(int colIdx = 1; colIdx <= meta.getColumnCount(); colIdx++) {
            String colName = getColumnName(colIdx);
            Object value = ResultSetUtils.getRSVal(rs, colIdx);
            pojo.setProperty(colName, value);
        }

        return (T)pojo.getObject();
    }

    /**
     * Fallback to executeScalar if converter exists,
     * we're selecting 1 column, and no property setter exists for the column.
     */
    private boolean shouldTryExecuteScalar() {
        try {
            return converter != null &&
                   meta.getColumnCount() == 1 &&
                   metadata.getPropertySetterIfExists(getColumnName(1)) == null;
        }
        catch (SQLException ex) {
            throw new Sql2oException("Database error: " + ex.getMessage(), ex);
        }
    }
}
