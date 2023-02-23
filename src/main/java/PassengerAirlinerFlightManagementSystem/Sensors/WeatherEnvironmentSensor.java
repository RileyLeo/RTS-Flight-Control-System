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
    }

    @Override
    public void run() {
        //send the speed to the flight control system
        try {
            outputChannel.exchangeDeclare(FCSMain.flightControlExchangeName, "fanout");


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
