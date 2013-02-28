package org.sql2o.data;

import org.sql2o.QuirksMode;
import org.sql2o.Sql2oException;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

/**
 * The table factory is used to create {@link Table} instances.
 */
public class TableFactory {
    
    public static Table createTable(ResultSet rs, boolean isCaseSensitive, QuirksMode quirksMode){
        Table table = new Table(isCaseSensitive);
        try {
            applyMetadata(table, rs.getMetaData(), quirksMode);

        } catch (SQLException e) {
            throw new Sql2oException("Error while reading metadata from database", e);
        }

        try {
            while(rs.next()){
                Row row = new Row(table);
                table.rows().add(row);
                for (Column column : table.columns()){
                    row.addValue(column.getIndex(), rs.getObject(column.getIndex() + 1));
                }
            }
        } catch (SQLException e) {
            throw new Sql2oException("Error while filling Table with data from database", e);
        }
        
        return table;
    }
    
    private static void applyMetadata(Table table, ResultSetMetaData metadata, QuirksMode quirksMode) throws SQLException {
        
        table.setName( metadata.getTableName(1) );

        for (int colIdx = 1; colIdx <= metadata.getColumnCount(); colIdx++){
            String colName;
            if (quirksMode == QuirksMode.DB2){
                colName = metadata.getColumnName(colIdx);
            } else {
                colName = metadata.getColumnLabel(colIdx);
            }
            String colType = metadata.getColumnTypeName(colIdx);
            table.columns().add(new Column(colName, colIdx - 1, colType));

            String colMapName = table.isCaseSensitive() ? colName : colName.toLowerCase();
            table.getColumnNameToIdxMap().put(colMapName, colIdx - 1);
        }
    }
}
