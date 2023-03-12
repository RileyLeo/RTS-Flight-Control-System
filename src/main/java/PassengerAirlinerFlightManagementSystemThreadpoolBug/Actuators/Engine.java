package PassengerAirlinerFlightManagementSystemThreadpoolBug.Actuators;

import PassengerAirlinerFlightManagementSystemThreadpoolBug.FCSMain;
import PassengerAirlinerFlightManagementSystemThreadpoolBug.Sensors.AltitudeSensor;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;

import java.io.IOException;

public class Engine implements Runnable{
    Channel inputChannel;
    Channel outputChannel;
    String queueName;
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
                System.out.println("\u001B[35m" + "Engine received - " + message + "\u001B[0m");
                if (message.equals("Decelerate")) {
                    if (!currentEngineState.equals("Decelerate")) {
                        System.out.println("\u001B[35m" + "Engine decelerated" + "\u001B[0m");
                        String engineMessage = "EN:Engine decelerated";
                        currentEngineState = "Decelerate";
                        outputChannel.basicPublish(FCSMain.flightControlExchangeName, "", null, engineMessage.getBytes("UTF-8"));
                        System.out.println("\u001B[35m" + "Engine sent: " + engineMessage + "\u001B[0m");
                    } else {
                        System.out.println("\u001B[35m" + "Engine already in decelerate mode" + "\u001B[0m");
                    }
                } else if (message.equals("Accelerate")) {
                    if (!currentEngineState.equals("Accelerate")) {
                        System.out.println("\u001B[35m" + "Engine accelerated" + "\u001B[0m");
                        String engineMessage = "EN:Engine accelerated";
                        currentEngineState = "Accelerate";
                        outputChannel.basicPublish(FCSMain.flightControlExchangeName, "", null, engineMessage.getBytes("UTF-8"));
                        System.out.println("\u001B[35m" + "Engine sent: " + engineMessage + "\u001B[0m");
                    } else {
                        System.out.println("\u001B[35m" + "Engine already in accelerate mode" + "\u001B[0m");
                    }
                } else if (message.equals("Moderate")) {
                    if (!currentEngineState.equals("Moderate")) {
                        System.out.println("\u001B[35m" + "Engine moderated" + "\u001B[0m");
                        String engineMessage = "EN:Engine moderated";
                        currentEngineState = "Maintain";
                        outputChannel.basicPublish(FCSMain.flightControlExchangeName, "", null, engineMessage.getBytes("UTF-8"));
                        System.out.println("\u001B[35m" + "Engine sent: " + engineMessage + "\u001B[0m");
                    } else {
                        System.out.println("\u001B[35m" + "Engine already in maintain mode" + "\u001B[0m");
                    }
                }
            }, consumerTag -> {});
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}

