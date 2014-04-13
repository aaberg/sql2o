package org.sql2o.tools;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Utilities for {@link java.sql.ResultSet}.
 */
public class ResultSetUtils {
    // TODO. Remove class for version 2.0
    @Deprecated
    public static Object getRSVal(ResultSet rs, int idx) throws SQLException {
        Object o = rs.getObject(idx);

        // oracle timestamps are not always convertible to a java Date. If ResultSet.getTimestamp is used instead of
        // ResultSet.getObject, a normal java.sql.Timestamp instance is returnd.
        if (o != null && FeatureDetector.isOracleAvailable() && o.getClass().getCanonicalName().startsWith("oracle.sql.TIMESTAMP")){
            o = rs.getTimestamp(idx);
        }

        return o;
    }
}
