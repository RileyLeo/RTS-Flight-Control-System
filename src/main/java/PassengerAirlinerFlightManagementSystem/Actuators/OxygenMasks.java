package PassengerAirlinerFlightManagementSystem.Actuators;

import PassengerAirlinerFlightManagementSystem.FCSMain;
import PassengerAirlinerFlightManagementSystem.LatencyTester;
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
    AtomicBoolean isMaskDropped = new AtomicBoolean(false);
    public static AtomicBoolean isAfterTurbulence = new AtomicBoolean(false);
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
                AtomicBoolean alreadyAdjusted = new AtomicBoolean(false);
                System.out.println("\u001B[36m" + "Oxygen Masks received - " + message + "\u001B[0m");
                String[] parts = message.split("-");
                long sendTime;
                sendTime = Long.parseLong(parts[1]);
                if (parts[0].equals("Deploy Masks")) {
                    if (!isMaskDropped.get()) {
                        System.out.println("\u001B[36m" +"Oxygen Masks deployed" + "\u001B[0m");
                        String oxygenMaskMessage = "OM:Deployed";
                        outputChannel.basicPublish(FCSMain.flightControlExchangeName, "", null, oxygenMaskMessage.getBytes("UTF-8"));
                        System.out.println("\u001B[36m" + "Oxygen Masks sent: " + oxygenMaskMessage +"\u001B[0m");
                        isMaskDropped.set(true);
                    } else{
                        System.out.println("\u001B[36m" + "Oxygen Masks are already deployed" + "\u001B[0m");
                    }
                } else if (parts[0].equals("Retract Masks")) {
                    if (isMaskDropped.get()) {
                        System.out.println("\u001B[36m" + "Oxygen Masks have been retracted" + "\u001B[0m");
                        isMaskDropped.set(false);
                        String oxygenMaskMessage = "OM:Retracted";
                        outputChannel.basicPublish(FCSMain.flightControlExchangeName, "", null, oxygenMaskMessage.getBytes("UTF-8"));
                        System.out.println("\u001B[36m" + "Oxygen Masks sent: " + oxygenMaskMessage + "\u001B[0m");

                    } else{
                        System.out.println("\u001B[36m" + "Oxygen Masks are already retracted" + "\u001B[0m");
                    }
                    if (alreadyAdjusted.get() == false && isAfterTurbulence.get() == false){
                        long currentTime = System.nanoTime();
                        System.out.println("\u001B[32m" + "Wing Flaps current time: " + currentTime + "\u001B[0m");
                        long totalTime = currentTime - sendTime;
                        System.out.println("\u001B[32m" + "Wing Flaps Latency: " + totalTime + "\u001B[0m");
                        LatencyTester.timeList.add((double) (totalTime)/1000000);
                    }
                    isAfterTurbulence.set(false);
                }
            }, consumerTag -> {});
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

