package PassengerAirlinerFlightManagementSystem.Sensors;

import PassengerAirlinerFlightManagementSystem.Actuators.Engine;
import PassengerAirlinerFlightManagementSystem.FCSMain;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SpeedSensor implements Runnable{

    Channel inputChannel;
    Channel outputChannel;
    String queueName;
    ScheduledExecutorService timer;
    public SpeedSensor(Connection connection) throws IOException {
        timer = Executors.newScheduledThreadPool(1);
        inputChannel = connection.createChannel();
        inputChannel.exchangeDeclare(FCSMain.speedExchangeName, "fanout");
        queueName = inputChannel.queueDeclare().getQueue();
        inputChannel.queueBind(queueName, FCSMain.speedExchangeName, "");

        outputChannel = connection.createChannel();
        outputChannel.exchangeDeclare(FCSMain.flightControlExchangeName, "fanout");
    }
    Random rand = new Random();
    //base speed in km/h
    Integer currentCruiseSpeed = 900;
    double engineModifier = 0;
    boolean landingMode = false;
//    boolean isSpeedZero = false;
    boolean isMessageAcknowledged = false;

    @Override
    public void run() {
        timer.scheduleAtFixedRate(new SpeedSensorInput(this), 0, 1000, TimeUnit.MILLISECONDS);
        //adjust the speed randomly
        int change = rand.nextInt(10);
        if (landingMode == false) {
            if (rand.nextBoolean()) {
                change *= -1;
            }
        } else {
            change = 0;
        }

        //final equation
        //change max/min= +- 100
        // modifier max/min = +-50
        currentCruiseSpeed = (int) (currentCruiseSpeed + (change * 8) + (engineModifier * 45));
        if (currentCruiseSpeed < 0) {
            currentCruiseSpeed = 0;
        }

        //send the speed to the flight control system
        try {
            if (isMessageAcknowledged == false) {
                String message = FCSMain.speedExchangeName + ":" + currentCruiseSpeed.toString();
                outputChannel.basicPublish(FCSMain.flightControlExchangeName, "", null, message.getBytes("UTF-8"));
                System.out.println("\u001B[38;5;226m" + "Speed Sensor:"+ "\u001B[0m" + currentCruiseSpeed.toString() + " km/h speed sent to flight control");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

class SpeedSensorInput implements Runnable {
    SpeedSensor speedSensor;
    public SpeedSensorInput(SpeedSensor speedSensor) {
        this.speedSensor = speedSensor;
    }

    @Override
    public void run() {

        try {
            speedSensor.inputChannel.basicConsume(speedSensor.queueName, true, (consumerTag, delivery) -> {
                String message = new String(delivery.getBody(), "UTF-8");
                if (message.equals("Engine decelerated")) {
                    System.out.println("\u001B[38;5;226m" + "Speed Sensor:"+ "\u001B[0m" + "Engine decelerated");
                    speedSensor.engineModifier = -1;
                } else if (message.equals("Engine accelerated")) {
                    System.out.println("\u001B[38;5;226m" + "Speed Sensor:"+ "\u001B[0m" + "Engine accelerated");
                    speedSensor.engineModifier = 1;
                } else if (message.equals("Engine moderated")) {
                    System.out.println("\u001B[38;5;226m" + "Speed Sensor:"+ "\u001B[0m" + "Engine moderated");
                    speedSensor.engineModifier = 0;
                }else if (message.equals("Landing mode activate, Engine decelerated")) {
                    System.out.println("\u001B[38;5;226m" + "Speed Sensor:"+ "\u001B[0m" + "Landing mode activated, Engine decelerated");
                    speedSensor.landingMode = true;
                    speedSensor.engineModifier = -1.5;
                }else if (message.equals("Message Acknowledged")) {
                    speedSensor.isMessageAcknowledged = true;
//                    speedSensor.isSpeedZero = true;
                    System.out.println("\u001B[38;5;226m" + "Speed Sensor:"+ "\u001B[0m" + "Message acknowledged, sending stopped");
                }
            }, consumerTag -> {
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
