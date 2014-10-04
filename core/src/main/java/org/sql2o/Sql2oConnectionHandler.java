package org.sql2o;

public class Sql2oConnectionHandler implements ConnectionHandler
{
	private final Sql2o sql2o;
	private boolean rollbackOnException = true;

	public Sql2oConnectionHandler( Sql2o sql2o ) {
		this.sql2o = sql2o;
	}

	@Override
	public java.sql.Connection getJdbcConnection() {
		try {
			return sql2o.getDataSource().getConnection();
		}
		catch( Exception ex ) {
			throw new Sql2oException( "Could not acquire a connection from DataSource - " + ex.getMessage(), ex );
		}
	}

	@Override
	public Connection handleCommit( Connection connection, boolean closeConnection ) {
		return connection.internalCommit( closeConnection );
	}

	@Override
	public Connection handleRollback( Connection connection, boolean closeConnection ) {
		return connection.internalRollback( closeConnection );
	}

	@Override
	public void handleClose( Connection connection ) {
		connection.internalClose();
	}

	@Override
	public boolean isRollbackOnException() {
		return rollbackOnException;
	}

	@Override
	public void setRollbackOnException( boolean rollbackOnException ) {
		this.rollbackOnException = rollbackOnException;
	}
}
