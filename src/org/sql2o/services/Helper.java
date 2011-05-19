package org.sql2o.services;

import org.sql2o.Sql2o;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSetMetaData;
import java.sql.Types;
import java.util.Properties;

/**
 * Created by IntelliJ IDEA.
 * User: lars
 * Date: 5/18/11
 * Time: 9:15 PM
 * To change this template use File | Settings | File Templates.
 */
public class Helper {

    public static Connection createConnection(Sql2o sql2o){


        Properties conProps = new Properties();
        conProps.put("user", sql2o.getUser());
        conProps.put("password", sql2o.getPass());
        Connection con;
        try{
            if (!sql2o.getUrl().startsWith("jdbc")){
                sql2o.setUrl("jdbc:" + sql2o.getUrl());
            }
            con = DriverManager.getConnection(sql2o.getUrl(), conProps);
        }
        catch(Exception ex){
            throw new RuntimeException(ex);
        }

        return con;
    }
}
