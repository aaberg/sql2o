package org.sql2o;

import org.sql2o.converters.Convert;
import org.sql2o.converters.Converter;
import org.sql2o.converters.ConverterException;
import org.sql2o.reflection.Pojo;
import org.sql2o.reflection.PojoMetadata;
import org.sql2o.tools.ResultSetUtils;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Iterator for a {@link java.sql.ResultSet}. Tricky part here is getting {@link #hasNext()}
 * to work properly, meaning it can be called multiple times without calling {@link #next()}.
 *
 * @author aldenquimby@gmail.com
 */
public class ResultSetIterator<T> implements Iterator<T> {
    // fields needed to read result set
    private ResultSet rs;
    private ResultSetMetaData meta;
    private PojoMetadata metadata;
    private boolean isCaseSensitive;
    private QuirksMode quirksMode;
    private Converter converter;
    private boolean useExecuteScalar;

    public ResultSetIterator(ResultSet rs, PojoMetadata metadata, boolean isCaseSensitive, QuirksMode quirksMode) {
        this.rs = rs;
        this.metadata = metadata;
        this.isCaseSensitive = isCaseSensitive;
        this.quirksMode = quirksMode;
        try {
            meta = rs.getMetaData();
        }
        catch(SQLException ex) {
            throw new Sql2oException("Database error: " + ex.getMessage(), ex);
        }
        this.converter = Convert.getConverterIfExists(metadata.getType());
        this.useExecuteScalar = shouldTryExecuteScalar();
    }

    // fields needed to properly implement
    private T next; // keep track of next item in case hasNext() is called multiple times
    private boolean resultSetFinished; // used to note when result set exhausted

    public boolean hasNext() {
        // check if we already fetched next item
        if (next != null) {
            return true;
        }

        // check if result set already finished
        if (resultSetFinished) {
            return false;
        }

        // now fetch next item
        next = readNext();

        // check if we got something
        if (next != null) {
            return true;
        }

        // no more items
        resultSetFinished = true;

        return false;
    }

    public T next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }

        T result = next;

        next = null;

        return result;
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings("unchecked") // Convert should be refactored so that this isn't needed
    private T readNext() {
        try {
            if (rs.next()) {

                if (useExecuteScalar) {
                    return (T)converter.convert(ResultSetUtils.getRSVal(rs, 1));
                }

                // otherwise we want executeAndFetch with object mapping
                Pojo pojo = new Pojo(metadata, isCaseSensitive);

                for(int colIdx = 1; colIdx <= meta.getColumnCount(); colIdx++) {
                    String colName = getColumnName(colIdx);
                    pojo.setProperty(colName, ResultSetUtils.getRSVal(rs, colIdx));
                }

                return (T)pojo.getObject();
            }
        }
        catch (SQLException ex) {
            throw new Sql2oException("Database error: " + ex.getMessage(), ex);
        }
        catch (ConverterException e) {
            throw new Sql2oException("Error occurred while converting value from database to type " + metadata.getType(), e);
        }
        return null;
    }

    private String getColumnName(int colIdx) throws SQLException {
        if (quirksMode == QuirksMode.DB2){
            return meta.getColumnName(colIdx);
        }
        else {
            return meta.getColumnLabel(colIdx);
        }
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
