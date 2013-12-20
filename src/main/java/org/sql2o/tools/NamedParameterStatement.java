package org.sql2o.tools;

import java.io.InputStream;
import java.sql.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


/**
* This class wraps around a {@link PreparedStatement} and allows the 
programmer to set parameters by name instead
* of by index.  This eliminates any confusion as to which parameter index 
represents what.  This also means that
* rearranging the SQL statement or adding a parameter doesn't involve 
renumbering your indices.
* Code such as this:
*
* Connection con=getConnection();
* String query="select * from my_table where name=? or address=?";
* PreparedStatement p=con.prepareStatement(query);
* p.setString(1, "bob");
* p.setString(2, "123 terrace ct");
* ResultSet rs=p.executeQuery();
*
* can be replaced with:
*
* Connection con=getConnection();
* String query="select * from my_table where name=:name or 
address=:address";
* NamedParameterStatement p=new NamedParameterStatement(con, query);
* p.setString("name", "bob");
* p.setString("address", "123 terrace ct");
* ResultSet rs=p.executeQuery();
*
* @author adam_crume
*/
public class NamedParameterStatement {
    /** The statement this object is wrapping. */
    private final PreparedStatement statement;

    /** Maps parameter names to arrays of ints which are the parameter indices. */
    private final Map indexMap;


    /**
     * Creates a NamedParameterStatement.  Wraps a call to
     * c.{@link Connection#prepareStatement(java.lang.String) prepareStatement}.
     * @param connection the database connection
     * @param query      the parameterized query
     * @throws SQLException if the statement could not be created
     */
    public NamedParameterStatement(Connection connection, String query, boolean returnGeneratedKeys) throws SQLException {
        indexMap=new HashMap();
        String parsedQuery=parse(query, indexMap);
        if (returnGeneratedKeys) {
            statement = connection.prepareStatement(parsedQuery, Statement.RETURN_GENERATED_KEYS);
        } else {
            statement = connection.prepareStatement(parsedQuery);
        }
    }


    /**
     * Parses a query with named parameters.  The parameter-index mappings are put into the map, and the
     * parsed query is returned.  DO NOT CALL FROM CLIENT CODE.  This method is non-private so JUnit code can
     * testSelect it.
     * @param query    query to parse
     * @param paramMap map to hold parameter-index mappings
     * @return the parsed query
     */
    private static final String parse(String query, Map paramMap) {
        // I was originally using regular expressions, but they didn't work well for ignoring
        // parameter-like strings inside quotes.
        int length=query.length();
        StringBuffer parsedQuery=new StringBuffer(length);
        boolean inSingleQuote=false;
        boolean inDoubleQuote=false;
        int index=1;

        for(int i=0;i<length;i++) {
            char c=query.charAt(i);
            if(inSingleQuote) {
                if(c=='\'') {
                    inSingleQuote=false;
                }
            } else if(inDoubleQuote) {
                if(c=='"') {
                    inDoubleQuote=false;
                }
            } else {
                if(c=='\'') {
                    inSingleQuote=true;
                } else if(c=='"') {
                    inDoubleQuote=true;
                } else if(c==':' && i+1<length &&
                        Character.isJavaIdentifierStart(query.charAt(i+1))) {
                    int j=i+2;
                    while(j<length && Character.isJavaIdentifierPart(query.charAt(j))) {
                        j++;
                    }
                    String name=query.substring(i+1,j);
                    c='?'; // replace the parameter with a question mark
                    i+=name.length(); // skip past the end if the parameter

                    List indexList=(List)paramMap.get(name);
                    if(indexList==null) {
                        indexList=new LinkedList();
                        paramMap.put(name, indexList);
                    }
                    indexList.add(new Integer(index));

                    index++;
                }
            }
            parsedQuery.append(c);
        }

        // replace the lists of Integer objects with arrays of ints
        for(Iterator itr=paramMap.entrySet().iterator(); itr.hasNext();) {
            Map.Entry entry=(Map.Entry)itr.next();
            List list=(List)entry.getValue();
            int[] indexes=new int[list.size()];
            int i=0;
            for(Iterator itr2=list.iterator(); itr2.hasNext();) {
                Integer x=(Integer)itr2.next();
                indexes[i++]=x.intValue();
            }
            entry.setValue(indexes);
        }

        return parsedQuery.toString();
    }


    /**
     * Returns the indexes for a parameter.
     * @param name parameter name
     * @return parameter indexes
     * @throws IllegalArgumentException if the parameter does not exist
     */
    private int[] getIndexes(String name) {
        int[] indexes=(int[])indexMap.get(name);
        if(indexes==null) {
            throw new IllegalArgumentException("Parameter not found: "+name);
        }
        return indexes;
    }


