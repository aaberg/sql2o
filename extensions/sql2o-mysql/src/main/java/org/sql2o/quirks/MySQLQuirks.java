package org.sql2o.quirks;

import java.sql.ResultSet;
import java.sql.SQLException;

public class MySQLQuirks extends NoQuirks {
    public MySQLQuirks() {
        super();
    }

    @Override
    public Object getRSVal(ResultSet rs, int idx) throws SQLException {
        Object o = rs.getObject(idx);
        if (o != null && o.getClass().getCanonicalName().startsWith("java.time.LocalDateTime")){
            o = rs.getObject(idx, java.sql.Timestamp.class);
        }
        return o;
    }
}
