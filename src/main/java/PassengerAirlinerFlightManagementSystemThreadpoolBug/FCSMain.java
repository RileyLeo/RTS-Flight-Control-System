package PassengerAirlinerFlightManagementSystemThreadpoolBug;

import PassengerAirlinerFlightManagementSystemThreadpoolBug.Actuators.Engine;
import PassengerAirlinerFlightManagementSystemThreadpoolBug.Actuators.LandingGear;
import PassengerAirlinerFlightManagementSystemThreadpoolBug.Actuators.OxygenMasks;
import PassengerAirlinerFlightManagementSystemThreadpoolBug.Actuators.WingFlaps;
import PassengerAirlinerFlightManagementSystemThreadpoolBug.Sensors.AltitudeSensor;
import PassengerAirlinerFlightManagementSystemThreadpoolBug.Sensors.CabinPressureSensor;
import PassengerAirlinerFlightManagementSystemThreadpoolBug.Sensors.SpeedSensor;
import PassengerAirlinerFlightManagementSystemThreadpoolBug.Sensors.WeatherEnvironmentSensor;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.concurrent.*;

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



    public static void main(String[] args) throws IOException, TimeoutException {

        //Create a connection to the RabbitMQ server
        ConnectionFactory factory = new ConnectionFactory();
        Connection connection = factory.newConnection();

        ScheduledExecutorService timer = Executors.newScheduledThreadPool(4);
        ExecutorService ex = Executors.newFixedThreadPool(5);
        ex.submit(new FlightControl(connection));
        ex.submit(new OxygenMasks(connection));
        ex.submit(new WingFlaps(connection));
        ex.submit(new Engine(connection));
        ex.submit(new LandingGear(connection));
//      timer.scheduleAtFixedRate(new FlightControl(connection), 0, 1000, TimeUnit.MILLISECONDS);
//      timer.scheduleAtFixedRate(new LandingGear(connection), 0, 1000, TimeUnit.MILLISECONDS);
//      timer.scheduleAtFixedRate(new OxygenMasks(connection), 0, 1000, TimeUnit.MILLISECONDS);
//      timer.scheduleAtFixedRate(new Engine(connection), 0, 1000, TimeUnit.MILLISECONDS);
//      timer.scheduleAtFixedRate(new WingFlaps(connection), 0, 1000, TimeUnit.MILLISECONDS);
        timer.scheduleAtFixedRate(new CabinPressureSensor(connection), 0, 1000, TimeUnit.MILLISECONDS);
        timer.scheduleAtFixedRate(new AltitudeSensor(connection), 0, 1000, TimeUnit.MILLISECONDS);
        timer.scheduleAtFixedRate(new SpeedSensor(connection), 0, 1000, TimeUnit.MILLISECONDS);
        timer.scheduleAtFixedRate(new WeatherEnvironmentSensor(connection), 0, 1000, TimeUnit.MILLISECONDS);

        //set ladning mode to true when use provide any input
        System.in.read();
        FlightControl.isLanding = true;

        while (!FlightControl.isLandingGearDeployed || !FlightControl.isAltitudeZero || !FlightControl.isSpeedZero) {
        }
        timer.shutdown();
        //sleep for 5 seconds to make sure all the messages are sent
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("Landing complete");
        System.exit(0);

    }
}
