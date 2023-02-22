package PassengerAirlinerFlightManagementSystem.Actuators;

import PassengerAirlinerFlightManagementSystem.FCSMain;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;

import java.io.IOException;

public class LandingGear implements Runnable{

    Channel inputChannel;
    Channel outputChannel;
    String queueName;
    boolean isMaskDropped = false;
    public LandingGear(Connection connection) throws IOException {
        inputChannel = connection.createChannel();
        inputChannel.exchangeDeclare(FCSMain.landingGearExchangeName, "fanout");
        queueName = inputChannel.queueDeclare().getQueue();
        inputChannel.queueBind(queueName, FCSMain.landingGearExchangeName, "");

        outputChannel = connection.createChannel();
    }

    @Override
    public void run() {
        // TODO Auto-generated method stub

    }
}
