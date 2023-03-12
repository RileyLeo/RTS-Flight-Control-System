package PassengerAirlinerFlightManagementSystemThreadpoolBug.Actuators;

import PassengerAirlinerFlightManagementSystem.FCSMain;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

public class OxygenMasks implements Runnable {

    Channel inputChannel;
    Channel outputChannel;
    String queueName;
    boolean isMaskDropped = false;
    public OxygenMasks(Connection connection) throws IOException {
        inputChannel = connection.createChannel();
        inputChannel.exchangeDeclare(FCSMain.oxygenMaskExchangeName, "fanout");
        queueName = inputChannel.queueDeclare().getQueue();
        inputChannel.queueBind(queueName, FCSMain.oxygenMaskExchangeName, "");

        outputChannel = connection.createChannel();
        outputChannel.exchangeDeclare(FCSMain.flightControlExchangeName, "fanout");
    }

    @Override
    public void run() {
        try {
//            System.out.println("\u001B[36m" + "Oxygen Masks:" + "\u001B[0m" + " Oxygen Masks is waiting for flight control message");
            inputChannel.basicConsume(queueName, true,  (consumerTag, delivery) -> {
                String message = new String(delivery.getBody(), "UTF-8");
                System.out.println("\u001B[36m" + "Oxygen Masks received - " + message + "\u001B[0m");
                if (message.equals("Deploy Masks")) {
                    if (!isMaskDropped) {
                        System.out.println("\u001B[36m" +"Oxygen Masks deployed" + "\u001B[0m");
                        String oxygenMaskMessage = "OM:Deployed";
                        outputChannel.basicPublish(FCSMain.flightControlExchangeName, "", null, oxygenMaskMessage.getBytes("UTF-8"));
                        System.out.println("\u001B[36m" + "Oxygen Masks sent: " + oxygenMaskMessage +"\u001B[0m");
                        isMaskDropped = true;
                    } else{
                        System.out.println("\u001B[36m" + "Oxygen Masks are already deployed" + "\u001B[0m");
                    }
                } else if (message.equals("Retract Masks")) {
                    if (isMaskDropped) {
                        System.out.println("\u001B[36m" + "Oxygen Masks have been retracted" + "\u001B[0m");
                        isMaskDropped = false;
                        String oxygenMaskMessage = "OM:Retracted";
                        outputChannel.basicPublish(FCSMain.flightControlExchangeName, "", null, oxygenMaskMessage.getBytes("UTF-8"));
                        System.out.println("\u001B[36m" + "Oxygen Masks sent: " + oxygenMaskMessage + "\u001B[0m");

                    } else{
                        System.out.println("\u001B[36m" + "Oxygen Masks are already retracted" + "\u001B[0m");
                    }
                }
            }, consumerTag -> {});
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

