package org.sql2o;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import org.sql2o.quirks.Quirks;

/**
 * Iterator for a {@link java.sql.ResultSet}. Tricky part here is getting {@link #hasNext()}
 * to work properly, meaning it can be called multiple times without calling {@link #next()}.
 *
 * @author aldenquimby@gmail.com
 */
public abstract class ResultSetIteratorBase<T> implements Iterator<T> {
    // fields needed to read result set
    protected ResultSet rs;
    protected boolean isCaseSensitive;
    protected Quirks quirks;
    protected ResultSetMetaData meta;

    public ResultSetIteratorBase(ResultSet rs, boolean isCaseSensitive, Quirks quirks) {
        this.rs = rs;
        this.isCaseSensitive = isCaseSensitive;
        this.quirks = quirks;
        try {
            meta = rs.getMetaData();
        }
        catch(SQLException ex) {
            throw new Sql2oException("Database error: " + ex.getMessage(), ex);
        }
    }

    // fields needed to properly implement
    private ResultSetValue<T> next; // keep track of next item in case hasNext() is called multiple times
    private boolean resultSetFinished; // used to note when result set exhausted

    @Override
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
        next = safeReadNext();

        // check if we got something
        if (next != null) {
            return true;
        }

        // no more items
        resultSetFinished = true;

        return false;
    }

    @Override
    public T next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }

        T result = next.value;

        next = null;

        return result;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    private ResultSetValue<T> safeReadNext()
    {
        try {
            if (!rs.next())
                return null;

            @SuppressWarnings("unchecked")
            ResultSetValue<T> resultSetValue = new <T>ResultSetValue(readNext());
            return resultSetValue;
        }
        catch (SQLException ex) {
            throw new Sql2oException("Database error: " + ex.getMessage(), ex);
        }
    }

    protected abstract T readNext() throws SQLException;

    protected String getColumnName(int colIdx) throws SQLException {
        return quirks.getColumnName(meta, colIdx);
    }

    private final class ResultSetValue<T> {
        public final T value;

        public ResultSetValue(T value){
            this.value = value;
        }
    }
}
