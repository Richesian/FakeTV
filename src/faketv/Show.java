package faketv;

import java.util.ArrayList;
import java.util.Random;

import faketv.db.DBConnection;

public class Show {
	public int show_id;
	public Random random;
	public String folder;
	

	public static Show[] getShows(int channel_id) {
		ArrayList<Show> shows = new ArrayList<Show>();
		
		//Load all shows and times
		DBConnection db = new DBConnection("faketv");
		try {
			db.executeQuery("select cs.show_id, s.folder from channel_shows cs, shows s where cs.show_id = s.id and cs.channel_id = ? order by rand()",channel_id);
			while(db.rs.next()) {
				Show s = new Show();
				s.show_id = db.rs.getInt("show_id");
				s.folder = db.rs.getString("folder");
				s.random = new Random();
				shows.add(s);
			}
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
		if (shows.size()>0) {
			System.out.println("We loaded: "+shows.size()+" for channel "+channel_id);
		}
		
		return shows.toArray(new Show[shows.size()]);
	}
	
	public double getRandom() {
		return random.nextDouble();
	}
	
	
}
