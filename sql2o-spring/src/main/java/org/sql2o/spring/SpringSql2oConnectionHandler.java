package org.sql2o.spring;

import org.springframework.jdbc.datasource.DataSourceUtils;
import org.sql2o.Connection;
import org.sql2o.ConnectionHandler;
import org.sql2o.Sql2o;
import org.sql2o.Sql2oException;

import javax.sql.DataSource;
import java.sql.SQLException;

public class SpringSql2oConnectionHandler implements ConnectionHandler
{
	private final Sql2o sql2o;

	public SpringSql2oConnectionHandler( Sql2o sql2o ) {
		this.sql2o = sql2o;
	}

	@Override
	public java.sql.Connection getJdbcConnection() {
		DataSource dataSource = sql2o.getDataSource();
		try {
			return DataSourceUtils.doGetConnection( dataSource );
		}
		catch( SQLException e ) {
			throw new Sql2oException( e );
		}
	}

	@Override
	public Connection handleCommit( Connection connection, boolean closeConnection ) {
		throw new Sql2oException( "commit() should not be invoked explicitly - the Spring Transaction Manager is managing this" );
	}

	@Override
	public Connection handleRollback( Connection connection, boolean closeConnection ) {
		throw new Sql2oException( "rollback() should not be invoked explicitly - the Spring Transaction Manager is managing this" );
	}

	@Override
	public void handleClose( Connection connection ) {
		throw new Sql2oException( "close() should not be invoked explicitly - the Spring Transaction Manager is managing this" );
	}

	@Override
	public boolean isRollbackOnException() {
		return false;
	}

	@Override
	public void setRollbackOnException( boolean rollbackOnException ) {
		throw new Sql2oException( "Cannot set this - rollback is managed by the Spring Transaction Manager" );
	}
}
