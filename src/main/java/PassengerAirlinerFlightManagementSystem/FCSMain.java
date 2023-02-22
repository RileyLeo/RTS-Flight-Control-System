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

    //Declare the every flight control system, Actuators, and Sensors exchange name
    public static String flightControlExchangeName = "FC";
    public static String oxygenMaskExchangeName = "OM";
    public static String cabinPressureExchangeName = "CP";

    public static void main(String[] args) throws IOException, TimeoutException {

        ConnectionFactory factory = new ConnectionFactory();
        Connection connection = factory.newConnection();

        ScheduledExecutorService timer = Executors.newScheduledThreadPool(1);
        timer.scheduleAtFixedRate(new FlightControl(connection), 0, 1000, TimeUnit.MILLISECONDS);
        timer.scheduleAtFixedRate(new OxygenMasks(connection), 0, 1000, TimeUnit.MILLISECONDS);
        timer.scheduleAtFixedRate(new CabinPressureSensor(connection), 0, 1000, TimeUnit.MILLISECONDS);

    }
}
