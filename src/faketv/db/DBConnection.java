package faketv.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javax.sql.rowset.CachedRowSet;




import com.sun.rowset.CachedRowSetImpl;

import faketv.util.Logger;

  
/**
 * This is a nice Class that simplifies database connection management. This object is guaranteed to 
 * only hold onto connections as long as necessary and then return them to the connection pool, even
 * if errors occur. This is possible because of javax.sql.rowset.CachedRowSet. By default only 10000
 * rows can be returned, so it is better not to use this routine for big operations where more than
 * 10000 rows will be returned. Of course it can be modified to allow more rows, but it is probably better
 * to bypass this for huge result sets since results are stored in memory. However, if you have the memory,
 * the 10000 row limit can be changed via the setMaxRows(row_count) function.
 * 
 * @author Richard Bird
 *
 */
public class DBConnection {
	
	public CachedRowSet rs = null;
	private String db_name;
	private int max_rows;
	
	public DBConnection(String db_name) {
		this.db_name = db_name;
		max_rows = 10000;
	}

	public void executeQuery(String sql) {
		executeQuery(sql,(Object) null);
	}
	
	public void executeQuery(String sql, Object... parameters) {

		PreparedStatement ps = null;
		ResultSet results = null;
		Connection con = null;
		try {
			con = ConnManager.getInstance().getConnection(db_name);
			ps = con.prepareStatement(sql);
			for (int i=0;i<parameters.length;i++) {
				if (parameters[i]==null) break;
				ps.setObject(i+1, parameters[i]);
			}
			
			results = ps.executeQuery();
			rs = new CachedRowSetImpl();
			rs.setMaxRows(max_rows);
			rs.setPageSize(0);
			rs.populate(results);
			results.close();
			ps.close();
		}
		catch(Exception e) {
			Logger.log("db_wrapper","Error executing query on "+db_name+": "+sql+": "+e);
			throw new RuntimeException(e);
		}
		finally {
			try{results.close();}catch(Exception e){}
			try{ps.close();}catch(Exception e){}
			try{ConnManager.getInstance().freeConnection(db_name, con);}catch(Exception e){}
		}
	}
	
	/**
	 * Run an SQL update, returns auto generated key
	 * @param sql
	 * @return
	 * @throws Exception
	 */
	public int executeUpdate(String sql) {
		return executeUpdate(sql,(Object) null);
	}

	/**
	 * Run an SQL update, returns auto generated key
	 * @param sql
	 * @param parameters
	 * @return
	 * @throws Exception
	 */
	public int executeUpdate(String sql, Object... parameters) {
		ResultSet insert_id_rs = null;
		PreparedStatement ps = null;
		Connection con = null;
		int insert_id = -1;
		try {
			con = ConnManager.getInstance().getConnection(db_name);
			ps = con.prepareStatement(sql,PreparedStatement.RETURN_GENERATED_KEYS);

			for (int i=0;i<parameters.length;i++) {
				//if (parameters[i]==null) break;
				ps.setObject(i+1, parameters[i]);
			}
			ps.executeUpdate();
			insert_id_rs = ps.getGeneratedKeys();
			while(insert_id_rs.next()) {
				insert_id = insert_id_rs.getInt(1); 
				break;
			}
			return insert_id;
		}
		catch(Exception e) {
			Logger.log("db_wrapper","Error executing update on "+db_name+": "+sql+": "+e);
			throw new RuntimeException(e);
		}
		finally {
			try{insert_id_rs.close();}catch(Exception e){}
			try{ps.close();}catch(Exception e){}
			try{ConnManager.getInstance().freeConnection(db_name, con);}catch(Exception e){}
		}
	}
	
	public void setMaxRows(int max_rows) {
		this.max_rows = max_rows;
	}

	public int getMaxRows() {
		return max_rows;
	}

	
}