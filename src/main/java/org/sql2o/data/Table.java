package org.sql2o.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents an offline result set with columns and rows and data.
 */
public class Table {
    
    private String name;
    
    private List<Row> rows;
    
    private List<Column> columns;
    
    private Map<String, Integer> columnNameToIdxMap;
    
    private boolean caseSensitive;

    Table(boolean isCaseSensitive) {
        this.rows = new ArrayList<Row>();
        this.columns = new ArrayList<Column>();
        this.columnNameToIdxMap = new HashMap<String, Integer>();
        this.caseSensitive = isCaseSensitive;
    }

    public String getName() {
        return name;
    }

    void setName(String name) {
        this.name = name;
    }

    public List<Row> rows() {
        return rows;
    }

    public List<Column> columns() {
        return columns;
    }

    Map<String, Integer> getColumnNameToIdxMap() {
        return columnNameToIdxMap;
    }

    public boolean isCaseSensitive() {
        return caseSensitive;
    }
}
