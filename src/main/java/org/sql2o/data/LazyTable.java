package org.sql2o.data;

import org.sql2o.ResultSetIterable;

import java.util.List;

/**
 * @author aldenquimby@gmail.com
 */
public class LazyTable {
    private String name;
    private ResultSetIterable<Row> rows;
    private List<Column> columns;

    public String getName()
    {
        return name;
    }

    void setName(String name)
    {
        this.name = name;
    }

    public ResultSetIterable<Row> getRows()
    {
        return rows;
    }

    public void setRows(ResultSetIterable<Row> rows)
    {
        this.rows = rows;
    }

    public List<Column> getColumns()
    {
        return columns;
    }

    void setColumns(List<Column> columns)
    {
        this.columns = columns;
    }
}
