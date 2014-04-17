package org.sql2o.tools;

import java.sql.CallableStatement;
import java.sql.SQLException;

/**
 * Created by lars on 15.04.14.
 */
public interface OutParameterGetter<V> {

    V handle(CallableStatement statement, int paramIdx) throws SQLException;
}
