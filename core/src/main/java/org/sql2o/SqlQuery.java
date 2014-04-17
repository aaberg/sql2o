package org.sql2o;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Created by lars on 15.04.14.
 */
public class SqlQuery extends Query {

    private final PreparedStatement statement;

    public SqlQuery(Connection connection, String queryText, String name, boolean returnGeneratedKeys) {
        super(connection, queryText, name, returnGeneratedKeys);

        try {
            if (returnGeneratedKeys) {
                statement = getConnection().getJdbcConnection().prepareStatement(getParsedQuery(), Statement.RETURN_GENERATED_KEYS);
            } else {
                statement = getConnection().getJdbcConnection().prepareStatement(getParsedQuery());
            }
        } catch(SQLException ex) {
            throw new RuntimeException(String.format("Error preparing statement - %s", ex.getMessage()), ex);
        }
    }

    @Override
    protected PreparedStatement getStatement() {
        return statement;
    }

    @Override
    public <V> Query addParameter(OutParameter<V> parameter) {
        throw new Sql2oException("Output parameters are not supported when using createQuery method. Use createProcedureCall method instead");
    }
}
