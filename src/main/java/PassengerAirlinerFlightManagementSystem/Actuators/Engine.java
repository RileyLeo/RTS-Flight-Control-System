package PassengerAirlinerFlightManagementSystem.Actuators;

import PassengerAirlinerFlightManagementSystem.FCSMain;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;

import java.io.IOException;

public class Engine implements Runnable{
    Channel inputChannel;
    Channel outputChannel;
    String queueName;
    public Engine(Connection connection) throws IOException {
        inputChannel = connection.createChannel();
        inputChannel.exchangeDeclare(FCSMain.engineExchangeName, "fanout");
        queueName = inputChannel.queueDeclare().getQueue();
        inputChannel.queueBind(queueName, FCSMain.engineExchangeName, "");

        outputChannel = connection.createChannel();
    }

    @Override
    public void run() {
        // TODO Auto-generated method stub

    }
}
