package org.sql2o;

/**
 * User: npratt
 * Date: 9/26/14
 * Time: 11:55
 */
public class Sql2oConnectionHandlerFactory implements ConnectionHandlerFactory
{
	private final Sql2o sql2o;

	public Sql2oConnectionHandlerFactory( Sql2o sql2o ) {
		this.sql2o = sql2o;
	}

	@Override
	public ConnectionHandler getConnectionHandler() {
		return new ConnectionHandler()
		{
			@Override
			public java.sql.Connection getJdbcConnection() {
				try{
					return sql2o.getDataSource().getConnection();
				}
				catch(Exception ex){
					throw new Sql2oException("Could not acquire a connection from DataSource - " + ex.getMessage(), ex);
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
		};
	}
}
