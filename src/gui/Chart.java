// Clase independiente en archivo Chart.java
package gui;

import javax.swing.JFrame;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.category.DefaultCategoryDataset;

import java.util.LinkedList;
import java.util.Queue;

public class Chart extends JFrame {
    private static final long serialVersionUID = 1L;
    private DefaultCategoryDataset dataset;
    private Queue<String> timeStamps;
    private static final int MAX_POINTS = 60;

    public Chart() {
        super("ECG Monitor");
        this.dataset = new DefaultCategoryDataset();
        this.timeStamps = new LinkedList<>();

        JFreeChart chart = ChartFactory.createLineChart(
                "Electrocardiogram Monitor", "time (s)", "bpm", dataset
        );

        ChartPanel panel = new ChartPanel(chart);
        setContentPane(panel);
    }

    public void addDataPoint(double bpm) {
        String timeLabel = timeStamps.size() + "s";
        dataset.addValue(bpm, "Heart Rate", timeLabel);
        timeStamps.add(timeLabel);

        if (timeStamps.size() > MAX_POINTS) {
            String oldestTime = timeStamps.poll();
            dataset.removeValue("Heart Rate", oldestTime);
        }
    }
}
