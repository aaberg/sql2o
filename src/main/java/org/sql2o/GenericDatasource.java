package org.sql2o;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.*;
import java.sql.Connection;

/**
 * Created with IntelliJ IDEA.
 * User: lars
 * Date: 10/5/12
 * Time: 12:13 AM
 * To change this template use File | Settings | File Templates.
 */
public class GenericDatasource implements DataSource {

    private final String url;
    private final String user;
    private final String password;

    private final Logger logger;

    public GenericDatasource(String url, String user, String password) {

        if (!url.startsWith("jdbc")){
            url = "jdbc:" + url;
        }

        this.url = url;
        this.user = user;
        this.password = password;

        this.logger = LoggerFactory.getLogger(GenericDatasource.class);
    }

    public String getUrl() {
        return url;
    }

    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(this.getUrl(), this.getUser(), this.getPassword());
    }

    public Connection getConnection(String username, String password) throws SQLException {
        return DriverManager.getConnection(this.getUrl(), username, password);
    }

    public PrintWriter getLogWriter() throws SQLException {
        return null;
    }

    public void setLogWriter(PrintWriter printWriter) throws SQLException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setLoginTimeout(int i) throws SQLException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public int getLoginTimeout() throws SQLException {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public <T> T unwrap(Class<T> tClass) throws SQLException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean isWrapperFor(Class<?> aClass) throws SQLException {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
