package org.sql2o;

import com.sun.servicetag.Installer;
import org.sql2o.services.Helper;

import javax.management.RuntimeOperationsException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
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

    private String getSetterName(String fieldName){
        return  "set" + fieldName.substring(0,1).toUpperCase() + fieldName.substring(1);
    }

    private String getGetterName(String fieldName){
        return  "get" + fieldName.substring(0,1).toUpperCase() + fieldName.substring(1);
    }

    private void setField(Object obj, String fieldName, Object value) throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        Class objClass = obj.getClass();
        try{
            Field field = objClass.getField(fieldName);
            field.set(obj, value);
        }
        catch(NoSuchFieldException nsfe){
            String methodName = getSetterName(fieldName);
            Method method = objClass.getMethod(methodName, value.getClass());
            method.invoke(obj, value);
        }
    }

    private Object instantiateIfNecessary(Object obj, String fieldName) throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {

        Object instantiation;

        Class objClass = obj.getClass();
        try{
            Field field = objClass.getField(fieldName);
            instantiation = field.get(obj);
            if (instantiation == null){
                instantiation = field.getType().newInstance();
                field.set(obj, instantiation);
            }
        }
        catch(NoSuchFieldException nsfe){
            Method getter = objClass.getMethod(getGetterName(fieldName));
            instantiation = getter.invoke(obj);
            if (instantiation == null){
                Method setter = objClass.getMethod(getSetterName(fieldName), getter.getReturnType());
                instantiation = getter.getReturnType().newInstance();
                setter.invoke(obj, instantiation);
            }
        }

        return instantiation;
    }


    public <T> List<T> fetch(){
        List list = new ArrayList();
        try{
            ResultSet rs = statement.executeQuery();

            ResultSetMetaData meta = rs.getMetaData();

            while(rs.next()){
                Object obj = this.destinationClass.newInstance();
                for(int colIdx = 1; colIdx <= meta.getColumnCount(); colIdx++){
                    String colName = meta.getColumnName(colIdx);
                    //int colType = meta.getColumnType(colIdx);

                    String[] fieldPath = colName.split("\\.");
                    if (fieldPath.length == 0){
                        fieldPath = new String[]{colName};
                    }

                    Object value = rs.getObject(colName);

                    Object pathObject = obj;
                    for (int pathIdx = 0; pathIdx < fieldPath.length; pathIdx++){
                        if (pathIdx == fieldPath.length - 1){
                            setField(pathObject, fieldPath[pathIdx], value);
                            break;
                        }

                        pathObject = instantiateIfNecessary(pathObject, fieldPath[pathIdx]);

                    }


                }

                list.add(obj);
            }

            rs.close();
        }
        catch(Exception ex){
            throw new RuntimeException(ex);
        }
        finally {
            if (statement != null){
                try{
                    statement.getStatement().getConnection().close();
                    statement.close();
                }
                catch (Exception ex){
                    throw new RuntimeException(ex);
                }
            }
        }

        return list;
    }

    public <T> T fetchFirst(){
        List l = this.fetch();
        if (l.size() == 0){
            return null;
        }
        else{
            return (T)l.get(0);
        }
    }
}
