package faketv;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.awt.event.WindowListener;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.awt.image.MemoryImageSource;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinDef.HINSTANCE;
import com.sun.jna.platform.win32.WinDef.LPARAM;
import com.sun.jna.platform.win32.WinDef.LRESULT;
import com.sun.jna.platform.win32.WinDef.WPARAM;
import com.sun.jna.platform.win32.WinUser;
import com.sun.jna.platform.win32.WinUser.HHOOK;
import com.sun.jna.platform.win32.WinUser.KBDLLHOOKSTRUCT;
import com.sun.jna.platform.win32.WinUser.LowLevelKeyboardProc;
import com.sun.jna.platform.win32.WinUser.MSG;

import faketv.db.DBConnection;
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
	
    
	public FakeTV() {
		

	        
		




	}
	




		public void paint(Graphics g) {
				Graphics2D g2 = (Graphics2D) g;
			   g2.setColor(Color.BLACK);
			   g2.setBackground(Color.BLACK);
			   g2.clearRect(0, 0, screen_width,  screen_height);
				

		}

	  	
		
	  
	
	
	public static void main(String[] args) {
	   gd = ge.getDefaultScreenDevice();
	   System.out.println("Starting");
	   FakeTV rvu = new FakeTV();
	   
	   
	   window = new JFrame();
	   window.add(rvu);
       window.addWindowListener(rvu);
       window.setIgnoreRepaint(false);
	   window.addWindowFocusListener(rvu);
	    window.setSize(rvu.screen_width, rvu.screen_height);
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
		

		
		

		Thread t = new Thread(rvu);
		t.start();


		
	    window.setVisible(true);         
	    
	    
	    DBConnection db = new DBConnection("antiharpist");
	    
	    

	    rvu.repaint();

	    WinDef.HWND shellTray = User32.INSTANCE.FindWindow("Shell_TrayWnd", "");
	    User32.INSTANCE.ShowWindow(shellTray, 0);
		User32.INSTANCE.SetCursorPos(5000, 5000);
	    
		while (true) {
			db.executeQuery("select filename, duration_seconds from faketv order by rand() limit 0,1");
			try {
				while(db.rs.next()) {
					String filename = db.rs.getString("filename");
					double duration_seconds = db.rs.getDouble("duration_seconds");
					filename = filename.replace("/","\\");
					filename = filename.replace("\\mnt\\storage","\\\\klaus\\storage");

					System.out.println(filename);
					double random_duration = Math.random()*duration_seconds;
					System.out.println(random_duration);
					rvu.sc.execute("j:\\ffplay.exe", "-ss",""+random_duration,"-alwaysontop","-autoexit","-fs",filename);
				}
			} catch(Exception e) {System.out.println(e);}
			if (!is_running) {
				break;
			}
		}
		
		
	    User32.INSTANCE.ShowWindow(shellTray, 1);
	    
	    window.dispose();
		
	}
	
	
	
	private void handleKeyDown(int k) {
		System.out.println("Key: "+k);
		if (k==27) {
			System.out.println("Escape hit");
			is_running = false;

			sc.kill();
		} else if (k==106) {
			System.out.println("Killing process.");
			sc.kill();
			
		}
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
	                
	                if (nCode >= 0) {
	                    switch (wParam.intValue()) {
	                        // alternatively WM_KEYUP and WM_SYSKEYUP
	                        case WinUser.WM_KEYDOWN:
	                        case WinUser.WM_SYSKEYDOWN:
	                            handleKeyDown(info.vkCode);
	                    }
	                    if (!is_running) {
	                    	User32.INSTANCE.UnhookWindowsHookEx(hookHandle);
	                    	User32.INSTANCE.PostQuitMessage(0);
	                    }
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