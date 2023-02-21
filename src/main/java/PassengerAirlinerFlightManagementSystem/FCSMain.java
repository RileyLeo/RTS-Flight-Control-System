package PassengerAirlinerFlightManagementSystem;

import PassengerAirlinerFlightManagementSystem.Actuators.OxygenMasks;
import PassengerAirlinerFlightManagementSystem.Sensors.CabinPressureSensor;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class FCSMain {
    public static void main(String[] args) throws IOException, TimeoutException {

//        String flightControlExchangeName = "FC";
        ConnectionFactory factory = new ConnectionFactory();
        Connection connection = factory.newConnection();
//        Channel channel = connection.createChannel();
//
//        channel.exchangeDeclare(flightControlExchangeName, "fanout");
//        String queueName = channel.queueDeclare().getQueue();
//        channel.queueBind(queueName, flightControlExchangeName, "");

        ScheduledExecutorService timer = Executors.newScheduledThreadPool(1);
        timer.scheduleAtFixedRate(new FlightControl(connection), 0, 1000, TimeUnit.MILLISECONDS);
        timer.scheduleAtFixedRate(new OxygenMasks(), 0, 1000, TimeUnit.MILLISECONDS);
        timer.scheduleAtFixedRate(new CabinPressureSensor(), 0, 1000, TimeUnit.MILLISECONDS);

    }
}
