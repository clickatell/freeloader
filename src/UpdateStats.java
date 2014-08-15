import java.util.TimerTask;

import org.jfree.ui.RefineryUtilities;


public class UpdateStats extends TimerTask {
	private StatsChart chart = null;
	private FreeLoader parent = null;

	public UpdateStats(FreeLoader parent) {
		super();

		this.parent = parent;
		this.chart = new StatsChart(parent, "FreeLoader");
        this.chart.pack();
        RefineryUtilities.centerFrameOnScreen(this.chart);
        this.chart.setVisible(true);
	}


	public void run() {
		try {
			
			this.chart.update();
	        this.chart.pack();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}
