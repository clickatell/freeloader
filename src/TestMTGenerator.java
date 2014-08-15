import java.util.Properties;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Layout;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;


public class TestMTGenerator {
	public static int MAX_LOG_EPS = 3;
	static Logger log = Logger.getLogger("TestMTGenerator");
	private static Properties properties = null;
	
	private HTTPClientProcessor mtGernerator = null;
	
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

	
	public TestMTGenerator(Properties props) {
		properties = props;
		String mt_url = getProp("mt_url", "http://127.0.0.1:8000/http/sendmsg");
		int num_mt_msgs = getPropAsInt("num_mt_msgs", 20000);

		try {
			
			//MT processor
			this.mtGernerator = new HTTPClientProcessor(
					"MTgen",
					1000,
					mt_url,
					getPropAsInt("mt_timeout", 10000),
					getPropAsInt("mt_threads", 10)
					);
			
			// load up the queue
			for (int i = 0; i < num_mt_msgs; i++) {
				ClickMessage msg = new ClickMessage(Utils.getRandomMSISDN(), Utils.getRandomMSISDN(), "12345", "test", "1234", "Hello world " + Long.toString((long)(Long.MAX_VALUE * Math.random())));
				msg.setDelivAck("1");
				this.mtGernerator.addToQueue(msg);
			}
			
			// Wait for the queue to clear
			while (this.mtGernerator.getQueueSize() > 0) {
				Thread.sleep(500);
				
				log.info("TestMTGenerator queue: " + this.mtGernerator.getQueueSize());
			}
			
			// shutdown
			this.mtGernerator.shutdown();
	    
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
		TestMTGenerator mtg = null;
		
		try {
			//first see if there are another config file path spcified 
			Properties props = Utils.getPropertiesFromArgs(args, null);
	
			mtg = new TestMTGenerator(props);
			
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
