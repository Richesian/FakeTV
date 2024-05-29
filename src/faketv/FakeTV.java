package faketv;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Hashtable;

import javax.swing.JFrame;
import javax.swing.JPanel;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.BaseTSD;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinDef.HINSTANCE;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinDef.LPARAM;
import com.sun.jna.platform.win32.WinDef.LRESULT;
import com.sun.jna.platform.win32.WinDef.WPARAM;
import com.sun.jna.platform.win32.WinUser;
import com.sun.jna.platform.win32.WinUser.HHOOK;
import com.sun.jna.platform.win32.WinUser.KBDLLHOOKSTRUCT;
import com.sun.jna.platform.win32.WinUser.LowLevelKeyboardProc;
import com.sun.jna.platform.win32.WinUser.MSG;

import faketv.db.DBConnection;
import faketv.db.Settings;
import faketv.util.ShellCommands;



public class FakeTV extends JPanel implements WindowListener,WindowFocusListener, Runnable {
	
	
	Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
	int screen_width = (int) screenSize.getWidth();
	int screen_height = (int) screenSize.getHeight();


	private static final long serialVersionUID = 1L;
	private static GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();	
	private static GraphicsDevice gd;
	private static JFrame window = null;
	private boolean not_closed=true;
	private ShellCommands sc = new ShellCommands("");

	private static boolean is_running=false;

	private static LowLevelKeyboardProc keyboardHook;
	private static HHOOK hookHandle = null;
	private int current_channel = 0;
	private ChannelDefinition[] channel_definitions;
	
	private boolean assign_show_to_this_channel = false;
	private boolean get_random_program = false;
	private boolean no_program_change = false;
	private boolean was_killed = false;
	
	
	
	private Episode[] channels;
    
	public FakeTV() {
		reload();
	}

	
	public void reload() {
		channel_definitions = ChannelDefinition.getChannelDefinitions();
		System.out.println("Channel definitions: "+channel_definitions.length);
        channels = new Episode[channel_definitions.length]; // build our channels list based on how many channels we have.
        populateChannels(true);
	}
	
	private Episode GetEpisode() {
		populateChannels(false);
		this.get_random_program = false;
		return channels[current_channel];
	}
	
	private void sendKey(int keycode) {
	    HWND handler = User32.INSTANCE.FindWindow(null, "VLC");
	  //  User32.INSTANCE.SetForegroundWindow( handler );
	    
	       WinUser.INPUT input = new WinUser.INPUT(  );

           input.type = new WinDef.DWORD( WinUser.INPUT.INPUT_KEYBOARD );
           input.input.setType("ki"); // Because setting INPUT_INPUT_KEYBOARD is not enough: https://groups.google.com/d/msg/jna-users/NDBGwC1VZbU/cjYCQ1CjBwAJ
           input.input.ki.wScan = new WinDef.WORD( 0 );
           input.input.ki.time = new WinDef.DWORD( 0 );
           input.input.ki.dwExtraInfo = new BaseTSD.ULONG_PTR( 0 );

           // Press "a"
           input.input.ki.wVk = new WinDef.WORD(keycode); 
           input.input.ki.dwFlags = new WinDef.DWORD( 0 );  // keydown

           
           
           User32.INSTANCE.SendInput( new WinDef.DWORD( 1 ), ( WinUser.INPUT[] ) input.toArray( 1 ), input.size() );

           // Release "a"
           input.input.ki.wVk = new WinDef.WORD(keycode); 
           input.input.ki.dwFlags = new WinDef.DWORD( 2 );  // keyup

           User32.INSTANCE.SendInput( new WinDef.DWORD( 1 ), ( WinUser.INPUT[] ) input.toArray( 1 ), input.size() );

	    
	    
	   // System.out.println("Handler: "+handler);
	   // System.out.println("Sending key: "+keycode);
	    // 0x0100 WM_KEYDOWN
//	    User32.INSTANCE.SendMessage(handler, 0x0104, new WinDef.WPARAM(keycode), new WinDef.LPARAM(0));

	    // recommended for dedection
		
		
	}
	
