import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Properties;
import java.util.Timer;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Layout;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import com.sun.net.httpserver.HttpServer;


public class FreeLoader implements ClickMessageHandler{
	public static int MAX_LOG_EPS = 3;
	static Logger log = Logger.getLogger("FreeLoader");
	
	// __General
	//config_file_path=freeloader.conf
	public static final String config_file_path = "config_file_path"; 
	//log_file_path=freeloader.log
	public static final String log_file_path = "log_file_path"; 

	// __MTs
	//mt.listen_port=8000
	public static final String mt_listen_port = "mt.listen_port"; 
	//mt.url_path=/http/sendmsg
	public static final String mt_url_path = "mt.url_path"; 
	//mt.percentage_failed=5
	public static final String mt_percentage_failed = "mt.percentage_failed"; 
	//mt.response.max_delay_seconds=0
	public static final String mt_response_max_delay_seconds = "mt.response.max_delay_seconds"; 
	//mt.user=test
	public static final String mt_user = "mt.user"; 
	//mt.password=test
	public static final String mt_password = "mt.password"; 
	//mt.api_id=test
	public static final String mt_api_id = "mt.api_id"; 
	
	// __MT callbacks
	//mt.callback.url=http://127.0.0.1/mt_callback
	public static final String mt_callback_url = "mt.callback.url"; 
	//mt.callback.response.max_delay_seconds=0
	public static final String mt_callback_response_max_delay_seconds = "mt.callback.response.max_delay_seconds"; 
	//mt.callback.timeout=10000
	public static final String mt_callback_timeout = "mt.callback.timeout"; 
	//mt.callback.threads=10
	public static final String mt_callback_threads = "mt.callback.threads"; 
	 
	// __MTs resulting in MOs
	//mt.percentage_mo=30
	public static final String mt_percentage_mo = "mt.percentage_mo"; 
	 
	// __MO config
	//mo.url=http://127.0.0.1/mo_callback
	public static final String mo_url = "mo.url"; 
	//mt.timeout=10000
	public static final String mo_timeout = "mt.timeout"; 
	//mo.threads=10
	public static final String mo_threads = "mo.threads"; 
	//mo.response.max_delay_seconds=0
	public static final String mo_response_max_delay_seconds = "mo.response.max_delay_seconds"; 
	//mo.short_long_code=304050
	public static final String mo_short_long_code = "mo.short_long_code"; 
	//mo.test.number_of_mo_messages=0
	public static final String mo_test_number_of_mo_messages = "mo.test.number_of_mo_messages";
	//mo.text=
	public static final String mo_text = "mo.text";

	
	
	private static Properties properties = null;
	private HttpRequestHandler mtHandler = null; 
	private HttpServer httpServer = null;
	private HTTPClientProcessor mtCallbackProcessor = null;
	private HTTPClientProcessor moProcessor = null;
	private String mt_usr;
	private String mt_passwd;
	private String mt_api;
	private int mt_percent_fail;
	private int mt_response_delay;
	private String mo_code;
	private int mo_test_num_msgs;
	private int mt_percent_mo;
	private String mo_txt;
	private Timer timer = new Timer();
	private int  mt_callback_percentage_fail;
	
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

		// user validation for MT messages? TODO
		
		// fail?
		if (this.mt_percent_fail > 0) {
			if (Math.random() * 100 < mt_percent_fail) {
				throw new ClickApiException("001");
			}
		}
		
		// delay?
		if (this.mt_response_delay > 0) {
			try {
				Thread.sleep((long)((this.mt_response_delay * 1000) * Math.random()));
			} catch (Exception e1) {
				//dont care
			}
		}

		// Delivery report?
		if (request.getDelivAck() != null && Integer.parseInt(request.getDelivAck()) == 1) {
			String rc = "004";
			if (Math.random() * 100 < mt_callback_percentage_fail) {
				rc = "005";
			}

			this.mtCallbackProcessor.addToQueue(new ClickMessage(
					request.getTo(),
					request.getFrom(),
					request.getApiMsgId(),
					request.getCliMsgId(),
					rc,
					0.8
					));
		}
		
		if (this.mt_percent_mo > 0) {
			if (Math.random() * 100 < mt_percent_mo) {
				this.moProcessor.addToQueue(new ClickMessage(
						this.mo_code,
						request.getFrom(),
						request.getApiId(),	//TODO ?
						Utils.genMsgId(),
						((this.mo_txt != null) ? this.mo_txt : Long.toString((long)(Math.random() * Long.MAX_VALUE)))
						));
			}
		}
		
