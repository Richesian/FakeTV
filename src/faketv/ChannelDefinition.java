package faketv;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Hashtable;
import java.util.Random;

import faketv.db.DBConnection;

public class ChannelDefinition {

	public int id;
	public String title;
	public String descr;
	public Show[] shows;
	public int show_index = 0;
	
	public static ChannelDefinition[] getChannelDefinitions() {
		ArrayList<ChannelDefinition> channel_definitions = new ArrayList<ChannelDefinition>();
		try {
			DBConnection db = new DBConnection("faketv");
			
			//First load all of the channels from the channels table
			db.executeQuery("select id,title,descr from channels order by id");
			while(db.rs.next()) {
				ChannelDefinition c = new ChannelDefinition();
				c.id = db.rs.getInt("id");
				c.title = db.rs.getString("title");
				c.descr = db.rs.getString("descr");
				channel_definitions.add(c);
			}
			
			//Now let's load the shows for each Channel.
			for(ChannelDefinition c : channel_definitions) {
				c.shows = Show.getShows(c.id);
			}
			
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
		return channel_definitions.toArray(new ChannelDefinition[channel_definitions.size()]);
	}

	
	public void assignShowToThisChannel(int show_id) {
		
		DBConnection db = new DBConnection("faketv");
		db.executeUpdate("delete from channel_shows where show_id = ?",show_id);

		try {
			db.executeUpdate("insert into channel_shows (show_id,channel_id) values(?,?)",show_id,this.id);
		} catch(Exception e) {
			System.out.println(e);
		}
	}
	
	

	
	
	public static Episode getRandomShow(boolean random_time) {
	    DBConnection db = new DBConnection("faketv");

	    Hashtable<String,Boolean> skip_folders = new Hashtable<String, Boolean>();
	    

	    
	    try {

	    	//We need to figure out how to find an unassigned show. 
	    	//Shows are assigned by folder, but videos aren't related directly to the folders.
	    	String folder = "";
	    	db.executeQuery("select folder from shows where id not in (select show_id from channel_shows) order by rand() limit 0,1");
	    	while(db.rs.next()) {
	    		folder = db.rs.getString("folder");
	    	}
	    	
	    	
		    //Now return a random show
		    db.executeQuery("select id, filename, duration_seconds, is_television, is_interlaced from videos where filename like ? limit 0,1",folder+"/%");
			while(db.rs.next()) {
				String filename = db.rs.getString("filename");
				
				double duration_seconds = db.rs.getDouble("duration_seconds");
				filename = filename.replace("/","\\");
				
				Episode cs = new Episode();
				cs.setFilename(filename);
				
				
				
				if (random_time) {
					cs.start_time = System.currentTimeMillis()-((int)(Math.random()*duration_seconds)*1000);
				}
				cs.duration_seconds = duration_seconds;
				cs.is_interlaced = (db.rs.getInt("is_interlaced") > 0);
				cs.is_television = (db.rs.getInt("is_television") > 0);
				cs.id = db.rs.getInt("id");
				return cs;
			}
	    } catch(Exception e) {
	    	throw new RuntimeException(e);
	    }
	    //If nothing found, return null
	    return null;
	}
	
	
	
	
	/**
	 * Grab an episode. If random_time is true, then we'll start the show at a random time to simulate something being in progress. Otherwise we'll start from the beginning.
	 * @param random_time
	 * @return
	 */
	public Episode getEpisode(boolean random_time) { //Channel number and start time can be used later to lookup programming types, to start.. all random.
		try {
		    DBConnection db = new DBConnection("faketv");
		    
		    if (shows.length==0) {
		    	return getRandomShow(random_time);
		    }
	    	System.out.println("Have shows: "+shows.length);
		    
		    //Get the show from our randomly ordered round robin list
		    Show s = this.shows[show_index];
		    show_index++;
		    if (show_index>=this.shows.length) {
		    	show_index=0;
		    }
	
		    
		    //Get a count of all the episodes
		    int episode_count = 0;
		    String show_folder = s.folder;
		    db.executeQuery("select count(*) cnt from videos where filename like ?",show_folder+"/%");
		    while(db.rs.next()) {
		    	episode_count = db.rs.getInt("cnt");
		    }
		    
		    
		    
		    int episode_to_select = (int)(s.getRandom()*episode_count); //We use the stored random so that it doesn't jump around.
		    db.executeQuery("select id, filename, duration_seconds, is_television, is_interlaced from videos where filename like ? limit ?,1",show_folder+"/%",episode_to_select);
		    Episode ep = null;
			while(db.rs.next()) {
				String filename = db.rs.getString("filename");
				
				double duration_seconds = db.rs.getDouble("duration_seconds");
				filename = filename.replace("/","\\");
				
				ep = new Episode();
				ep.setFilename(filename);
				ep.start_time = System.currentTimeMillis()-((int)(Math.random()*duration_seconds)*1000);
				ep.duration_seconds = duration_seconds;
				ep.is_interlaced = (db.rs.getInt("is_interlaced") > 0);
				ep.is_television = (db.rs.getInt("is_television") > 0);
				ep.id = db.rs.getInt("id");
				return ep;
			}

			//If we got here, we don't have anything, so add something random
			return getRandomShow(random_time);
			
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
		
	}
	
	
	
	

}
