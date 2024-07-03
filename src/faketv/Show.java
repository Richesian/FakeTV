package faketv;

import java.util.ArrayList;
import java.util.Random;

import faketv.db.DBConnection;

public class Show {
	public int show_id;
	public Random random;
	public String folder;
	public String last_played_episode; 
	

	public static Show[] getShows(int channel_id) {
		return getShows(""+channel_id);
	}
	

	public static Show[] getShows(String show_list) {
		ArrayList<Show> shows = new ArrayList<Show>();
			
		//Load all shows and times
		DBConnection db = new DBConnection("faketv");
		try {
			db.executeQuery("select cs.show_id, s.folder,s.last_played_episode from channel_shows cs, shows s where cs.show_id = s.id and cs.channel_id in ("+show_list+") order by rand()");
			while(db.rs.next()) {
				Show s = new Show();
				s.show_id = db.rs.getInt("show_id");
				s.folder = db.rs.getString("folder");
				s.random = new Random();
				s.last_played_episode = db.rs.getString("last_played_episode");
				shows.add(s);
			}
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
		return shows.toArray(new Show[shows.size()]);		
	}
	
	public void setLastPlayedEpisode(String filename) {
		this.last_played_episode = filename;
		DBConnection db = new DBConnection("faketv");
		db.executeUpdate("update shows set last_played_episode = ? where id = ?",filename,show_id);
	}
	
	
	
	public double getRandom() {
		return random.nextDouble();
	}
	
	
}
