import java.io.OutputStream;
import java.util.List;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.junit.runner.Request;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;



/**
 * HTTP MT Handler
 */
public class HttpRequestHandler implements HttpHandler{
	private EventCounter counter = null;
	private ClickMessageHandler parent = null;
	public Logger log = null;
	
	private EventCounter ecReqRecv  = null; 
	private EventCounter ecRspSent  = null; 
	private EventCounter ecReqErr   = null; 
	

	public HttpRequestHandler(ClickMessageHandler parent, Logger log, String name, int history_size) {
		super();
		this.counter = EventCounter.get(HttpRequestHandler.class.getSimpleName());
		this.parent = parent;
		this.log = log;
		
		this.ecReqRecv  = EventCounter.get(name + "_req_recv", history_size); 
		this.ecRspSent  = EventCounter.get(name + "_rsp_sent", history_size); 
		this.ecReqErr   = EventCounter.get(name + "_req_err",  history_size); 
	}

	/* 
	 */
    public void handle(HttpExchange e) {
    	StringBuilder buf = new StringBuilder(1024);
    	OutputStream os = null;
    	boolean log_debug = true;
		byte[] rsp = null;  
		        	
		try {
			this.counter.inc();
			int mps = counter.getPerSecAvg();

			//log.info("Inbound message: " + e.getRequestURI().toASCIIString());
			
			this.ecReqRecv.inc();
			
			// Log debug info
			if (mps > FreeLoader.MAX_LOG_EPS) {	// Don't flood the logs when it gets busy
				if (counter.isNewSecond()) {
					buf.setLength(0);
					log.info(buf.append("Recv HTTP request(s): Debug output paused - messages per second is ").append(mps).append(")").toString());
				}
				log_debug = false;
			} else {
				// Dump request
				buf.setLength(0);
				buf.append("Received HTTP request:\n ").append(e.getRequestMethod()).append(' ').append(e.getRequestURI().toASCIIString()).append('\n');
				buf.append("  Headers:\n");
				for (Entry<String, List<String>> header : e.getRequestHeaders().entrySet()) {
					buf.append("   ").append(header.getKey()).append('=').append(header.getValue()).append('\n');
				}
				log.info(buf.toString());
			}

			// TODO evaluate user password and api_id and respond appropriately if invalid
			ClickMessage msg = new ClickMessage(e.getRequestURI().toASCIIString());
			String rsp_data = null;
			try {
				rsp_data = this.parent.onClickMessage(msg);
			} catch (ClickApiException cae) {
				this.ecReqErr.inc();
				rsp = ("ERR: " + cae.getCode()).getBytes();		//TODO TODO TODO pull from a ClickAPIException 
			}			

			// Send id in response
			if (rsp == null) {
				rsp = ("ID: " + msg.getApiMsgId()).getBytes();  //TODO charset
			}
			
			if (log_debug) {
				// Dump response
				buf.setLength(0);
				buf.append("Sending HTTP response:\n ").append(" 200 - OK "  + new String(rsp) + " \n");
				buf.append("  Headers:\n");
				for (Entry<String, List<String>> header : e.getResponseHeaders().entrySet()) {
					buf.append("   ").append(header.getKey()).append('=').append(header.getValue()).append('\n');
				}
				//buf.append(" ID: ").append(uuid).append('\n');
				log.info(buf.toString());
			}

			// Ready the response headers
			Headers rh = e.getResponseHeaders();
			e.sendResponseHeaders(200, rsp.length);	
			os = e.getResponseBody();
			os.write(rsp);
			
			this.ecRspSent.inc();

		} catch (Exception e1) {
			this.ecReqErr.inc();
			Utils.logThrottledException(log, this.getClass(), "Exception processing HTTP request", e1, FreeLoader.MAX_LOG_EPS);

		} finally {
			try {
				if (os != null) {
					os.close();
				}
				os = null;
			} catch (Exception ge) {
				//nothing
			}
			//e.close();
		}
			
    }

	protected EventCounter getEcReqRecv() {
		return ecReqRecv;
	}

	protected EventCounter getEcRspSent() {
		return ecRspSent;
	}

	protected EventCounter getEcReqErr() {
		return ecReqErr;
	}
	
}



