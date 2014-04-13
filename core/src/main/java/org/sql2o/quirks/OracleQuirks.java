package org.sql2o.quirks;


import java.sql.ResultSet;
import java.sql.SQLException;

public class OracleQuirks extends NoQuirks {
    @Override
    public Object getRSVal(ResultSet rs, int idx) throws SQLException {
        Object o = super.getRSVal(rs, idx);
        // oracle timestamps are not always convertible to a java Date. If ResultSet.getTimestamp is used instead of
        // ResultSet.getObject, a normal java.sql.Timestamp instance is returnd.
        if (o != null && o.getClass().getCanonicalName().startsWith("oracle.sql.TIMESTAMP")){
            //TODO: move to sql2o-oracle
            //TODO: use TIMESTAMP.dateValue
            o = rs.getTimestamp(idx);
        }
        return o;
    }
}
