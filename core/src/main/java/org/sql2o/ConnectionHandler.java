package org.sql2o;

/**
 * User: npratt
 * Date: 9/26/14
 * Time: 11:52
 */
public interface ConnectionHandler
{
	public java.sql.Connection getJdbcConnection();

	public Connection handleCommit( Connection connection, boolean closeConnection );
	public Connection handleRollback( Connection connection, boolean closeConnection );
	public void handleClose( Connection connection );
}
