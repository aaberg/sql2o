package org.sql2o;

import org.sql2o.tools.NamedParameterStatement;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.*;
import java.sql.Date;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: lars
 * Date: 5/18/11
 * Time: 8:27 PM
 * To change this template use File | Settings | File Templates.
 */
public class Query {

    public Query(Sql2o sql2O, String queryText) {
        this.sql2O = sql2O;

        try{
            statement = new NamedParameterStatement(sql2O.getConnection(), queryText);
        }
        catch(Exception ex){
            throw new RuntimeException(ex);
        }

        this.columnMappings = sql2O.getDefaultColumnMappings() == null ? new HashMap<String, String>() : sql2O.getDefaultColumnMappings();
        this.caseSensitive = sql2O.isDefaultCaseSensitive();
    }

    private Sql2o sql2O;

    private Map<String, String> columnMappings;

    private NamedParameterStatement statement;

    private boolean caseSensitive;

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

    public Query addParameter(String name, Date value){
        try{
            statement.setDate(name, value);
        }
        catch (Exception ex){
            throw new RuntimeException(ex);
        }

        return this;
    }

    public Query addParameter(String name, java.util.Date value){
        Date sqlDate = new Date(value.getTime());
        return addParameter(name, sqlDate);
    }

    public Query addParameter(String name, Time value){
        try {
            statement.setTime(name,value);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    public boolean isCaseSensitive() {
        return caseSensitive;
    }

    public Query setCaseSensitive(boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
        return this;
    }

    private String getSetterName(String fieldName){
        return  "set" + fieldName.substring(0,1).toUpperCase() + fieldName.substring(1);
    }

    private String getGetterName(String fieldName){
        return  "get" + fieldName.substring(0,1).toUpperCase() + fieldName.substring(1);
    }

    private void prepareColumnMappings(Class objClass){
        if (!this.isCaseSensitive()){

            for (Field f : objClass.getFields()){
                this.columnMappings.put(f.getName().toLowerCase(), f.getName());
            }
        }
    }

    private void setField(Object obj, String fieldName, Object value) throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        Class objClass = obj.getClass();

        if (!this.isCaseSensitive()){
            fieldName = fieldName.toLowerCase();
        }

        fieldName = columnMappings.containsKey(fieldName) ? columnMappings.get(fieldName) : fieldName;
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

    public <T> List<T> executeAndFetch(Class returnType){
        List list = new ArrayList();
        try{
            prepareColumnMappings(returnType);
            java.util.Date st = new java.util.Date();
            ResultSet rs = statement.executeQuery();
            System.out.println(String.format("execute query time: %s", new java.util.Date().getTime() - st.getTime()));

            ResultSetMetaData meta = rs.getMetaData();

            while(rs.next()){

                Object obj = returnType.newInstance();
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
            try{
                if (this.sql2O.getConnection().getAutoCommit() && statement != null){
                    sql2O.getConnection().close();
                    statement.close();
                }
            }
            catch (Exception ex){
                throw new RuntimeException(ex);
            }
        }

        return list;
    }

    public <T> T executeAndFetchFirst(Class returnType){
        List l = this.executeAndFetch(returnType);
        if (l.size() == 0){
            return null;
        }
        else{
            return (T)l.get(0);
        }
    }

    public Sql2o executeUpdate(){
        int result;
        try{
            result = statement.executeUpdate();
            if (this.sql2O.getConnection().getAutoCommit()){
                this.sql2O.getConnection().close();
                statement.close();
            }
        }
        catch(Exception ex){
            this.sql2O.rollback();
            throw new RuntimeException(ex);
        }

        return this.sql2O;
    }

    public Object executeScalar(){
        try {
            ResultSet rs = this.statement.executeQuery();
            if (rs.next()){
                return rs.getObject(1);
            }
            else{
                return null;
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }

    /************** batch stuff *******************/

    public Query addToBatch(){
        try {
            statement.addBatch();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return this;
    }

    public Sql2o executeBatch(){
        try {
            statement.executeBatch();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return this.sql2O;
    }

    /*********** column mapping ****************/

    public Map<String, String> getColumnMappings() {
        return columnMappings;
    }

    public Query addColumnMapping(String columnName, String fieldName){
        this.columnMappings.put(columnName, fieldName);

        return this;
    }

}
