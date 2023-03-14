package PassengerAirlinerFlightManagementSystem;

import PassengerAirlinerFlightManagementSystem.Actuators.Engine;
import PassengerAirlinerFlightManagementSystem.Actuators.LandingGear;
import PassengerAirlinerFlightManagementSystem.Actuators.OxygenMasks;
import PassengerAirlinerFlightManagementSystem.Actuators.WingFlaps;
import PassengerAirlinerFlightManagementSystem.Sensors.AltitudeSensor;
import PassengerAirlinerFlightManagementSystem.Sensors.CabinPressureSensor;
import PassengerAirlinerFlightManagementSystem.Sensors.SpeedSensor;
import PassengerAirlinerFlightManagementSystem.Sensors.WeatherEnvironmentSensor;
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

        //instantiate all classes
        FlightControl flightControl = new FlightControl(connection);
        CabinPressureSensor cabinPressureSensor = new CabinPressureSensor(connection);
        WeatherEnvironmentSensor weatherEnvironmentSensor = new WeatherEnvironmentSensor(connection);
        SpeedSensor speedSensor = new SpeedSensor(connection);
        AltitudeSensor altitudeSensor = new AltitudeSensor(connection);
        OxygenMasks oxygenMasks = new OxygenMasks(connection);
        WingFlaps wingFlaps = new WingFlaps(connection);
        LandingGear landingGear = new LandingGear(connection);
        Engine engine = new Engine(connection);


        ScheduledExecutorService timer = Executors.newScheduledThreadPool(5);
        ExecutorService ex = Executors.newFixedThreadPool(4);
        ex.submit(flightControl);
        ex.submit(wingFlaps);
        ex.submit(oxygenMasks);
        ex.submit(engine);
        timer.scheduleAtFixedRate(cabinPressureSensor, 0, 1000, TimeUnit.MILLISECONDS);
        timer.scheduleAtFixedRate(altitudeSensor, 0, 1000, TimeUnit.MILLISECONDS);
        timer.scheduleAtFixedRate(speedSensor, 0, 1000, TimeUnit.MILLISECONDS);
        timer.scheduleAtFixedRate(weatherEnvironmentSensor, 0, 1000, TimeUnit.MILLISECONDS);
        timer.scheduleAtFixedRate(landingGear, 0, 1000, TimeUnit.MILLISECONDS);

        long startingTime = System.nanoTime();
        System.out.println("Flight Control System started at " + startingTime);
        //set ladning mode to true when use provide any input
//        System.in.read();
        //set landing mode to true when 45 second has elapsed
        try {
//            Thread.sleep(45000);
            Thread.sleep(60000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
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

        long endingTime = System.nanoTime();
        float totalTime = (float) ((endingTime - startingTime)/ 1_000_000_000);
        //calculte the total time taken for landing in miliseconds
        float totalTimeInMiliseconds =  (float) (endingTime - startingTime)/ 1_000_000;
        System.out.println("Total time taken for landing: " + totalTimeInMiliseconds + " miliseconds");

        LatencyTester latencyTester = new LatencyTester();
        System.out.println("Average latency: " + latencyTester.calculateTime() + " miliseconds");
        latencyTester.display();


        System.exit(0);

    }
}
