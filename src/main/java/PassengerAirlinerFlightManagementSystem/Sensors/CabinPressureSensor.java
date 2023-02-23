package PassengerAirlinerFlightManagementSystem.Sensors;

import PassengerAirlinerFlightManagementSystem.FCSMain;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;

import java.io.IOException;
import java.util.Random;

public class CabinPressureSensor implements Runnable {

    Channel inputChannel;
    Channel outputChannel;
    String queueName;
    Random rand = new Random();
    Boolean isDropping = true;

    public CabinPressureSensor(Connection connection) throws IOException {
        inputChannel = connection.createChannel();
        inputChannel.exchangeDeclare(FCSMain.cabinPressureExchangeName, "fanout");
        queueName = inputChannel.queueDeclare().getQueue();
        inputChannel.queueBind(queueName, FCSMain.cabinPressureExchangeName, "");

        outputChannel = connection.createChannel();
        outputChannel.exchangeDeclare(FCSMain.flightControlExchangeName, "fanout");
    }


    public Integer CabinPressurePercentage = 100;

    @Override
    public void run() {
        // adjust the cabin pressure
        int percentageDrop = rand.nextInt(5);

        if (isDropping) {
            CabinPressurePercentage -= percentageDrop;
        } else if (CabinPressurePercentage < 100) {
            CabinPressurePercentage += percentageDrop;
        }

        //Initialize cabin pressure restoration if cabin pressure is below 40%
        if (CabinPressurePercentage <= 40) {
            System.out.println("\u001B[31m" + "Cabin Pressure Sensor:"+ "\u001B[0m" + "Cabin pressure system is fixed, restoring cabin pressure");
            isDropping = false;
        }

        try {
            if (CabinPressurePercentage < 100){
                String message = FCSMain.cabinPressureExchangeName  + ":" + CabinPressurePercentage.toString();

                outputChannel.basicPublish(FCSMain.flightControlExchangeName, "", null, message.getBytes("UTF-8"));
                System.out.println("\u001B[31m" + "Cabin Pressure Sensor:"+ "\u001B[0m" + CabinPressurePercentage.toString() + " % cabin pressure sent to flight control");

            }
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

}
