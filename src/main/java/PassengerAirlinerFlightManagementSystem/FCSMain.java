package PassengerAirlinerFlightManagementSystem;

import PassengerAirlinerFlightManagementSystem.Actuators.Engine;
import PassengerAirlinerFlightManagementSystem.Actuators.OxygenMasks;
import PassengerAirlinerFlightManagementSystem.Actuators.WingFlaps;
import PassengerAirlinerFlightManagementSystem.Sensors.AltitudeSensor;
import PassengerAirlinerFlightManagementSystem.Sensors.CabinPressureSensor;
import PassengerAirlinerFlightManagementSystem.Sensors.SpeedSensor;
import PassengerAirlinerFlightManagementSystem.Sensors.WeatherEnvironmentSensor;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class FCSMain {

    //Declare the every flight control system, Actuators, and Sensors exchange name
    //Flight Control system
    public static String flightControlExchangeName = "FC";

    //Sensory systems
    public static String cabinPressureExchangeName = "CP";
    public static String weatherEnvironmentExchangeName = "WE";
    public static String speedExchangeName = "SP";
    public static String altitudeExchangeName = "AL";


    //Actuators systems
    public static String oxygenMaskExchangeName = "OM";
    public static String wingFlapsExchangeName = "WF";
    public static String landingGearExchangeName = "LG";
    public static String engineExchangeName = "EN";
//    public static String


    public static void main(String[] args) throws IOException, TimeoutException {
        //Create a connection to the RabbitMQ server
        ConnectionFactory factory = new ConnectionFactory();
        Connection connection = factory.newConnection();

        ScheduledExecutorService timer = Executors.newScheduledThreadPool(8);
        timer.scheduleAtFixedRate(new FlightControl(connection), 0, 1000, TimeUnit.MILLISECONDS);
        timer.scheduleAtFixedRate(new OxygenMasks(connection), 0, 1000, TimeUnit.MILLISECONDS);
        timer.scheduleAtFixedRate(new CabinPressureSensor(connection), 0, 1000, TimeUnit.MILLISECONDS);
        timer.scheduleAtFixedRate(new WingFlaps(connection), 0, 1000, TimeUnit.MILLISECONDS);
        timer.scheduleAtFixedRate(new AltitudeSensor(connection), 0, 1000, TimeUnit.MILLISECONDS);
        timer.scheduleAtFixedRate(new SpeedSensor(connection), 0, 1000, TimeUnit.MILLISECONDS);
        timer.scheduleAtFixedRate(new Engine(connection), 0, 1000, TimeUnit.MILLISECONDS);
        timer.scheduleAtFixedRate(new WeatherEnvironmentSensor(connection), 0, 1000, TimeUnit.MILLISECONDS);

    }
}
