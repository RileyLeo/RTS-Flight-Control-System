package PassengerAirlinerFlightManagementSystem.Sensors;

import PassengerAirlinerFlightManagementSystem.FCSMain;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.TimeoutException;

public class AltitudeSensor implements Runnable {

    Channel inputChannel;
    Channel outputChannel;
    String queueName;
    public AltitudeSensor(Connection connection) throws IOException {
        inputChannel = connection.createChannel();
        inputChannel.exchangeDeclare(FCSMain.altitudeExchangeName, "fanout");
        queueName = inputChannel.queueDeclare().getQueue();
        inputChannel.queueBind(queueName, FCSMain.altitudeExchangeName, "");

        outputChannel = connection.createChannel();
    }
    Random rand = new Random();
    Integer CurrentAltitude = 34500;
    String flapPosition;

    @Override
    public void run() {

        // adjust the altitude
        int change = rand.nextInt(5);
        if (rand.nextBoolean()) {
            change *= -1;
        }
        //modifier code here


        //final equation
        CurrentAltitude += change*500;


        // send the altitude to the flight control system
        try {
            outputChannel.exchangeDeclare(FCSMain.flightControlExchangeName, "fanout");
            String message = FCSMain.altitudeExchangeName + ":" + CurrentAltitude.toString();

            outputChannel.basicPublish(FCSMain.flightControlExchangeName, "", null, message.getBytes("UTF-8"));
            System.out.println("\u001B[33m" + "Altitude Sensor:"+ "\u001B[0m" + CurrentAltitude.toString() + " ft altitude sent to flight control");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }




    }

}

class AltitudeSensorInput implements Runnable {

    Channel inputChannel;
    String queueName;
    AltitudeSensor altitudeSensor;

    public AltitudeSensorInput(Channel inputChannel, String queueName, AltitudeSensor altitudeSensor) {
        this.inputChannel = inputChannel;
        this.queueName = queueName;
        this.altitudeSensor = altitudeSensor;
    }

    @Override
    public void run() {
//        altitudeSensor.flapPosition = "neutral";

        // retrieve updated wing flaps status from the flight control system
        try {
            inputChannel.basicConsume(queueName, true, (consumerTag, delivery) -> {
                String message = new String(delivery.getBody(), "UTF-8");
                System.out.println("\u001B[33m" + "Altitude Sensor:"+ "\u001B[0m" + " received - " + message);
//                if (message.contains("Wing Flaps:")) {
//                    //Wing Flaps
//                    String[] parts = message.split(":");
//                    String wingFlaps = parts[1];
//                    if (wingFlaps.equals("up")) {
//                        System.out.println("\u001B[33m" + "Altitude Sensor:"+ "\u001B[0m" + " received - " + message);
//                        // deploy oxygen mask
//                        // send a message to oxygen mask to come down
//                        outputChannel.exchangeDeclare(FCSMain.oxygenMaskExchangeName, "fanout");
//                        String oxygenMaskMessage = "Deploy Masks";
//                        outputChannel.basicPublish(FCSMain.oxygenMaskExchangeName, "", null, oxygenMaskMessage.getBytes("UTF-8"));
//                        System.out.println("\u001B[33m" + "Altitude Sensor:"+ "\u001B[0m" + " sent: " + oxygenMaskMessage);
//                    } else if (wingFlaps.equals("down")) {
//                        System.out.println("\u001B[33m" + "Altitude Sensor:"+ "\u001B[0m" + " received - " + message);
//                        // retract oxygen mask
//                        // send a message to oxygen mask to retract
//                        outputChannel.exchangeDeclare(FCSMain.oxygenMaskExchangeName, "fanout");
//                        String oxygenMaskMessage = "Retract Masks";
//                        outputChannel.basicPublish(FCSMain.oxygenMaskExchangeName, "", null, oxygenMaskMessage.getBytes("UTF-8"));
//                        System.out.println("\u001B[33m" + "Altitude Sensor:"+ "\u001B[0m" + " sent: " + oxygenMaskMessage);
//                    }
//                }
            }, consumerTag -> {
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
