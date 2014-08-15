
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Layout;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;


public class HTTPClientProcessor {
	private static Logger log = Logger.getLogger(HTTPClientProcessor.class);
	private static EventCounter counter = EventCounter.get(HTTPClientProcessor.class.getSimpleName());

	private LinkedList<ClickMessage> queue;
	private LinkedList<HttpClientRunnable> threadPool;
	private boolean shutdown = false; 
	
	private EventCounter ecEnqueued = null; 
	private EventCounter ecDequeued = null; 
	private EventCounter ecReqSent  = null; 
	private EventCounter ecRspRecv  = null; 
	private EventCounter ecRspErr   = null;
	private String name = ""; 

	/**
	 * */
	public static class HttpClientRunnable implements Runnable {
		private static boolean shutdown = false;
		private String url;
		private int timeout;
		private EventCounter counter = null;
		private HTTPClientProcessor parent = null;
		private Thread parentThread = null; 

		/**
		 * Constructor
		 */
		public HttpClientRunnable(HTTPClientProcessor parent, EventCounter counter, String url, int timeout) {
			this.timeout = timeout;
			this.url = url;
			this.counter = counter;
			this.parent = parent;
		}


		/**
		 * */
		public void sendRequest(ClickMessage msg) {
			StringBuilder buf = new StringBuilder(2048);
			boolean log_rsp = true;
			HttpURLConnection connection = null;
			InputStream response = null;
			String url_str = null;

			try {
				this.counter.inc();
				int mps = counter.getPerSecAvg();

				url_str = msg.toUrl(buf, url);
				
				connection = (HttpURLConnection) new URL(url_str).openConnection();	
				connection.setRequestProperty("Accept-Charset", "UTF-8");
				connection.setReadTimeout(this.timeout);		

				// Debug log request sent
				// Log debug info
				if (mps > FreeLoader.MAX_LOG_EPS) {	// Don't flood the logs when it gets busy
					if (counter.isNewSecond()) {
						buf.setLength(0);
						log.info(buf.append(this.parent.name).append(": Sending request - messages per second is ").append(mps).append(")").toString());
					}
					log_rsp = false;
				} else {
					// Dump request
					buf.setLength(0);
					buf.append(this.parent.name).append(": Sending HTTP request:\n ").append("GET").append(' ').append(url_str).append('\n');
					buf.append("  Headers:\n");
					for (Entry<String, List<String>> header : connection.getHeaderFields().entrySet()) {
						buf.append("   ").append(header.getKey()).append('=').append(header.getValue()).append('\n');
					}
					log.info(buf.toString());
				}

				// send request and wait for response
				msg.setEventStatus(EventType.SENT);
				this.parent.ecReqSent.inc();
				try {
					response = connection.getInputStream();
					this.parent.ecRspRecv.inc();
				} catch (IOException e1) {
					this.parent.ecRspErr.inc();
					msg.setEventStatus(EventType.TIMEOUT);	//TODO needs to be something else
					throw e1;
				}

				// prepare the response log entry TODO remove all of this?
				if (log_rsp) {
					buf.setLength(0);
					buf.append(this.parent.name).append(": Recv HTTP response:\n ").append(" ").append(connection.getResponseCode()).append(" - ").append(connection.getResponseMessage()).append('\n');
					buf.append("  Headers:\n");

					// get the content type and from it the charset
					String contentType = connection.getHeaderField("Content-Type");
					if (contentType != null) {
						String charset = null;
						for (String param : contentType.replace(" ", "").split(";")) {
							if (param.startsWith("charset=")) {
								charset = param.split("=", 2)[1];
								break;
							}
						}

						if (charset != null) {
							BufferedReader reader = null;
							try {
								reader = new BufferedReader(new InputStreamReader(response, charset));
								for (String line; (line = reader.readLine()) != null;) {
									if (log_rsp) {
										buf.append("   ").append(line).append('\n');
									}
								}

								if (log_rsp) {
									log.info(buf.toString());
								}
							} finally {
								if (reader != null) try { reader.close(); } catch (IOException e) {}
							}
						} else {
							// binary content...
						}
					}
				}

				try {
					response.close();
				} finally {
					response = null;
					connection = null;
				}		
			} catch (UnsupportedEncodingException e) {
				this.parent.ecRspErr.inc();
				Utils.logThrottledException(log, this.getClass(), "Exception processing HTTP request", e, FreeLoader.MAX_LOG_EPS);
			} catch (MalformedURLException e) {
				this.parent.ecRspErr.inc();
				Utils.logThrottledException(log, this.getClass(), "Exception processing HTTP request", e, FreeLoader.MAX_LOG_EPS);
			} catch (IOException e) {
				this.parent.ecRspErr.inc();
				Utils.logThrottledException(log, this.getClass(), "Exception processing HTTP request", e, FreeLoader.MAX_LOG_EPS);
			} finally {
				//nothing
			}

		}

		
		/**
		 * Shuts down the component
		 */
		public void shutdown() {
			log.info("Shutting down http client thread...");

			this.parentThread.interrupt();
			this.shutdown = true;

			log.info("the end...");
		}



