package faketv;

import faketv.db.DBConnection;

public class Episode {
	public long start_time;
	private String filename;
	public String server_side_filename;
	public double duration_seconds;
	public boolean is_interlaced;
	public boolean is_television;
	public boolean use_vlc = false;
	public int audio_track = 0;
	
	public int id = -1;
	
	public long getEndTime() {
		long show_end_milliseconds = (long)(start_time+(duration_seconds*1000));
		return show_end_milliseconds;
	}
	
	
	
	
	
	
	
	public void loadDetails() {
		try {
			DBConnection db = new DBConnection("faketv");
			db.executeQuery("select id, filename, duration_seconds, is_television, is_interlaced, use_vlc, audio_track from videos where id = ?",id);
			while(db.rs.next()) {
				
				filename = db.rs.getString("filename");
				setFilename(filename);
				start_time = System.currentTimeMillis();
				duration_seconds = db.rs.getDouble("duration_seconds");
				is_interlaced = (db.rs.getInt("is_interlaced") > 0);
				is_television = (db.rs.getInt("is_television") > 0);
				audio_track = db.rs.getInt("audio_track");
				
				use_vlc = (db.rs.getInt("use_vlc") > 0);
			}
		} catch(Exception e) {
			
			throw new RuntimeException(e);
		}
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
			
			
			System.out.println("Show ID for "+folder_name+" assignment is "+show_id);
			
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
		s = s.replace("/","\\");
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
	
	
	public void toggleVLC() {
		DBConnection db = new DBConnection("faketv");
		
		
		if (use_vlc) {
			System.out.println("Setting VLC to off");
			db.executeUpdate("update videos set use_vlc = 0 where id = ?",id);
		} else {
			System.out.println("Setting VLC to on");

			db.executeUpdate("update videos set use_vlc = 1 where id = ?",id);
		}
		use_vlc = !use_vlc;
	}
	
	public void toggleInterlaced() {
		DBConnection db = new DBConnection("faketv");
		if (is_interlaced) {
			db.executeUpdate("update videos set is_interlaced = 0 where id = ?",id);
		} else {
			db.executeUpdate("update videos set is_interlaced = 1 where id = ?",id);
		}
		is_interlaced = !is_interlaced;
	}
	
	public void incrementAudioTrack() {
		DBConnection db = new DBConnection("faketv");
		audio_track++;
		if (audio_track>=4) {
			audio_track =0;
		}
		db.executeUpdate("update videos set audio_track = ? where id = ?",audio_track, id);
		
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
