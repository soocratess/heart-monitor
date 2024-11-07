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

    public Chart getChart() {
        return this.chart;
    }

    /**
     * Displays the ECG GUI.
     */
    @Override
    public void run() {
        this.chart.setAlwaysOnTop(true);
        this.chart.pack();
        this.chart.setSize(800, 600);
        this.chart.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.chart.setTimeLeft(this.chart.getTimeLeft() + 60);
        for (int i = 0; i < 60; i++) this.chart.addDataPoint(i - 60, -1);
        this.chart.setVisible(true);
    }
}
