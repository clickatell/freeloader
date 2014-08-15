

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.JSplitPane;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;



/**
 */
public class StatsChart extends ApplicationFrame  {
	private JSplitPane split1 = null; 
    private JSplitPane split2 = null;
	private FreeLoader freeloader; 

	public ChartPanel getMTChart(){
        CategoryDataset dataset = createMTDataset();
        JFreeChart chart = createChart("MT", dataset, 6);
        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(1200, 200));
		
        return chartPanel;
	}
	
	public ChartPanel getMTCBChart(){
        CategoryDataset dataset = createMTCBDataset();
        JFreeChart chart = createChart("MT_Callback", dataset, 6);
        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(1200, 200));
		
        return chartPanel;
	}
	
	public ChartPanel getMOChart(){
        CategoryDataset dataset = createMODataset();
        JFreeChart chart = createChart("MO", dataset, 6);
        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(1200, 200));
		
        return chartPanel;
	}
	
	
    /**
     * Creates a new demo.
     *
     * @param title  the frame title.
     */
    public StatsChart(FreeLoader parent, final String title) {
        super(title);

        this.freeloader = parent;
        
        update();
    }

    public void update() {
    	
    	EventCounter.update();
    	
        split1 = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        split1.setTopComponent(getMTChart());
        split1.setBottomComponent(getMTCBChart());
        split1.setDividerLocation(200);
        
        split2 = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        split2.setTopComponent(split1);
        split2.setBottomComponent(getMOChart());
        split2.setDividerLocation(400);
        
        setContentPane(split2);
    }
    
    private void addPoints(DefaultCategoryDataset dataset, EventCounter.StatPoint[] arr, String name) {
        SimpleDateFormat sdf = new SimpleDateFormat("ss");
        
        for (int i = 0; i < arr.length; i++) {
        	EventCounter.StatPoint sp = arr[arr.length - 1 - i];
	        dataset.addValue(sp.value, name, sdf.format(new Date(sp.timestampSeconds * 1000)));
		}
    }
    
    private CategoryDataset createMTDataset() {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        this.addPoints(dataset, this.freeloader.getMtHandler().getEcRspSent().getStats(), "rsp_sent");
        this.addPoints(dataset, this.freeloader.getMtHandler().getEcReqRecv().getStats(), "req_recv");
        this.addPoints(dataset, this.freeloader.getMtHandler().getEcReqErr().getStats(),  "req_err");
        
        return dataset;
    }
    
    private CategoryDataset createMTCBDataset() {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        this.addPoints(dataset, this.freeloader.getMtCallbackProcessor().getEcReqSent().getStats(),  "req sent");
        this.addPoints(dataset, this.freeloader.getMtCallbackProcessor().getEcRspRecv().getStats(),  "rsp recv");
        this.addPoints(dataset, this.freeloader.getMtCallbackProcessor().getEcRspErr().getStats(),   "rsp err");
        this.addPoints(dataset, this.freeloader.getMtCallbackProcessor().getEcDequeued().getStats(), "queue:req in");
        this.addPoints(dataset, this.freeloader.getMtCallbackProcessor().getEcEnqueued().getStats(), "queue:req out");
        
        return dataset;
    }
    
    private CategoryDataset createMODataset() {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        this.addPoints(dataset, this.freeloader.getMoProcessor().getEcReqSent().getStats(),  "req sent");
        this.addPoints(dataset, this.freeloader.getMoProcessor().getEcRspRecv().getStats(),  "rsp recv");
        this.addPoints(dataset, this.freeloader.getMoProcessor().getEcRspErr().getStats(),   "rsp error");
        this.addPoints(dataset, this.freeloader.getMoProcessor().getEcDequeued().getStats(), "queue:req in");
        this.addPoints(dataset, this.freeloader.getMoProcessor().getEcEnqueued().getStats(), "queue:req out");
        
        return dataset;
    }
    
    
    /**
     * Creates a sample chart.
     * 
     * @param dataset  a dataset.
     * 
     * @return The chart.
     */
    private JFreeChart createChart(String title, CategoryDataset dataset, int ssize) {
        
        // create the chart...
        final JFreeChart chart = ChartFactory.createLineChart(
            title,				       // chart title
            "time",                    // domain axis label
            "msgs/sec",                // range axis label
            dataset,                   // data
            PlotOrientation.VERTICAL,  // orientation
            true,                      // include legend
            true,                      // tooltips
            false                      // urls
        );

        chart.setBackgroundPaint(Color.white);

        CategoryPlot plot = (CategoryPlot) chart.getPlot();
        plot.setBackgroundPaint(Color.black);
        plot.setRangeGridlinePaint(Color.white);

        // customise the range axis...
        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        rangeAxis.setAutoRangeIncludesZero(true);

        // customise the renderer...
        LineAndShapeRenderer renderer = (LineAndShapeRenderer) plot.getRenderer();

        for (int i = 0; i < ssize; i++) {
            renderer.setSeriesStroke(i, new BasicStroke(3.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		}
        
        return chart;
    }

    
    /**
     * Starting point for the demonstration application.
     *
     * @param args  ignored.
     */
    public static void main(final String[] args) {

        StatsChart demo = new StatsChart(null, "FreeLoader");

        for (int i = 0; i < 100; i++) {
            try {
    			Thread.sleep(100);
    		} catch (InterruptedException e) {
    			e.printStackTrace();
    		}
            demo.update();
		}
    }

}