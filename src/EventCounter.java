

import java.util.HashMap;
import java.util.LinkedList;

/**
 * Maintains 'events per second' counter that can be used to keep track of stats
 */
public class EventCounter {

	public static class StatPoint {
		long timestampSeconds = 0;
		long value = 0;
		long avg = 0;
		/**
		 * @param timestampSeconds
		 * @param value
		 * @param avg
		 */
		public StatPoint(long timestampSeconds, long value, long avg) {
			super();
			this.timestampSeconds = timestampSeconds;
			this.value = value;
			this.avg = avg;
		}
	}

	private static HashMap<String, EventCounter> counterMap = new HashMap<String, EventCounter>();
	private LinkedList<StatPoint> stats = null; 

	private boolean newSecond;
	private long total; 
	private int perSecTotal; 
	private int perSecAvg; 
	private long lastSec;
	private int historySize = 0; 

	/**
	 * */
	public static EventCounter get(String name){
		synchronized (counterMap) {
			EventCounter c = counterMap.get(name);
			if (c == null) {
				c = new EventCounter(name);
				counterMap.put(name, c);
			}
			return c;
		}
	}

	public static EventCounter get(String name, int history_size) {
		EventCounter ec = get(name);
		ec.historySize = history_size;
		ec.stats = new LinkedList<EventCounter.StatPoint>();
		return ec;
	}
	
	/**
	 * Private default constructor
	 */
	private EventCounter() {
	}
	
	public StatPoint[] getStats() {
		StatPoint[] arr = new StatPoint[this.stats.size()];
		return this.stats.toArray(arr);
	}

	/**
	 * Constructor 
	 * @param name Name of the counter
	 */
	private EventCounter(String name) {
		this.lastSec = System.currentTimeMillis() / 1000;
	}

	private void addStat(long sec, long total, long avg) {
		this.stats.addFirst(new StatPoint(sec, total, avg));		//TODO averages is incorrect
		if (this.stats.size() > this.historySize) {
			this.stats.removeLast();
		}
	}
	
	/**
	 * Increment the counters
	 */
	public synchronized void inc() {
		this.total++;
		this.perSecTotal++;

		this.update(System.currentTimeMillis() / 1000);
	}

	/**
	 * Update the "timestamp" of current data only
	 */
	private synchronized void update(long cs) {
		if (cs > this.lastSec) {
			long ls = this.lastSec;
			//build the stats
			if (this.stats != null) {
				this.addStat(ls, this.perSecAvg, this.perSecAvg);
				while (ls < cs-1) {
					ls++;
					this.addStat(ls, 0, 0);
				}
			}
			
			if (cs == this.lastSec + 1) {
				this.perSecAvg = perSecTotal;
			} else {
				this.perSecAvg = 0;		//nothing happened in the previous second
			}
			this.newSecond = true;
			this.lastSec = cs;
			this.perSecTotal = 0;
		}
	}

	/**
	 * updates all 
	 */
	public static void update() {
		long cs = System.currentTimeMillis() / 1000;
		for (EventCounter ec : counterMap.values()) {
			ec.update(cs);
		}
	}
	
	/**
	 * Returns the 'average per second'
	 * @return
	 */
	public int getPerSecAvg() {
		return Math.max(this.perSecTotal, this.perSecAvg);
	}

	/**
	 * Returns the total
	 * @return
	 */
	public long getTotal() {
		return total;
	}

	/**
	 * @return the lastSec
	 */
	public long getLastSec() {
		return lastSec;
	}

	/**
	 * Returns if a new second was entered since the last time this method was called.
	 * @return
	 */
	public synchronized boolean isNewSecond() {
		boolean ns = this.newSecond;
		this.newSecond = false;
		return ns;
	}
}
