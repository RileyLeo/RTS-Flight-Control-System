package PassengerAirlinerFlightManagementSystem.Sensors;

import PassengerAirlinerFlightManagementSystem.FCSMain;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;

import java.io.IOException;

public class WeatherEnvironmentSensor implements Runnable{

    Channel inputChannel;
    Channel outputChannel;
    String queueName;
    public WeatherEnvironmentSensor(Connection connection) throws IOException {
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
    boolean hasResolvedTurbulence = false;
    @Override
    public void run() {
        //send the speed to the flight control system
        try {
            // weather check
            if (currentWeather != "Thunderstorm" || hasResolvedTurbulence == true) {
                if (firstRun || currentWeather != reportedWeather) {
                    String message = FCSMain.weatherEnvironmentExchangeName + ":" + currentWeather;
                    outputChannel.basicPublish(FCSMain.flightControlExchangeName, "", null, message.getBytes("UTF-8"));
                    System.out.println("\u001B[38;5;226m" + "Weather Environment Sensor:"+ "\u001B[0m" + currentWeather + " weather sent to flight control");
                    reportedWeather = currentWeather;
                    firstRun = false;
                    System.out.println("\u001B[38;5;226m" + "Weather Environment Sensor:"+ "\u001B[0m" + "Standby for weather change");
                }
                //1/10 chance of weather change to rain or clear (alternating)
                if (Math.random() < 0.1) {
                    if (currentWeather == "Clear") {
                        currentWeather = "Rain";
                    } else {
                        currentWeather = "Clear";
                    }
                }
                //if it rains, 1/10 chance of thunderstorm
                if (currentWeather == "Rain") {
                    if (Math.random() < 0.1) {
                        currentWeather = "Thunderstorm";
                        hasResolvedTurbulence = false;
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

            }, consumerTag -> {
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
