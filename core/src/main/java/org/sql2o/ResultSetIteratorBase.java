package org.sql2o;

import org.sql2o.quirks.Quirks;

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
 * @param <T> the type of the objects that this iterator will return
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

    // Fields needed to properly implement: the prefetched value, if there is one, and whether the result
    // set is exhausted. A null value is a value like any other — only rs.next() saying false ends the iteration —
    // so the flag and not the value tracks whether a row was prefetched. This used to wrap every row in a holder
    // object, allocating one per row for nothing.
    private T next;
    private boolean hasPrefetch;
    private boolean resultSetFinished; // used to note when result set exhausted

    public boolean hasNext() {
        // check if we already fetched next item
        if (hasPrefetch) {
            return true;
        }

        // check if result set already finished
        if (resultSetFinished) {
            return false;
        }

        // now fetch next item
        try {
            if (!rs.next()) {
                // no more items
                resultSetFinished = true;
                return false;
            }
            next = readNext();
            hasPrefetch = true;
            return true;
        }
        catch (SQLException ex) {
            throw new Sql2oException("Database error: " + ex.getMessage(), ex);
        }
    }

    public T next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }

        T result = next;

        next = null;
        hasPrefetch = false;

        return result;
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }

    protected abstract T readNext() throws SQLException;

    protected String getColumnName(int colIdx) throws SQLException {
        return quirks.getColumnName(meta, colIdx);
    }
}
