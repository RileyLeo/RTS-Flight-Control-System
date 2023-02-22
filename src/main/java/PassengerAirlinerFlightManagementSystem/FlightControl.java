package PassengerAirlinerFlightManagementSystem;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

class FlightControl implements Runnable {
    Channel inputChannel;
    Channel outputChannel;
    String queueName;
    boolean isMasksDeployed = false;
        public FlightControl(Connection connection) throws IOException {
        inputChannel = connection.createChannel();
        inputChannel.exchangeDeclare(FCSMain.flightControlExchangeName, "fanout");
        queueName = inputChannel.queueDeclare().getQueue();
        inputChannel.queueBind(queueName, FCSMain.flightControlExchangeName, "");

        outputChannel = connection.createChannel();
    }
    @Override
    public void run() {

        try {

//            System.out.println("Flight Control is listening for messages");
            inputChannel.basicConsume(queueName, true, (consumerTag, delivery) -> {
                String message = new String(delivery.getBody(), "UTF-8");
                System.out.println("Flight Control received - " + message);
                if (message.contains("CP:")) {
                    String[] parts = message.split(":");
                    int cabinPressure = Integer.parseInt(parts[1]);
                    if (cabinPressure < 80 && !isMasksDeployed) {
                        System.out.println("Cabin pressure is low, deploying masks");
                        // deploy oxygen mask
                        // send a message to oxygen mask to come down
                        outputChannel.exchangeDeclare(FCSMain.oxygenMaskExchangeName, "fanout");
                        String oxygenMaskMessage = "Deploy Masks";
                        outputChannel.basicPublish(FCSMain.oxygenMaskExchangeName, "", null, oxygenMaskMessage.getBytes("UTF-8"));
                        System.out.println("Flight Control sent: " + oxygenMaskMessage);
                    } else if (cabinPressure > 90 && isMasksDeployed) {
                        System.out.println("Cabin pressure is high, retracting masks");
                        // retract oxygen mask
                        // send a message to oxygen mask to retract
                        outputChannel.exchangeDeclare(FCSMain.oxygenMaskExchangeName, "fanout");
                        String oxygenMaskMessage = "Retract Masks";
                        outputChannel.basicPublish(FCSMain.oxygenMaskExchangeName, "", null, oxygenMaskMessage.getBytes("UTF-8"));
                        System.out.println("Flight Control sent: " + oxygenMaskMessage);
                    }
                } else if (message.contains("OM:")){
                    // oxygen mask status
                    String[] parts = message.split(":");
                    String oxygenMaskStatus = parts[1];
                    if (oxygenMaskStatus.equals("Deployed")) {
                        isMasksDeployed = true;
                        System.out.println("Oxygen masks deployed successfully");
                    } else if (oxygenMaskStatus.equals("Retracted")) {
                        isMasksDeployed = false;
                        System.out.println("Oxygen masks retracted successfully");
                    }
                }

            }, consumerTag -> {});
        } catch (Exception e) {
            e.printStackTrace();
        }


    }
}
