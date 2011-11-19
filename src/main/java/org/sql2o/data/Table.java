package org.sql2o.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: lars
 * Date: 11/19/11
 * Time: 9:04 PM
 * To change this template use File | Settings | File Templates.
 */
public class Table {
    
    private String name;
    
    private List<Row> rows;
    
    private List<Column> columns;
    
    private Map<String, Integer> columnNameToIdxMap;

    Table() {
        this.rows = new ArrayList<Row>();
        this.columns = new ArrayList<Column>();
        this.columnNameToIdxMap = new HashMap<String, Integer>();
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
}
