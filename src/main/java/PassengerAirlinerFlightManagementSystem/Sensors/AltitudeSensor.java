package PassengerAirlinerFlightManagementSystem.Sensors;

import PassengerAirlinerFlightManagementSystem.FCSMain;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AltitudeSensor implements Runnable {

    Channel inputChannel;
    Channel outputChannel;
    String queueName;
    ScheduledExecutorService timer;
    public AltitudeSensor(Connection connection) throws IOException {
        timer = Executors.newScheduledThreadPool(1);
        inputChannel = connection.createChannel();
        inputChannel.exchangeDeclare(FCSMain.altitudeExchangeName, "fanout");
        queueName = inputChannel.queueDeclare().getQueue();
        inputChannel.queueBind(queueName, FCSMain.altitudeExchangeName, "");

        outputChannel = connection.createChannel();
        outputChannel.exchangeDeclare(FCSMain.flightControlExchangeName, "fanout");
    }
    Random rand = new Random();
    Integer CurrentAltitude = 34500;
    Integer flapModifier = 0;
    boolean landingMode = false;
//    boolean isAltitudeZero = false;
    boolean isMessageAcknowledged = false;

    @Override
    public void run() {

        timer.scheduleAtFixedRate(new AltitudeSensorInput(this), 0, 1000, TimeUnit.MILLISECONDS);
        // adjust the altitude randomly
        int change = rand.nextInt(5);
        if (landingMode == false) {
            if (rand.nextBoolean()) {
                change *= -1;
            }
        } else {
            change = 0;
        }

        //final equation
        //change max/min= +- 2500
        // modifier max/min = +-1000
        CurrentAltitude = CurrentAltitude + (change * 500) + (flapModifier * 1500);
        if (CurrentAltitude < 0) {
            CurrentAltitude = 0;
        }

        // send the altitude to the flight control system
        try {
            if (!isMessageAcknowledged) {
                String message = FCSMain.altitudeExchangeName + ":" + CurrentAltitude.toString();
                outputChannel.basicPublish(FCSMain.flightControlExchangeName, "", null, message.getBytes("UTF-8"));
                System.out.println("\u001B[33m" + "Altitude Sensor:"+ "\u001B[0m" + CurrentAltitude.toString() + " ft altitude sent to flight control");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}

class AltitudeSensorInput implements Runnable {

    AltitudeSensor altitudeSensor;

    public AltitudeSensorInput(AltitudeSensor altitudeSensor) {
        this.altitudeSensor = altitudeSensor;
    }

    @Override
    public void run() {
        // retrieve updated wing flaps status from the flight control system
        try {
            altitudeSensor.inputChannel.basicConsume(altitudeSensor.queueName, true, (consumerTag, delivery) -> {
                String message = new String(delivery.getBody(), "UTF-8");
                System.out.println("\u001B[33m" + "Altitude Sensor:" + "\u001B[0m" + " received - " + message);
                if (message.equals("Wing flaps adjusted lower")) {
                    System.out.println("\u001B[33m" + "Altitude Sensor:"+ "\u001B[0m" + "Wing flaps adjusted lower successfully");
                    altitudeSensor.flapModifier = -1;
                } else if (message.equals("Wing flaps adjusted higher")) {
                    System.out.println("\u001B[33m" + "Altitude Sensor:"+ "\u001B[0m" + "Wing flaps adjusted higher successfully");
                    altitudeSensor.flapModifier = 1;
                } else if (message.equals("Wing flaps adjusted neutral position")) {
                    System.out.println("\u001B[33m" + "Altitude Sensor:"+ "\u001B[0m" + "Wing flaps adjusted neutral position successfully");
                    altitudeSensor.flapModifier = 0;
                } else if (message.equals("Landing mode activate, Wing flaps adjusted lower")) {
                    System.out.println("\u001B[33m" + "Altitude Sensor:"+ "\u001B[0m" + "Landing mode activate, Wing flaps adjusted lower successfully");
                    altitudeSensor.landingMode = true;
                    altitudeSensor.flapModifier = -2;
                }else if (message.equals("Message Acknowledged")) {
                    altitudeSensor.isMessageAcknowledged = true;
                    System.out.println("\u001B[33m" + "Altitude Sensor:"+ "\u001B[0m" + "Message acknowledged, sending stopped");
                }
            }, consumerTag -> {
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