		@Override
		public void run() {
			while (! shutdown) {
				ClickMessage msg = null; 
				
				msg = this.parent.getNextMessage();
				if (msg == null || this.shutdown) {
					return;
				}
				
				try {
					this.sendRequest(msg);
				} catch (Exception e) {
					Utils.logThrottledException(log, this.getClass(), "Exception sending http request", e, FreeLoader.MAX_LOG_EPS);
				}
			}
		}


		public void setParentThread(Thread t) {
			this.parentThread = t;
		}
	}

	
	public HTTPClientProcessor(String name, int history_size, String base_url, int timeout, int thread_pool_size) {
		this.ecEnqueued = EventCounter.get(name + "_enqueue",  history_size); 
		this.ecDequeued = EventCounter.get(name + "_dequeue",  history_size); 
		this.ecReqSent  = EventCounter.get(name + "_req_sent", history_size); 
		this.ecRspRecv  = EventCounter.get(name + "_rsp_recv", history_size); 
		this.ecRspErr   = EventCounter.get(name + "_rsp_err",  history_size); 
		this.name  = name;
		
		this.queue = new LinkedList<ClickMessage>();
		this.threadPool = new LinkedList<HttpClientRunnable>();
		for (int j = 0; j < thread_pool_size; j++) {
			HttpClientRunnable ct = new HttpClientRunnable(this, this.counter, base_url, timeout);
			this.threadPool.add(ct);
			Thread t = new Thread(ct);
			ct.setParentThread(t);
			t.start();
		}
		
	}

	
	public void shutdown(){
		this.shutdown = true;
		
		for (HttpClientRunnable t : this.threadPool) {
			t.shutdown();
		}
		synchronized (this.queue) {
			this.queue.clear();
			this.queue.notifyAll();
		} 
		
		//delay a bit
		try {
			Thread.sleep(250);
		} catch (InterruptedException e) {
			//nothing
		}
	}
	
	/**
	 * 
	 * @return
	 * @throws InterruptedException 
	 */
	public ClickMessage getNextMessage(){
		ClickMessage msg = null;
				 
		synchronized (this.queue) {
			while ( true ) {
				if (! this.queue.isEmpty()) {
					this.ecDequeued.inc();
					return this.queue.removeFirst();
				}
				
				try {
					this.queue.wait();
				} catch (InterruptedException e) {
					//log.debug("interrupted 1");
				}
				
				if (this.shutdown) {
					return null;
				}
			}
		}
	}

	
	/**
	 * 
	 * @param msg
	 */
	public void addToQueue(ClickMessage msg) {
		synchronized (this.queue) {
			this.ecEnqueued.inc();
			msg.setEventStatus(EventType.QUEUED);
			this.queue.addLast(msg);
			this.queue.notifyAll();
		}
	}


	/**
	 * 
	 * @param msg
	 */
	public int getQueueSize() {
		synchronized (this.queue) {
			return this.queue.size();
		}
	}
	
	/**
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		ArrayList<Thread> threads = new ArrayList<Thread>();

		try {
			int timeout = 10000;
			int total_num_tests = 1;
			int total_threads = 10;

			HTTPClientProcessor p = new HTTPClientProcessor("mt", 60 * 60 * 24, "http://127.0.0.1:8001/http/mt_callback", timeout, total_threads);

			// load up the queue
			for (int i = 0; i < total_num_tests; i++) {
				ClickMessage msg = new ClickMessage(Utils.getRandomMSISDN(), null, Utils.genMsgId(), Utils.genMsgId(), "4", "");
				p.addToQueue(msg);
			}
			
			// Wait for the queue to clear
			while (p.getQueueSize() > 0) {
				Thread.sleep(500);
				
				log.info(p.name + " queue size: " + p.getQueueSize());
			}
			
			// shutdown
			p.shutdown();
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


	}
	
	/* static inits*/
	static {
		try {
			Layout l = new PatternLayout("%d %p %m%n");
			log.addAppender(new ConsoleAppender(l));
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}

	protected EventCounter getEcEnqueued() {
		return ecEnqueued;
	}


	protected EventCounter getEcDequeued() {
		return ecDequeued;
	}


	protected EventCounter getEcReqSent() {
		return ecReqSent;
	}


	protected EventCounter getEcRspRecv() {
		return ecRspRecv;
	}


	protected EventCounter getEcRspErr() {
		return ecRspErr;
	}

}