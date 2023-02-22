package PassengerAirlinerFlightManagementSystem.Sensors;

import PassengerAirlinerFlightManagementSystem.FCSMain;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;

import java.io.IOException;

public class SpeedSensor implements Runnable{

    Channel inputChannel;
    Channel outputChannel;
    String queueName;
    boolean isMaskDropped = false;
    public SpeedSensor(Connection connection) throws IOException {
        inputChannel = connection.createChannel();
        inputChannel.exchangeDeclare(FCSMain.speedExchangeName, "fanout");
        queueName = inputChannel.queueDeclare().getQueue();
        inputChannel.queueBind(queueName, FCSMain.speedExchangeName, "");

        outputChannel = connection.createChannel();
    }

    @Override
    public void run() {
        // TODO Auto-generated method stub

    }

}