	private void incrementChannel(int amount) {
		int new_current_channel = current_channel;
		new_current_channel+=amount;
		if (new_current_channel<0) {
			new_current_channel = channels.length-1;
		} else if (new_current_channel>= channels.length) {
			new_current_channel = 0;
		}
		current_channel = new_current_channel;
	}
	
	
	public void populateChannels(boolean is_init) {

		//During init, we'll just populate random episodes. This sould never ever get a null. So make sure channel definitions returns a random show if nothing can be found.
		if (is_init) {
			for(int i=0;i<channels.length;i++) {
				System.out.println(channel_definitions[i]);

				ChannelDefinition cd = channel_definitions[i];
				
				channels[i] = cd.getEpisode(true); 
			}
			return;
		}
		
		//Now, replace any shows that are over or null (forcibly over)
		for(int i=0;i<channels.length;i++) {
			if (channels[i]==null||channels[i].isExpired()) {
				System.out.println("Replacing expired show.");
				channels[i] = channel_definitions[i].getEpisode(false); 
			}
		}
	}
	




		public void paint(Graphics g) {
				Graphics2D g2 = (Graphics2D) g;
			   g2.setColor(Color.BLACK);
			   g2.setBackground(Color.BLACK);
			   g2.clearRect(0, 0, screen_width,  screen_height);
				

		}

	  	
		
	  
		
		
		
		
		
		
		
		
		
