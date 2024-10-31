package gui;

import javax.swing.*;

public class ChartDisplay implements Runnable {

    private Chart chart;

    @Override
    public void run() {
        this.chart = new Chart();
        chart.setAlwaysOnTop(true);
        chart.pack();
        chart.setSize(800, 600);
        chart.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        chart.setVisible(true);
    }

    public Chart getChart() {
        return chart;
    }
}