		return request.getApiMsgId();
	}


	public FreeLoader(Properties props) {
		properties = props;
		Timer timer = new Timer();

		int mt_port = getPropAsInt(mt_listen_port, 8003);
		String mt_path = getProp(mt_url_path, "/http/sendmsg");
		int hist_size = getPropAsInt("hist_size", 60);
				
		try {
			
			this.mt_usr = getProp(mt_user, "test"); 
			this.mt_passwd = getProp(mt_password, "1234"); 
			this.mt_api = getProp(mt_api_id, "12345"); 
			this.mt_percent_fail = getPropAsInt(mt_percentage_failed, 0); 
			this.mt_response_delay = getPropAsInt(mt_response_max_delay_seconds, 0);
			this.mt_callback_percentage_fail = getPropAsInt("mt_callback_percentage_fail", 50);	  
			
			this.mo_code = getProp(mo_short_long_code, "304050");
			this.mo_test_num_msgs = getPropAsInt(mo_test_number_of_mo_messages, 0);
			this.mt_percent_mo = getPropAsInt(mt_percentage_mo, 0);	  
			this.mo_txt = getProp(mo_text, "hello world");

			//MT setup 
			log.info("Starting http MT service listening on port " + mt_port);
			this.httpServer = HttpServer.create(new InetSocketAddress(mt_port), 0);	//this aint no tomcat... beware!
			this.mtHandler = new HttpRequestHandler(
					this, 
					log,
					"mt",
					hist_size
					);
			this.httpServer.createContext(mt_path, mtHandler);
			this.httpServer.setExecutor(null); 
			// start the server
			this.httpServer.start();

			//MT callback processor
			this.mtCallbackProcessor = new HTTPClientProcessor(
					"mt_callback",
					hist_size,
					//getProp(mt_callback_url, "http://192.168.0.1/freeloader/receive_mt_callback.php"),
					getProp(mt_callback_url, "http://127.0.0.1:8001/http/mt_callback"),
					getPropAsInt(mt_callback_timeout, 10000),
					getPropAsInt(mt_callback_threads, 10)
					);
			
			//MO processor 
			this.moProcessor = new HTTPClientProcessor(
					"mo_callback",
					hist_size,
					//getProp(mo_url, "http://192.168.0.1/freeloader/receive_mo_callback.php"),
					getProp(mo_url, "http://127.0.0.1:8002/http/mo_callback"),
					getPropAsInt(mo_timeout, 10000),
					getPropAsInt(mo_threads, 10)
					);


			try {Thread.sleep(1000);} catch (InterruptedException e1) {}
			EventCounter.update();
			try {Thread.sleep(1000);} catch (InterruptedException e1) {}
			EventCounter.update();
			try {Thread.sleep(1000);} catch (InterruptedException e1) {}
			EventCounter.update();
			
			// graph
			try {Thread.sleep(1000);} catch (InterruptedException e1) {}
			UpdateStats t = new UpdateStats(this);
			try {Thread.sleep(1000);} catch (InterruptedException e1) {}
			timer.schedule(t, 1000, 1000);

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
		FreeLoader freeloader = null;
		
		try {
			//first see if there are another config file path spcified 
			Properties props = Utils.getPropertiesFromArgs(args, null);
			String config_file = props.getProperty(config_file_path, "etc/freeloader.conf");
			// start fresh with loading from this config file 
			props = Utils.getPropertiesFromFile(config_file, null);
			// overwrite with whatever is in arguments
			props = Utils.getPropertiesFromArgs(args, props);
	
			//System.out.println(props.toString());
			freeloader = new FreeLoader(props);
			
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
	}

	
	
	/* static inits*/
	static {
		try {
			Layout l = new PatternLayout("%d %p %m%n");
			log.addAppender(new FileAppender(l, "FreeLoader.log", true));
			log.addAppender(new ConsoleAppender(l));
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}



	protected HttpRequestHandler getMtHandler() {
		return mtHandler;
	}

	protected HTTPClientProcessor getMtCallbackProcessor() {
		return mtCallbackProcessor;
	}

	protected HTTPClientProcessor getMoProcessor() {
		return moProcessor;
	}



}
