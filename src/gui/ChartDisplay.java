package gui;

import javax.swing.*;

/**
 * This class is used to display the ECG GUI through a thread.
 */
public class ChartDisplay implements Runnable {

    private Chart chart;

    public ChartDisplay(Chart chart) {
        this.chart = chart;
    }

    @Override
    public void run() {
        this.chart.setAlwaysOnTop(true);
        this.chart.pack();
        this.chart.setSize(800, 600);
        this.chart.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.chart.setVisible(true);
    }

    public Chart getChart() {
        return this.chart;
    }
}