    /**
     * Sets a parameter.
     * @param name  parameter name
     * @param value parameter value
     * @throws SQLException if an error occurred
     * @throws IllegalArgumentException if the parameter does not exist
     * @see PreparedStatement#setObject(int, java.lang.Object)
     */
    public void setObject(String name, Object value) throws SQLException {
        int[] indexes=getIndexes(name);
        for(int i=0; i < indexes.length; i++) {
        	statement.setObject(indexes[i], value, Types.OTHER);
        }
    }
    
    public void setNull(String name, int sqlType) throws SQLException {
        int[] indexes=getIndexes(name);
        for (int i = 0; i < indexes.length; i++){
            statement.setNull(indexes[i], sqlType);
        }
    }

    /**
     * Sets a parameter.
     * @param name  parameter name
     * @param value parameter value
     * @throws SQLException if an error occurred
     * @throws IllegalArgumentException if the parameter does not exist
     * @see PreparedStatement#setBinaryStream(int, java.io.InputStream inputStream)
     */
    public void setInputStream( String name, InputStream value ) throws SQLException {
        int[] indexes=getIndexes(name);
        for (int i = 0; i < indexes.length; i++){
            statement.setBinaryStream( indexes[i], value );
        }
    }


    /**
     * Sets a parameter.
     * @param name  parameter name
     * @param value parameter value
     * @throws SQLException if an error occurred
     * @throws IllegalArgumentException if the parameter does not exist
     * @see PreparedStatement#setString(int, java.lang.String)
     */
    public void setString(String name, String value) throws SQLException {
        int[] indexes=getIndexes(name);
        for(int i=0; i < indexes.length; i++) {
            statement.setString(indexes[i], value);
        }
    }


    /**
     * Sets a parameter.
     * @param name  parameter name
     * @param value parameter value
     * @throws SQLException if an error occurred
     * @throws IllegalArgumentException if the parameter does not exist
     * @see PreparedStatement#setInt(int, int)
     */
    public void setInt(String name, int value) throws SQLException {
        int[] indexes=getIndexes(name);
        for(int i=0; i < indexes.length; i++) {
            statement.setInt(indexes[i], value);
        }
    }


    /**
     * Sets a parameter.
     * @param name  parameter name
     * @param value parameter value
     * @throws SQLException if an error occurred
     * @throws IllegalArgumentException if the parameter does not exist
     * @see PreparedStatement#setInt(int, int)
     */
    public void setLong(String name, long value) throws SQLException {
        int[] indexes=getIndexes(name);
        for(int i=0; i < indexes.length; i++) {
            statement.setLong(indexes[i], value);
        }
    }


    /**
     * Sets a parameter.
     * @param name  parameter name
     * @param value parameter value
     * @throws SQLException if an error occurred
     * @throws IllegalArgumentException if the parameter does not exist
     * @see PreparedStatement#setTimestamp(int, java.sql.Timestamp)
     */
    public void setTimestamp(String name, Timestamp value) throws SQLException {
        int[] indexes=getIndexes(name);
        for(int i=0; i < indexes.length; i++) {
            statement.setTimestamp(indexes[i], value);
        }
    }

    public void setDate(String name, Date value) throws SQLException{
        int[] indexes=getIndexes(name);
        for (int index : indexes){
            statement.setDate(index, value);
        }
    }

    public void setTime(String name, Time value) throws SQLException {
        int[] indexes = getIndexes(name);
        for (int index: indexes){
            statement.setTime(index, value);
        }
    }


    /**
     * Returns the underlying statement.
     * @return the statement
     */
    public PreparedStatement getStatement() {
        return statement;
    }


    /**
     * Executes the statement.
     * @return true if the first result is a {@link ResultSet}
     * @throws SQLException if an error occurred
     * @see PreparedStatement#execute()
     */
    public boolean execute() throws SQLException {
        return statement.execute();
    }


    /**
     * Executes the statement, which must be a query.
     * @return the query results
     * @throws SQLException if an error occurred
     * @see PreparedStatement#executeQuery()
     */
    public ResultSet executeQuery() throws SQLException {
        return statement.executeQuery();
    }


    /**
     * Executes the statement, which must be an SQL INSERT, UPDATE or DELETE 
statement;
     * or an SQL statement that returns nothing, such as a DDL statement.
     * @return number of rows affected
     * @throws SQLException if an error occurred
     * @see PreparedStatement#executeUpdate()
     */
    public int executeUpdate() throws SQLException {
        return statement.executeUpdate();
    }


    /**
     * Closes the statement.
     * @throws SQLException if an error occurred
     * @see Statement#close()
     */
    public void close() throws SQLException {
        statement.close();
    }


    /**
     * Adds the current set of parameters as a batch entry.
     * @throws SQLException if something went wrong
     */
    public void addBatch() throws SQLException {
        statement.addBatch();
    }


    /**
     * Executes all of the batched statements.
     * 
     * See {@link Statement#executeBatch()} for details.
     * @return update counts for each statement
     * @throws SQLException if something went wrong
     */
    public int[] executeBatch() throws SQLException {
        return statement.executeBatch();
    }
}