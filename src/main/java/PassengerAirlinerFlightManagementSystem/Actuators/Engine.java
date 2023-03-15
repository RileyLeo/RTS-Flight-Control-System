package PassengerAirlinerFlightManagementSystem.Actuators;

import PassengerAirlinerFlightManagementSystem.FCSMain;
import PassengerAirlinerFlightManagementSystem.LatencyTester;
import PassengerAirlinerFlightManagementSystem.Sensors.AltitudeSensor;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

public class Engine implements Runnable{
    Channel inputChannel;
    Channel outputChannel;
    String queueName;
    public static AtomicBoolean isAfterTurbulence = new AtomicBoolean(false);
    public Engine(Connection connection) throws IOException {
        inputChannel = connection.createChannel();
        inputChannel.exchangeDeclare(FCSMain.engineExchangeName, "fanout");
        queueName = inputChannel.queueDeclare().getQueue();
        inputChannel.queueBind(queueName, FCSMain.engineExchangeName, "");

        outputChannel = connection.createChannel();
        outputChannel.exchangeDeclare(FCSMain.flightControlExchangeName, "fanout");
    }
    String currentEngineState = "Moderate";

    @Override
    public void run() {
        try {
            inputChannel.basicConsume(queueName, true,  (consumerTag, delivery) -> {
                String message = new String(delivery.getBody(), "UTF-8");
                AtomicBoolean alreadyAdjusted = new AtomicBoolean(false);
                System.out.println("\u001B[35m" + "Engine received - " + message + "\u001B[0m");
                String[] parts = message.split("-");
                long sendTime;
                sendTime = Long.parseLong(parts[1]);
                if (parts[0].equals("Decelerate")) {
                    if (!currentEngineState.equals("Decelerate")) {
                        System.out.println("\u001B[35m" + "Engine decelerated" + "\u001B[0m");
                        String engineMessage = "EN:Engine decelerated";
                        currentEngineState = "Decelerate";
                        outputChannel.basicPublish(FCSMain.flightControlExchangeName, "", null, engineMessage.getBytes("UTF-8"));
                        System.out.println("\u001B[35m" + "Engine sent: " + engineMessage + "\u001B[0m");
                    } else {
                        System.out.println("\u001B[35m" + "Engine already in decelerate mode" + "\u001B[0m");
                        alreadyAdjusted.set(true);
                    }
                } else if (parts[0].equals("Accelerate")) {
                    if (!currentEngineState.equals("Accelerate")) {
                        System.out.println("\u001B[35m" + "Engine accelerated" + "\u001B[0m");
                        String engineMessage = "EN:Engine accelerated";
                        currentEngineState = "Accelerate";
                        outputChannel.basicPublish(FCSMain.flightControlExchangeName, "", null, engineMessage.getBytes("UTF-8"));
                        System.out.println("\u001B[35m" + "Engine sent: " + engineMessage + "\u001B[0m");
                    } else {
                        System.out.println("\u001B[35m" + "Engine already in accelerate mode" + "\u001B[0m");
                        alreadyAdjusted.set(true);
                    }
                } else if (parts[0].equals("Moderate")) {
                    if (!currentEngineState.equals("Moderate")) {
                        System.out.println("\u001B[35m" + "Engine moderated" + "\u001B[0m");
                        String engineMessage = "EN:Engine moderated";
                        currentEngineState = "Maintain";
                        outputChannel.basicPublish(FCSMain.flightControlExchangeName, "", null, engineMessage.getBytes("UTF-8"));
                        System.out.println("\u001B[35m" + "Engine sent: " + engineMessage + "\u001B[0m");
                    } else {
                        System.out.println("\u001B[35m" + "Engine already in maintain mode" + "\u001B[0m");
                        alreadyAdjusted.set(true);
                    }
                }
                if (alreadyAdjusted.get() == false && isAfterTurbulence.get() == false){
                    long currentTime = System.nanoTime();
                    System.out.println("\u001B[32m" + "Wing Flaps current time: " + currentTime + "\u001B[0m");
                    long totalTime = currentTime - sendTime;
                    System.out.println("\u001B[32m" + "Wing Flaps Latency: " + totalTime + "\u001B[0m");
                    LatencyTester.timeList.add((double) (totalTime)/1000000);
                }
                isAfterTurbulence.set(false);
            }, consumerTag -> {});
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}

