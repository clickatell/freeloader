import java.net.InetSocketAddress;
import java.util.Properties;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Layout;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import com.sun.net.httpserver.HttpServer;


public class TestMOReceiver implements ClickMessageHandler{
	public static int MAX_LOG_EPS = 3;
	static Logger log = Logger.getLogger("MOReceiver");
	
	private static Properties properties = null;
	private HttpServer httpServer = null;
	
	
	public static synchronized String getProp(String name, String default_value) {
		if (properties == null) {
			return default_value;
		} else {
			return properties.getProperty(name, default_value);
		}
	}

	public static synchronized long getPropAsLong(String name, long default_value) {
		String value = getProp(name, null);
		
		if (value == null) {
			return default_value;
		} else {
			return Long.parseLong(value);
		}
	}
	
	public static synchronized int getPropAsInt(String name, int default_value) {
		String value = getProp(name, null);
		
		if (value == null) {
			return default_value;
		} else {
			return Integer.parseInt(value);
		}
	}

	
	@Override
	public String onClickMessage(ClickMessage request) throws ClickApiException {
		return "";
		// nothing to do
	}


	public TestMOReceiver(Properties props) {
		properties = props;
		int port = getPropAsInt("port", 8002);
		String path = getProp("url_path", "/http/mo_callback");

		try {
			//setup 
			log.info("Starting http service listening on port " + port);
			this.httpServer = HttpServer.create(new InetSocketAddress(port), 0);	//this aint no tomcat... beware!
			this.httpServer.createContext(path, new HttpRequestHandler(
					this, 
					log,
					"MO",
					60 * 60 * 24
					));
			this.httpServer.setExecutor(null); 
			this.httpServer.start();

			/*
			 * hang around
			 */
			while (true) {
				try {Thread.sleep(1000);} catch (InterruptedException e1) {}
			}
	    
		} catch (Exception e) {  	//ai
			log.error(e); 	
		}

		
	}
	
	/**
	 * 
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		TestMOReceiver cbr = null;
		
		try {
			//first see if there are another config file path spcified 
			Properties props = Utils.getPropertiesFromArgs(args, null);
	
			cbr = new TestMOReceiver(props);
			
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
	}

	
	
	/* static inits*/
	static {
		Layout l = new PatternLayout("%d %p %m%n");
		log.addAppender(new ConsoleAppender(l));
	}



}
