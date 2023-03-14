package PassengerAirlinerFlightManagementSystem;

import java.util.ArrayList;

import tech.tablesaw.plotly.Plot;
import tech.tablesaw.plotly.api.LinePlot;
import tech.tablesaw.plotly.components.Figure;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class LatencyTester {
    //create a static arraylist to store time
    public static ArrayList<Double> timeList = new ArrayList<Double>();

    public float calculateTime(){
        //calculate the average time
        float averageTime = 0;
        for (int i = 0; i < timeList.size(); i++) {
            averageTime += timeList.get(i);
        }
        averageTime = averageTime / timeList.size();
        return averageTime;
    }


    public void plot() {
        AtomicInteger index = new AtomicInteger(1);

        Figure lineChart = LinePlot.create("Latency" + " against Iterations",
                "Iterations", timeList.stream().mapToDouble(aDouble -> index.getAndIncrement()).toArray(),
                "Latency in milliseconds", timeList.stream().mapToDouble(aDouble -> aDouble).toArray());

        Plot.show(lineChart);
    }

    public void display() {
        plot();
    }

}