   public void DoThings() {
	   gd = ge.getDefaultScreenDevice();
	   System.out.println("Starting");
	   
	   window = new JFrame();
	   window.add(this);
       window.addWindowListener(this);
       window.setIgnoreRepaint(false);
	   window.addWindowFocusListener(this);
	    window.setSize(this.screen_width, this.screen_height);
	    window.setTitle("RichVU");
	    window.setUndecorated(true);
	    window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	    
	  
	    
		//Hide the cursor
		 Toolkit toolkit = Toolkit.getDefaultToolkit();
		    Point hotSpot = new Point(0,0);
		    BufferedImage cursorImage = new BufferedImage(1, 1, BufferedImage.TRANSLUCENT); 
		    Cursor invisibleCursor = toolkit.createCustomCursor(cursorImage, hotSpot, "InvisibleCursor");        
		    window.setCursor(invisibleCursor);
		
		//	gd.setFullScreenWindow(window);
		

		
		

		Thread t = new Thread(this);
		t.start();


		
	    window.setVisible(true);         
	    
	    DBConnection db = new DBConnection("antiharpist");
	    
	    

	    this.repaint();

	    WinDef.HWND shellTray = User32.INSTANCE.FindWindow("Shell_TrayWnd", "");
	    User32.INSTANCE.ShowWindow(shellTray, 0);
		User32.INSTANCE.SetCursorPos(5000, 5000);
	    
		
		String ffplay = Settings.getFFPlayPath();

		
		Episode show = null;
		while (true) {

			//We sometimes want to change the channel without changing the show. This lets us move a show to a different channel.
			if (no_program_change) {
				no_program_change = false;
			} else if (get_random_program) {
				get_random_program = false;
				show = ChannelDefinition.getRandomShow(true);
			} else {
				show = this.GetEpisode();
			}
			
			ArrayList<String> cmd = new ArrayList<String>();
			cmd.add(ffplay);
			cmd.add("--start-time");
			cmd.add(show.getProgressTimeString());
			cmd.add("--video-on-top");
			cmd.add("--play-and-exit");
			cmd.add("--fullscreen");
			cmd.add("--quiet");
			cmd.add("--dummy-quiet");
			cmd.add("-I");
			cmd.add("dummy");
			
			cmd.add(show.getFilename());
			
		//	cmd.add("-vf");
		
			//Need to figure out the current seconds
			int start_seconds = show.getStartSeconds()-30;
			int end_seconds = start_seconds+31;
			
			
			ChannelDefinition def = channel_definitions[current_channel];
			String text = ""+def.id+" - "+def.title;
			
			
			//text = text.replace("\"", "\\\"");
			cmd.add("--sub-source=marq{marquee='"+text+"',position=9,color=0xFFFF00,size=40}");
			
			
//			StringBuilder vf = new StringBuilder();
	//		cmd.add("-vf");
		//	vf.append("drawtext=boxcolor=black:boxborderw=10:borderw=15:fontfile=c\\\\:/Windows/fonts/calibri.ttf:fontsize=(h/15):fontcolor=cyan:x=10:y=10:text="+text+":enable='between(t,"+start_seconds+","+end_seconds+")'");

			if (show.is_interlaced) {
				cmd.add("--deinterlace=1");
				cmd.add("--deinterlace-mode=yadif2x");
				
//				vf.append(",yadif=1");
			}
//			cmd.add("\""+vf.toString()+"\"");
			
			//drawtext=boxcolor=black:boxborderw=10:borderw=15:fontfile=c\\\\:/Windows/fonts/calibri.ttf:fontsize=(h/15):fontcolor=cyan:x=10:y=10:text="+text+":enable='between(t,"+start_seconds+","+end_seconds+")'\"");
			
			System.out.println("Playing "+show.getFilename());
			
			
			String[] cmd_args = cmd.toArray(new String[cmd.size()]);
			
			this.was_killed = false;
			//rvu.sc.execute("j:\\ffplay.exe", "-ss",""+show.getProgressTimeString(),"-alwaysontop","-autoexit","-fs",show.filename);
			this.sc.execute(cmd_args);
			
			if (!is_running) {
				break;
			}
			if (assign_show_to_this_channel) {
				channel_definitions[current_channel].assignShowToThisChannel(show.getShowID());
				this.reload();
				assign_show_to_this_channel = false;
			}
			
			
			if (!this.was_killed) { //If it ended or was fast forwarded to the end.. then remove the first element
				channels[current_channel] = null;
			}

		}
		
		
	    User32.INSTANCE.ShowWindow(shellTray, 1);
	    
	    window.dispose();
	   
   }
		
		
	
	
	public static void main(String[] args) {
		new FakeTV().DoThings();
	}
	
	
	/**
	 * 
	 * @param k
	 * @return True if we don't eat the key, otherwise false.
	 */
	private boolean handleKeyDown(int k) {
		System.out.println("Key: "+k);
		if (k==27) { //Escape
			System.out.println("Escape hit");
			is_running = false;
			was_killed = true;
//			sc.kill();
			sendKey(83); //was 113
		} else if (k==82) { //r
			was_killed = true;

			//This will get a random show to fill the current slot
			get_random_program = true;
			
			//sc.kill();
			sendKey(83);
			return false;
			
		} else if (k==221) {//square bracket left ]
			this.incrementChannel(1);
			was_killed = true;
			sendKey(83);
			return false;

			//sc.kill();
		} else if (k==219) { //square bracked left [
			this.incrementChannel(-1);
			was_killed = true;
			sendKey(83);
			return false;

			//sc.kill();
		} else if (k==68) { //d
			try {
			channels[current_channel].toggleInterlaced();
			} catch(Exception e){}
			was_killed = true;
//			sc.kill();
			sendKey(83);
			return false;

		} else if (k==190) { //period . This means assign the current show to this channel at all times
			assign_show_to_this_channel = true;
			was_killed = true;
			sendKey(83);
			return false;

		} else if (k==38) { //Arrow up
			no_program_change = true;
			was_killed = true;
			this.incrementChannel(1);
			sendKey(83);
			return false;
		} else if (k==40) { //Arrow down
			no_program_change = true;
			was_killed = true;
			this.incrementChannel(-1);
			sendKey(83);
			return false;
		}
		return true;

	}
	
	
	
	
	
