package PassengerAirlinerFlightManagementSystem.Sensors;

import PassengerAirlinerFlightManagementSystem.FCSMain;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;

import java.io.IOException;

public class WeatherEnvironmentSensor implements Runnable{

    Channel inputChannel;
    Channel outputChannel;
    String queueName;
    boolean isMaskDropped = false;
    public WeatherEnvironmentSensor(Connection connection) throws IOException {
        inputChannel = connection.createChannel();
        inputChannel.exchangeDeclare(FCSMain.weatherEnvironmentExchangeName, "fanout");
        queueName = inputChannel.queueDeclare().getQueue();
        inputChannel.queueBind(queueName, FCSMain.weatherEnvironmentExchangeName, "");

        outputChannel = connection.createChannel();
    }

    @Override
    public void run() {
        // TODO Auto-generated method stub

    }

}
