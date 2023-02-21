package PassengerAirlinerFlightManagementSystem.Sensors;

import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;

import java.util.Random;

public class CabinPressureSensor implements Runnable {

    Random rand = new Random();
    public Integer CabinPressurePercentage = 100;
//    public static boolean isCabinPressureLow = false;

    @Override
    public void run() {
        // adjust the cabin pressure
        int percentageDrop = rand.nextInt(5);
        CabinPressurePercentage -= percentageDrop;

        try {
//            String cabinExchangeName = "CabinPressure";
            String flightControlExchangeName = "FC";
            ConnectionFactory factory = new ConnectionFactory();
            Connection connection = factory.newConnection();
            Channel channel = connection.createChannel();

            channel.exchangeDeclare(flightControlExchangeName, "fanout");

            String message = "CP:" + CabinPressurePercentage.toString();

            channel.basicPublish(flightControlExchangeName, "", null, message.getBytes("UTF-8"));
            System.out.println("\u001B[31m" + "Cabin Pressure Sensor:"+ "\u001B[0m" + CabinPressurePercentage.toString() + " % cabin pressure sent to flight control");

            channel.close();
            connection.close();
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

}
