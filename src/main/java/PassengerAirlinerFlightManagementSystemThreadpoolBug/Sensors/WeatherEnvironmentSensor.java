package PassengerAirlinerFlightManagementSystemThreadpoolBug.Sensors;

import PassengerAirlinerFlightManagementSystem.FCSMain;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class WeatherEnvironmentSensor implements Runnable{

    Channel inputChannel;
    Channel outputChannel;
    String queueName;
    ScheduledExecutorService timer;
    public WeatherEnvironmentSensor(Connection connection) throws IOException {
        ExecutorService ex = Executors.newFixedThreadPool(1);
        ex.submit(new WeatherEnvironmentSensorInput(this));
        timer = Executors.newScheduledThreadPool(1);
        inputChannel = connection.createChannel();
        inputChannel.exchangeDeclare(FCSMain.weatherEnvironmentExchangeName, "fanout");
        queueName = inputChannel.queueDeclare().getQueue();
        inputChannel.queueBind(queueName, FCSMain.weatherEnvironmentExchangeName, "");

        outputChannel = connection.createChannel();
        outputChannel.exchangeDeclare(FCSMain.flightControlExchangeName, "fanout");

    }
    boolean firstRun = true;
    String currentWeather = "Clear";
    String reportedWeather = "Clear";
    boolean hasResolvedTurbulence;
    boolean isLanding = false;
    @Override
    public void run() {
        //randomly resolve turbulence when thunderstorm (testing purposes)
//        if(currentWeather.equals("Thunderstorm") && !hasResolvedTurbulence){
//            if(Math.random() < 0.1){
//                hasResolvedTurbulence = true;
//            }
//        }
//        timer.scheduleAtFixedRate(new WeatherEnvironmentSensorInput(this), 0, 1000, TimeUnit.MILLISECONDS);

//        System.out.println("\u001B[38;2;255;165;0m" + "Weather: " + currentWeather + "\u001B[0m");
        //send the speed to the flight control system
        try {
            // weather check
            if (currentWeather != "Thunderstorm" || hasResolvedTurbulence == true) {
                //if it rains, 1/10 chance of thunderstorm
                if (currentWeather == "Rain") {
                    if (Math.random() < 0.35) {
                        currentWeather = "Thunderstorm";
//                        System.out.println("\u001B[38;2;255;165;0m" + "Weather is now a " + currentWeather + "\u001B[0m");
                        hasResolvedTurbulence = false;
                    }
                }else if (currentWeather == "Thunderstorm" && hasResolvedTurbulence == true){
                    currentWeather = "Clear";
                }

                //weather report
                if ((firstRun || currentWeather != reportedWeather) && !isLanding) {
                    String message = FCSMain.weatherEnvironmentExchangeName + ":" + currentWeather;
                    outputChannel.basicPublish(FCSMain.flightControlExchangeName, "", null, message.getBytes("UTF-8"));
                    System.out.println("\u001B[38;2;255;165;0m" + "Weather Environment Sensor:"+ "\u001B[0m" + currentWeather + " weather sent to flight control");
                    reportedWeather = currentWeather;
                    firstRun = false;
                    System.out.println("\u001B[38;2;255;165;0m" + "Weather Environment Sensor:"+ "\u001B[0m" + "Standby for weather change");
                }
                //1/10 chance of weather change to rain or clear (alternating)
                if (Math.random() < 0.1) {
                    if (currentWeather == "Clear") {
                        currentWeather = "Rain";
                    } else {
                        currentWeather = "Clear";
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}

class WeatherEnvironmentSensorInput implements Runnable {

    WeatherEnvironmentSensor weatherEnvironmentSensor;

    public WeatherEnvironmentSensorInput(WeatherEnvironmentSensor weatherEnvironmentSensor) {
        this.weatherEnvironmentSensor = weatherEnvironmentSensor;
    }
    @Override
    public void run() {
        // retrieve updated wing flaps status from the flight control system
        try {
            weatherEnvironmentSensor.inputChannel.basicConsume(weatherEnvironmentSensor.queueName, true, (consumerTag, delivery) -> {
                String message = new String(delivery.getBody(), "UTF-8");
                if (message.equals("Turbulence resolved")) {
                    weatherEnvironmentSensor.hasResolvedTurbulence = true;
                    System.out.println("\u001B[38;2;255;165;0m" + "Weather Environment Sensor:"+ "\u001B[0m" + "Turbulence resolved");
                } else if (message.equals("Landing")){
                    weatherEnvironmentSensor.currentWeather = "Clear";
                    weatherEnvironmentSensor.hasResolvedTurbulence = true;
                    System.out.println("\u001B[38;2;255;165;0m" + "Weather Environment Sensor:"+ "\u001B[0m" + "Landing, weather is now clear, stopping message sending");
                    weatherEnvironmentSensor.isLanding = true;
                }
            }, consumerTag -> {
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
