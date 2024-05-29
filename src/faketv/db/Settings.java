package faketv.db;

import java.io.InputStream;
import java.util.Properties;

public class Settings {

	private static Properties props = null;
	
	private static void loadProperties() {
		if (props!=null){return;}
		Settings s = new Settings();
		InputStream is = null;
		try {
		
			is = Settings.class.getResourceAsStream("/faketv.properties");
			props = new Properties();
			props.load(is);
		} catch(Exception e) {
			throw new RuntimeException(e);
		} finally {
			try{is.close();}catch(Exception e){}
		}
	}
	
	
	public static String getFFPlayPath() {
		loadProperties();
		return props.getProperty("ffplay_path");
	}
	
	
}
