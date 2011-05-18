package org.sql2o;

import org.sql2o.services.Helper;

import javax.management.RuntimeOperationsException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Created by IntelliJ IDEA.
 * User: lars
 * Date: 5/18/11
 * Time: 8:27 PM
 * To change this template use File | Settings | File Templates.
 */
public class Query {

    public Query(Sql2o sql2O, Class destinationClass, String queryText) {
        this.sql2O = sql2O;
        this.destinationClass = destinationClass;
        this.queryText = queryText;

        Connection con = Helper.createConnection(this.sql2O);
        try{
            statement = new NamedParameterStatement(con, queryText);
        }
        catch(Exception ex){
            throw new RuntimeException(ex);
        }
    }

    private Sql2o sql2O;
    private String queryText;
    private Class destinationClass;

    private NamedParameterStatement statement;

    public Query addParameter(String name, Object value){
        try{
            statement.setObject(name, value);
        }
        catch(Exception ex){
            throw new RuntimeException(ex);
        }
        return this;
    }

    public Query addParameter(String name, int value){
        try{
            statement.setInt(name, value);
        }
        catch(Exception ex){
            throw new RuntimeException(ex);
        }
        return this;
    }

    public Query addParameter(String name, long value){
        try{
            statement.setLong(name, value);
        }
        catch(Exception ex){
            throw new RuntimeException(ex);
        }
        return this;
    }

    public Query addParameter(String name, String value){
        try{
            statement.setString(name, value);
        }
        catch(Exception ex){
            throw new RuntimeException(ex);
        }
        return this;
    }

    public Query addParameter(String name, Timestamp value){
        try{
            statement.setTimestamp(name,value);
        }
        catch(Exception ex){
            throw new RuntimeException(ex);
        }
        return this;
    }


    public List fetch(){
        List list = new ArrayList();
        try{
            ResultSet rs = statement.executeQuery();

            ResultSetMetaData meta = rs.getMetaData();

            while(rs.next()){
                Object obj = this.destinationClass.newInstance();
                for(int colIdx = 1; colIdx <= meta.getColumnCount(); colIdx++){
                    String colName = meta.getColumnName(colIdx);
                    int colType = meta.getColumnType(colIdx);

                    Object value = rs.getObject(colName);

                    try{
                        Field field = destinationClass.getField(colName);
                        field.set(obj, value);
                    }
                    catch(NoSuchFieldException nsfe){
                        String methodName = "set" + colName.substring(0,1).toUpperCase() + colName.substring(1);
                        Method method = destinationClass.getMethod(methodName, value.getClass());
                        method.invoke(obj, value);
                    }


                }

                list.add(obj);
            }

        }
        catch(Exception ex){
            throw new RuntimeException(ex);
        }
        finally {
            if (statement != null){
                try{
                    statement.close();
                }
                catch (Exception ex){
                    throw new RuntimeException(ex);
                }
            }
        }

        return list;
    }
}
