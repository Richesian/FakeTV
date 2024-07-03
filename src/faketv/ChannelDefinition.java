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
	public boolean is_derived;
	public int wildcard_id;
	private String channel_list;
	
	public static ChannelDefinition[] getChannelDefinitions() {
		
		makeAllShowIDs();
		
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
			
			//Load up wildcard channels
			int virtual_number = 5000;
			db.executeQuery("select id, title, descr, channel_list from wildcard_channels order by rand()");
			while(db.rs.next()) {
				ChannelDefinition c = new ChannelDefinition();
				c.id = virtual_number;
				c.wildcard_id = db.rs.getInt("id");
				c.title = db.rs.getString("title");
				c.descr = db.rs.getString("descr");
				c.channel_list = db.rs.getString("channel_list");
				c.is_derived = true;
				channel_definitions.add(c);
				virtual_number++;
			}
			
			for(ChannelDefinition c : channel_definitions) {
				if (!c.is_derived){continue;}
				c.shows = Show.getShows(c.channel_list);
			}
			
					
			
			
			
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
		return channel_definitions.toArray(new ChannelDefinition[channel_definitions.size()]);
	}

	//Sometimes a new video might not have a show ID, we'll do this at startup to make sure we have everything
	private static void makeAllShowIDs() {
		try {
			DBConnection db = new DBConnection("faketv");
			
			//Find existing
			Hashtable<String,Boolean> existing_shows = new Hashtable<String,Boolean>();
			db.executeQuery("select folder from shows");
			while(db.rs.next()) {
				existing_shows.put(db.rs.getString("folder"),true);
			}
			
			ArrayList<String> folders_to_add = new ArrayList<String>();
			db.executeQuery("select filename from videos");
			while(db.rs.next()) {
				String folder = db.rs.getString("filename");
				folder = folder.replace("\\","/");
				folder = folder.substring(0,folder.lastIndexOf("/"));
				if (existing_shows.containsKey(folder)){continue;}
				folders_to_add.add(folder);
				existing_shows.put(folder,true);
			}
			
			for(String folder:folders_to_add) {
				System.out.println("Creating show id for: "+folder);
				db.executeUpdate("insert into shows (folder) values(?)",folder);
			}
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
		
		
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
	    

	    System.out.println("Getting random show.");
	    try {

	    	//We need to figure out how to find an unassigned show. 
	    	//Shows are assigned by folder, but videos aren't related directly to the folders.
	    	String folder = "";
	    	db.executeQuery("select folder from shows where id not in (select show_id from channel_shows) order by rand() limit 0,1");
	    	while(db.rs.next()) {
	    		folder = db.rs.getString("folder");
	    	}
	    	System.out.println("Got the folder "+folder);

	   
	    	
	    	
	    	
	    	
		    //Now return a random show
		    db.executeQuery("select id from videos where filename like ? and filename not like ? limit 0,1",folder+"/%",folder+"/%/%");
			while(db.rs.next()) {
				Episode ep = new Episode();
				ep.id = db.rs.getInt("id");
				ep.loadDetails();
				
				if (random_time) {
					
					int random_seconds = (int)(Math.random()*ep.duration_seconds);
					if (ep.duration_seconds-random_seconds<180) {
						random_seconds = (int) ep.duration_seconds-180;
					}
					ep.start_time = System.currentTimeMillis()-((int)(random_seconds)*1000);
				}
				return ep;
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
	    //	System.out.println("Have shows: "+shows.length);
		    
	    	
	    	Show s = this.shows[show_index];
		    show_index++;
		    if (show_index>=this.shows.length) {
		    	show_index=0;
		    }
	    	
		    
		    Episode ep = null;
		    
		    //If there was no episode, then pick one randomly.
		    if (s.last_played_episode==null) {
		    	
		    	System.out.println("Picking random episode");
		    	
		        //Get a count of all the episodes
			    int episode_count = 0;
			    String show_folder = s.folder;
			    db.executeQuery("select count(*) cnt from videos where filename like ?",show_folder+"/%");
			    while(db.rs.next()) {
			    	episode_count = db.rs.getInt("cnt");
			    }
			    
			    
			    
			    int episode_to_select = (int)(s.getRandom()*episode_count); //We use the stored random so that it doesn't jump around.
			    
			    db.executeQuery("select id from videos where filename like ? limit ?,1",show_folder+"/%",episode_to_select);
				while(db.rs.next()) {
					ep = new Episode();
					ep.id = db.rs.getInt("id");
					ep.loadDetails();
					if (random_time) {
						ep.start_time = System.currentTimeMillis()-((int)(Math.random()*ep.duration_seconds)*1000);
					}
				}
		    } else { //Otherwise, we'll pick the next show that works.
		    	
		    	String db_sort_name = getSortValueFromFilename(s.last_played_episode);
		    	
		    	System.out.println("Attempting to get next episode after: "+s.last_played_episode+", "+db_sort_name);

		    	
		    	String lowest_sort_name = "";
		    	int lowest_file_id=-1;
		    	String lowest_actual_sort_name="";
		    	int lowest_actual_file_id=-1;
		    	int file_id = -1;
			    db.executeQuery("select id, filename from videos where filename like ?",s.folder+"/%");
				while(db.rs.next()) {
					
					file_id = db.rs.getInt("id");
					String filename = db.rs.getString("filename");
					String sort_name = getSortValueFromFilename(filename);
					
					
					if (lowest_actual_sort_name.equals("")) {
						lowest_actual_sort_name = sort_name;
						lowest_actual_file_id = file_id;
					}
					if (sort_name.compareTo(lowest_actual_sort_name)<0) {
						lowest_actual_sort_name = sort_name;
						lowest_actual_file_id = file_id;
					}
					System.out.println("Sort name: "+sort_name+",Actual: "+db_sort_name);
					
					
					//Skip if the filename is less then or equal to the db_sort_name
					if (sort_name.compareTo(db_sort_name)<=0) {
						continue;
					}
					
					//If the lowest_sort name is blank, then this is our first good candidate.
					if (lowest_sort_name.equals("")) {
						lowest_sort_name = sort_name;
						lowest_file_id = file_id;
						continue;
					}
					
					//If this one is less than our current lowest, then let's use it
					if (sort_name.compareTo(lowest_sort_name)<0) {
						lowest_sort_name = sort_name;
						lowest_file_id = file_id;
					}
				}
				
				//Use the file id that was lowest, otherwise just let whatever file_id was captured be the one
		    	if (lowest_file_id!=-1) {
		    		file_id = lowest_file_id;
		    	} else {
		    		file_id = lowest_actual_file_id;
		    	}
		    	
		    	
		    	System.out.println("Sort name: "+lowest_sort_name);
		    	System.out.println("File ID: "+file_id);
		    	try {
		    	ep = new Episode();
		    	ep.id = file_id;
		    	System.out.println("Loading details");
				ep.loadDetails();
				if (random_time) {
					ep.start_time = System.currentTimeMillis()-((int)(Math.random()*ep.duration_seconds)*1000);
				}
				System.out.println("Got the details");
		    	} catch(Exception eload){}
		    }
		    
		    //If we still don't have an episode, grab a random one
		    if (ep==null) {
		    	System.out.println("Getting random show");
		    	ep = getRandomShow(random_time);
		    }

		    
		    
		    if (ep==null){return null;}
		    
		    
		    
		    //Update our last episode as this one
		    System.out.println("Setting last played to: "+ep.getFilename());
		    s.setLastPlayedEpisode(ep.getFilename());
		    
		    
		    
	
		    
		    return ep;
			
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
		
	}
	
	public String getSortValueFromFilename(String s) {
		s = s.replace("\\","/");
		if (s.indexOf("/")!=-1) {
			try {
				s = s.substring(s.lastIndexOf("/")+1);
			} catch(Exception e){}
		}
		
		//We need to find s01.e01, or s1e1, etc.
		//Basically we want to replace everything that isn't one of those with something else.
		//We'll be dumb about it
		boolean in_capture_mode = false;
		char[] chars = s.toCharArray();
		StringBuilder capture = new StringBuilder();
		StringBuilder sort_name = new StringBuilder();
		for(int i=0;i<chars.length;i++) {
			char c = chars[i];
			if (in_capture_mode) {
				if (c>='0'&&c<='9') { //Capture mode will stay going until it finds something that isn't a number
					capture.append(c);
					System.out.println("Capturing: "+c);
					continue;
				}
			}
			in_capture_mode = false;
			if (capture.length()>0) {
				int val = Integer.valueOf(capture.toString());
				capture.setLength(0);
				sort_name.append("X"+zeropad(""+val));
			}

			if (c=='e'||c=='E'||c=='s'|c=='S') {
				in_capture_mode = true;
			}
		}
		return sort_name.toString();
		
		
	}
	
	
    public String zeropad(String s) {
    	
    	while(s.length()<4) {
    		s = "0"+s;
    	}
    	return s;
    }
	
}
