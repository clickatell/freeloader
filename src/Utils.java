import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Properties;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;


/**
 */
public class Utils {
	private static AtomicLong id_counter = new AtomicLong(0);
	
	
    public static String binToString(byte[] data) throws Exception {
        StringBuilder buf = new StringBuilder(128);

        for (int i=0; i < data.length; i++) {
            buf.append(Integer.toString((data[i] & 0xff) + 0x100, 16).substring(1));
        }
        return buf.toString();
    }
    
    
    public static String genMsgId() {
    	MessageDigest md = null;
    	try {
	    	md = MessageDigest.getInstance("MD5");
			return binToString(md.digest((Long.toString(System.currentTimeMillis()) + Long.toString(id_counter.incrementAndGet())).getBytes()));
		} catch (Exception e) {
			// should not happen due to static digest def above 
			return null;
		}
    }

    
	public static void queryAdd(StringBuilder buf, String name, String value) {
		if (buf.length() == 0) {
			buf.append('?');
		} else if (buf.charAt(buf.length()-1) != '?') {
			buf.append('&');
		}
		buf.append(name).append('=');
		
		try {
			if (value == null) {
				value = "";
			}
			buf.append(URLEncoder.encode(value, "UTF-8"));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();  //should not happen on static encoding ty
		}
	}

	/**
	 */
	public static void logThrottledException(Logger log, Class parent_class, String message, Throwable thrown, int eps_limit) {
		try {
			String name = parent_class.getSimpleName() + "." + thrown.getClass().getSimpleName();
			EventCounter ec = EventCounter.get(name);
			ec.inc();
			
			int eps = ec.getPerSecAvg();
			if (eps > eps_limit) {  
				if (ec.isNewSecond()) {
					log.error(message + " - Exception logging limited - exceptions per second is " + eps, thrown);
				}
			} else {
				log.error(message, thrown);
			}
		} catch (Exception e2) {
			log.error("Exception", e2);
		}
	}
	
	/**
	 * 
	 * @return
	 */
	public static String getDateTime() {
		TimeZone utc = TimeZone.getTimeZone("UTC");
		SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-ddHH:mm:ss");
		f.setTimeZone(utc);
		GregorianCalendar cal = new GregorianCalendar(utc);
		f.setCalendar(cal);
		return f.format(new Date());
	}
	
	/**
	 */
	public static String getPropertyPath(String ... key) {
		StringBuilder buf = new StringBuilder(256);
		
		for (int i = 0; i < key.length; i++) {
			if (i != 0 ) {
				buf.append('.');
			}
			buf.append(key[i]);
		}
		
		return buf.toString();
	}
	
	
	/**
	 * Gets the properties from a properties file
	 */
	public static Properties getPropertiesFromFile(String file, Properties props) {
		if (props == null) {
			props = new Properties();
		}
		FileInputStream fis = null;
		
		try {
			fis = new FileInputStream(file);
			props.load(fis);
			return props;
		} catch (IOException localIOException) {
			return props;
		} finally {
			if (fis != null) {
				try {
					fis.close();
				} catch (IOException e) {
					//
				}
			}
		}
		
	}

    /**
     * 
     * @param args
     * @return
     */
    public static Properties getPropertiesFromUrl(String url, Properties props) {
		if (props == null) {
			props = new Properties();
		}
		String params = url.substring(url.indexOf('?'));
		for (String nv : params.split("&")) {
			String[] nva = nv.split("=");
			if (nva.length == 2) {
				props.setProperty(nva[0], nva[1]);
			}
		}
		
		return props;
    }
    
    /**
     * 
     * @param args
     * @return
     */
    public static Properties getPropertiesFromArgs(String[] args, Properties props) {
		if (props == null) {
			props = new Properties();
		}
		for (int i = 0; i < args.length; i++) {
			int ix = args[i].indexOf('='); 
			if (ix != -1) {
				String name  = args[i].substring(0,ix).trim();
				String value = args[i].substring(ix+1).trim();
				
				if ((name.length() > 0) && (value.length() > 0)) {
					props.setProperty(name, value);
				}
			}
		}
		
		return props;
    }


    
	public static String getRandomMSISDN() {
//		StringBuilder buf = new StringBuilder(127).append("27");
//		for (int i = 0; i < 9; i++) {
//			buf.append(Math.ra"");
//		}
		Long l = (long) (Long.MAX_VALUE * Math.random());
		if (l < 1000000000) {
			l += 1000000000;
		}
		String s = Long.toString(l);
		
		return "27" + s.substring(0,9);
	}
    
	
}