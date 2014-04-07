package org.sql2o;

import org.sql2o.converters.Convert;
import org.sql2o.converters.Converter;
import org.sql2o.converters.ConverterException;
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

    @SuppressWarnings("unchecked")
    public PojoResultSetIterator(ResultSet rs, boolean isCaseSensitive, QuirksMode quirksMode, ResultSetHandlerFactory<T> factory) {
        super(rs, isCaseSensitive, quirksMode);
        try {
            this.handler = factory.newResultSetHandler(rs.getMetaData());
        } catch (SQLException e) {
            throw new Sql2oException("Database error: " + e.getMessage(), e);
        }
    }

    public PojoResultSetIterator(ResultSet rs, boolean isCaseSensitive, QuirksMode quirksMode, ResultSetHandler<T> handler) {
        super(rs, isCaseSensitive, quirksMode);
        this.handler = handler;
    }

    @Override
    protected T readNext() throws SQLException {
        return handler.handle(rs);
    }

}
