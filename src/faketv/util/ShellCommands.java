package faketv.util;


import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;



public class ShellCommands {

	private String shell = null;
	public ShellCommands(String shell) {
		this.shell = shell;
	}
	private StringBuilder input_sb = new StringBuilder();
	private StringBuilder error_sb = new StringBuilder();
	private InputStream error_stream = null;
	private InputStream input_stream = null;
	private Process proc = null;
	private OutputStream output_stream = null;
	
	
	
	
	public int execute(String... cmd) {
		ProcessBuilder pb = new ProcessBuilder(cmd);
		try {
			proc=pb.start();
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
		input_stream = proc.getInputStream();
		error_stream = proc.getErrorStream();

		
		
		while (proc.isAlive()) {
			try {
			if (input_stream.available()>0) {
				for(int i=0;i<input_stream.available();i++){
					input_stream.read();
				}
			}}catch(Exception es){}

			try {
			if (error_stream.available()>0) {
				for(int i=0;i<error_stream.available();i++){
					error_stream.read();
				}
			}}catch(Exception es2){}
		}
		try{input_stream.close();}catch(Exception e){}
		try{output_stream.close();}catch(Exception e){}
		
		
		return 0;
	}
	
	public String getErrorString() {
		return error_sb.toString();
	}
	
	public String getSTDOUTString() {
		return input_sb.toString();
	}
	
	
	
	public static String ext(File f) {
		if (f.getName().indexOf(".")==-1){return "";}
		return f.getName().substring(f.getName().lastIndexOf(".")).toLowerCase();
	}
		
	
	
	
	

	
	
	
	
	
}
