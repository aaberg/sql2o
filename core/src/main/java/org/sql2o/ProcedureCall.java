package org.sql2o;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Created by lars on 15.04.14.
 */
public class ProcedureCall extends Query {

    private final CallableStatement statement;

    public ProcedureCall(Connection connection, String queryText, String name) {
        super(connection, queryText, name, false);

        try {
            statement = connection.getJdbcConnection().prepareCall(getParsedQuery());
        } catch (SQLException e) {
            throw new Sql2oException("Error creating new instance of CallableStatement", e);
        }

    }

    @Override
    protected PreparedStatement getStatement() {
        return statement;
    }

    public CallableStatement getCallableStatement() {
        return statement;
    }


    @Override
    public <V> Query addParameter(OutParameter<V> parameter) {
        try {
            this.getQuirks().registerOutParameter(this, parameter);
        } catch (SQLException e) {
            throw new Sql2oException("Error registering output parameter", e);
        }
        return this;
    }




}
