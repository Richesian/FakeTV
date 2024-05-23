package faketv.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Enumeration;
import java.util.Vector;

import faketv.util.Logger;



public class DBConnectionPool {

	private int checkedOut=0;
	private Vector<ConnWrapper> freeConnections;
	private int maxConn;
	private String name;
	private String password;
	private String URL;
	private String user;
	private String test_sql;
	private long last_cleanup_time = System.currentTimeMillis();
	private long timeout;

	public synchronized void freeConnection(Connection con) {
		freeConnections.addElement(new ConnWrapper(con));
		checkedOut--;
		notifyAll();
	}
	
	
	public void removeTimedOutConnections() {
		if (timeout==0){return;}
		for (int i=0;i<freeConnections.size();i++) {
			ConnWrapper cw = (ConnWrapper) freeConnections.elementAt(i);
			if ((System.currentTimeMillis()-cw.timeout)>=timeout) {
				freeConnections.removeElementAt(i);
				i--;
			}
		}
	}
	

	public synchronized Connection getConnection() {
		ConnWrapper cw = null;
		Connection con = null;
		
		removeTimedOutConnections();

		if (freeConnections.size() > 0) {
			int last_element = freeConnections.size() - 1;
			cw = (ConnWrapper) freeConnections.elementAt(last_element);
			con = cw.con;
			freeConnections.removeElementAt(last_element);
			try {
				if (con.isClosed()) {
					Logger.log("connection_log",
							"Removed closed connection from " + name);
					con = getConnection();
				} else

				if (isConnectionStale(con)) {
					try {
						con.close();
					} catch (Exception exception) {
					}
					//log("Disconnected/Stale connection removed from pool " + name);
					con = getConnection();
				}
			} catch (Exception e) {
				Logger.log("connection_log",
						"Removed bad connection from " + name);
				con = getConnection();
			}
		} else if (maxConn == 0 || checkedOut < maxConn) {
			con = newConnection();
		}

		if (con != null)
			checkedOut++;
		return con;
	}
	
	public Connection newConnection() {
		Connection con = null;
		try	{
			con = (Connection) DriverManager.getConnection(URL, user, password);
			Logger.log("connection_log", "Created new connection in pool: "+name);
		}
		catch(Exception e) {
			Logger.log("connection_log","Error in establishing new connection to URL: "+e.toString());
		}
		return con;
	}

	public synchronized void release() {
		for (Enumeration<?> allConnections = freeConnections.elements(); allConnections
				.hasMoreElements();) {
			ConnWrapper cw = (ConnWrapper) allConnections.nextElement();
			try {
				cw.con.close();
				//log("Closed connection for pool " + name);
			} catch (Exception e) {
				//log(e, "Can't close connection for pool " + name);
			}
		}
		freeConnections.removeAllElements();
	}



	public boolean isConnectionStale(Connection con) {
		if (test_sql == null || test_sql.equals(""))
			return false;
		Statement stmt = null;
		try {
			stmt = con.createStatement();
			stmt.executeQuery(test_sql);
		} catch (Exception e) {
			return true;
		} finally {
			try{stmt.close();}catch(Exception estmt){}
		}
		return false;
	}

	public DBConnectionPool(String name, String URL, String user,
			String password, int maxConn, String test_sql,
			String timeout_seconds) {
		freeConnections = new Vector<ConnWrapper>();
		this.name = name;
		this.URL = URL;
		this.user = user;
		this.password = password;
		this.maxConn = maxConn;
		this.test_sql = test_sql;
		this.timeout = 0;
		try {
			timeout = Integer.valueOf(timeout_seconds).intValue()*1000;
		}catch(Exception e){Logger.log("connection_log", "Error setting timeout: "+e+" ("+timeout_seconds+")");}
	}
}
