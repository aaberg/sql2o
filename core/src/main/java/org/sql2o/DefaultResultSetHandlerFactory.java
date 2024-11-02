package org.sql2o;

import org.sql2o.quirks.Quirks;
import org.sql2o.reflection2.ObjectBuildableFactoryDelegate;
import java.sql.ResultSetMetaData;

public class DefaultResultSetHandlerFactory<T> implements ResultSetHandlerFactory<T> {
    private final Quirks quirks;
    private final ObjectBuildableFactoryDelegate<T> objectBuilderDelegate;

    public DefaultResultSetHandlerFactory(ObjectBuildableFactoryDelegate<T> objectBuilderDelegate, Quirks quirks) {
        this.objectBuilderDelegate = objectBuilderDelegate;
        this.quirks = quirks;
    }


    @SuppressWarnings("unchecked")
    public ResultSetHandler<T> newResultSetHandler(final ResultSetMetaData meta) {
        return resultSet -> {

            final var objectBuilder = objectBuilderDelegate.newObjectBuilder();

            for (int i = 1; i <= meta.getColumnCount(); i++) {
                final var colName = quirks.getColumnName(meta, i);
                try {
                    objectBuilder.withValue(colName, resultSet.getObject(i));
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