	public void closeProject() {
		try {
		//repaint();
		}catch(Exception e){}
	}
	

	public String randTime() {
		int minute = (int)(15*Math.random());
		int second = (int)(60*Math.random());
		return ""+minute+":"+second;
	}
	

	


	public static String zeroPad(String s, int places) {
		StringBuffer sb = new StringBuffer();
		if (s == null)
			s = "";
		int zeroes_to_add = places - s.length();
		if (zeroes_to_add <= 0)
			return s;
		for (int i = 0; i < zeroes_to_add; i++) {
			sb.append("0");
		}
		return sb.toString() + s;
	}
	

	

	
	
	
	
	







	@Override
	public void windowOpened(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}


	private void stopThings() {
		System.out.println("Stopping Things");
		this.not_closed = false;
	}






	@Override
	public void windowClosing(WindowEvent e) {
		// TODO Auto-generated method stub
		stopThings();
	}








	@Override
	public void windowClosed(WindowEvent e) {
		System.out.println("Window closed event");

	}








	@Override
	public void windowIconified(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}








	@Override
	public void windowDeiconified(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}








	@Override
	public void windowActivated(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}








	@Override
	public void windowDeactivated(WindowEvent e) {
		// TODO Auto-generated method stub
	}



	@Override
	public void windowGainedFocus(WindowEvent e) {

	}



	@Override
	public void windowLostFocus(WindowEvent e) {

	}
	
	
	
	
	


	@Override
	public void run() {
		
		   //Setup windows event handler for keyboard keys
	       HINSTANCE moduleHandle = Kernel32.INSTANCE.GetModuleHandle(null);
	        LowLevelKeyboardProc keyboardHook = new LowLevelKeyboardProc() {
	        	
	            @Override
	            public LRESULT callback(int nCode, WPARAM wParam, KBDLLHOOKSTRUCT info) {
	                // LowLevelKeyboardProc docs: "If nCode is less than zero, the hook
	                // procedure must pass the message to the CallNextHookEx function
	                // without further processing and should return the value returned
	                // by CallNextHookEx."
	                
	            	boolean no_eat_key = true;
	            	
	                if (nCode >= 0) {
	                    switch (wParam.intValue()) {
	                        // alternatively WM_KEYUP and WM_SYSKEYUP
	                        case WinUser.WM_KEYDOWN:
	                        case WinUser.WM_SYSKEYDOWN:
	                        	no_eat_key = handleKeyDown(info.vkCode);
	                    }
	                    if (!is_running) {
	                    	User32.INSTANCE.UnhookWindowsHookEx(hookHandle);
	                    	User32.INSTANCE.PostQuitMessage(0);
	                    }
	                }

	                if (!no_eat_key) {
	                	return new LRESULT(1);
	                }
	                
	                Pointer ptr = info.getPointer();
	                long peer = Pointer.nativeValue(ptr);
	                return User32.INSTANCE.CallNextHookEx(hookHandle, nCode, wParam, new LPARAM(peer));
	            }
	        };
	        hookHandle = User32.INSTANCE.SetWindowsHookEx(User32.WH_KEYBOARD_LL, keyboardHook, moduleHandle, 0);
		
		
		
	        is_running = true;
	        System.out.println("Please press any key ...");
	        int result;
	        MSG msg = new MSG();
	
	        while (is_running) {
 	        	System.out.println("Is running: "+is_running);
	        	System.out.println("Right before calling getMessage");
	        	result = User32.INSTANCE.GetMessage(msg, null, 0, 0);
	        	System.out.println("Result: "+result);
	        	if (result>0) {
	        		User32.INSTANCE.TranslateMessage(msg);
	        		User32.INSTANCE.DispatchMessage(msg);
	        	}
	        }
		
	}
	

	
	
	

		
	
	
}