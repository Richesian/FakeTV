package faketv;

import faketv.db.DBConnection;

public class Episode {
	public long start_time;
	private String filename;
	public String server_side_filename;
	public double duration_seconds;
	public boolean is_interlaced;
	public boolean is_television;
	public int id = -1;
	
	public long getEndTime() {
		long show_end_milliseconds = (long)(start_time+(duration_seconds*1000));
		return show_end_milliseconds;
	}
	
	public int getShowID() {
		try {
			DBConnection db = new DBConnection("faketv");
			String folder_name = this.getInternalFolder();
			int show_id = -1;
			db.executeQuery("select id from shows where folder = ?",folder_name);
			while(db.rs.next()) {
				show_id = db.rs.getInt("id");
			}
			if (show_id==-1) {
				show_id = db.executeUpdate("insert into shows (folder) values(?)",folder_name);
			}
			return show_id;
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	
	public String getProgressTimeString() {
		long elapsed_milliseconds = System.currentTimeMillis()-start_time;
		return String.valueOf((elapsed_milliseconds/1000));
	}
	
	
	public void setFilename(String s) {
		this.filename = s;
	}
	
	public String getFilename() {
		String f = filename;
		f = f.replace("\\mnt\\storage","\\\\klaus\\storage");
		return f;
	}
	
	public String getInternalFolder() {
		String fn = filename;
		fn = fn.replace("\\","/");
		String foldername = fn.substring(0,fn.lastIndexOf("/"));
		return foldername;
	}
	
	
	public String getFolder() {
		String fn = getFilename();
		fn = fn.replace("\\","/");
		String foldername = fn.substring(0,fn.lastIndexOf("/"));
		return foldername;
	}
	
	public int getStartSeconds() {
		long elapsed_milliseconds = System.currentTimeMillis()-start_time;
		return (int)(elapsed_milliseconds/1000);
	}
	
	public void toggleInterlaced() {
		DBConnection db = new DBConnection("faketv");
		if (is_interlaced) {
			db.executeUpdate("update faketv set is_interlaced = 0 where id = ?",id);
		} else {
			db.executeUpdate("update faketv set is_interlaced = 1 where id = ?",id);
		}
		is_interlaced = !is_interlaced;
	}
	
	
	public boolean isExpired() {
		long now_time = System.currentTimeMillis();
		long show_end_milliseconds = getEndTime();
		if (show_end_milliseconds<now_time) {
			return true;
		}
		return false;
	}
	
}
