package org.sql2o;

import org.sql2o.quirks.Quirks;
import org.sql2o.reflection2.ObjectBuildableFactoryDelegate;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

public class DefaultResultSetHandlerFactory<T> implements ResultSetHandlerFactory<T> {
    private final Quirks quirks;
    private final ObjectBuildableFactoryDelegate<T> objectBuilderDelegate;

    public DefaultResultSetHandlerFactory(ObjectBuildableFactoryDelegate<T> objectBuilderDelegate, Quirks quirks) {
        this.objectBuilderDelegate = objectBuilderDelegate;
        this.quirks = quirks;
    }


    @SuppressWarnings("unchecked")
    public ResultSetHandler<T> newResultSetHandler(final ResultSetMetaData meta) throws SQLException {
        // The names of the columns are a property of the result set and not of the row being read, so they are asked
        // for once here rather than on every row, where this used to make a metadata call per column per row.
        final int columnCount = meta.getColumnCount();
        final String[] columnNames = new String[columnCount];
        for (int i = 0; i < columnCount; i++) {
            columnNames[i] = quirks.getColumnName(meta, i + 1);
        }

        return resultSet -> {

            final var objectBuilder = objectBuilderDelegate.newObjectBuilder();

            for (int i = 0; i < columnCount; i++) {
                final var colName = columnNames[i];
                try {
                    objectBuilder.withValue(colName, quirks.getRSVal(resultSet, i + 1));
                } catch (ReflectiveOperationException e) {
                    throw new Sql2oException("Error when trying to set value for column [" + colName + "]", e);
                }

            }
            try {
                return objectBuilder.build();
            } catch (ReflectiveOperationException e) {
                throw new Sql2oException("Error occurred while creating object from ResultSet", e);
            }
        };
    }
}
