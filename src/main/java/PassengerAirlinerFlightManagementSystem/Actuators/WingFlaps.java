package PassengerAirlinerFlightManagementSystem.Actuators;

import PassengerAirlinerFlightManagementSystem.FCSMain;
import PassengerAirlinerFlightManagementSystem.LatencyTester;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

public class WingFlaps implements Runnable{

    Channel inputChannel;
    Channel outputChannel;
    String queueName;
    String flapPosition = "neutral";
    public static AtomicBoolean isAfterTurbulence = new AtomicBoolean(false);

    public WingFlaps(Connection connection) throws IOException {
        inputChannel = connection.createChannel();
        inputChannel.exchangeDeclare(FCSMain.wingFlapsExchangeName, "fanout");
        queueName = inputChannel.queueDeclare().getQueue();
        inputChannel.queueBind(queueName, FCSMain.wingFlapsExchangeName, "");

        outputChannel = connection.createChannel();
        outputChannel.exchangeDeclare(FCSMain.flightControlExchangeName, "fanout");
    }

    @Override
    public void run() {
//        System.out.println("\u001B[32m" + "Wing Flaps:" + "\u001B[0m" + " Wing Flaps is waiting for flight control message");
        try {
            inputChannel.basicConsume(queueName, true,  (consumerTag, delivery) -> {
                String message = new String(delivery.getBody(), "UTF-8");
                AtomicBoolean alreadyAdjusted = new AtomicBoolean(false);
                System.out.println("\u001B[32m" + "Wing Flaps received - " + message + "\u001B[0m");
                String[] parts = message.split("-");
                long sendTime;
                sendTime = Long.parseLong(parts[1]);
//                System.out.println("\u001B[32m" + "Wing Flaps send time: " + sendTime + "\u001B[0m");
//                System.out.println("\u001B[32m" + "Wing Flaps current time: " + currentTime + "\u001B[0m");
                if (parts[0].equals("Adjust:Lower")) {
                    if(flapPosition != "low"){
                        System.out.println("\u001B[32m" + "Wing Flaps lowered" + "\u001B[0m");
                        String wingFlapsMessage = "WF:Wing flaps adjusted lower";
                        outputChannel.basicPublish(FCSMain.flightControlExchangeName, "", null, wingFlapsMessage.getBytes("UTF-8"));
                        flapPosition = "low";
                        System.out.println("\u001B[32m" + "Wing Flaps sent: " + wingFlapsMessage + "\u001B[0m");
                    }else{
                        System.out.println("\u001B[32m" + "Wing Flaps are already lowered" + "\u001B[0m");
                         alreadyAdjusted.set(true);
                    }
                } else if (parts[0].equals("Adjust:Higher")) {
                    if (flapPosition != "high") {
                        System.out.println("\u001B[32m" + "Wing Flaps raised" + "\u001B[0m");
                        String wingFlapsMessage = "WF:Wing flaps adjusted higher";
                        outputChannel.basicPublish(FCSMain.flightControlExchangeName, "", null, wingFlapsMessage.getBytes("UTF-8"));
                        flapPosition = "high";
                        System.out.println("\u001B[32m" + "Wing Flaps sent: " + wingFlapsMessage + "\u001B[0m");
                    }else{
                        System.out.println("\u001B[32m" + "Wing Flaps are already raised" + "\u001B[0m");
                        alreadyAdjusted.set(true);
                    }
                } else if (parts[0].equals("Adjust:Normal")) {
                    if (flapPosition != "neutral") {
                        System.out.println("\u001B[32m" + "Wing Flaps are at neutral position" + "\u001B[0m");
                        String wingFlapsMessage = "WF:Wing flaps adjusted neutral position";
                        outputChannel.basicPublish(FCSMain.flightControlExchangeName, "", null, wingFlapsMessage.getBytes("UTF-8"));
                        flapPosition = "neutral";
                        System.out.println("\u001B[32m" + "Wing Flaps sent: " + wingFlapsMessage + "\u001B[0m");
                    }else{
                        System.out.println("\u001B[32m" + "Wing Flaps are already at neutral position" + "\u001B[0m");
                        alreadyAdjusted.set(true);
                    }
                }
                if (alreadyAdjusted.get() == false && isAfterTurbulence.get() == false){
                    long currentTime = System.nanoTime();
                    System.out.println("\u001B[32m" + "Wing Flaps current time: " + currentTime + "\u001B[0m");
                    long totalTime = currentTime - sendTime;
                    System.out.println("\u001B[32m" + "Wing Flaps Latency: " + totalTime + "\u001B[0m");
                    LatencyTester.timeList.add((double) (totalTime)/ 1000000);
                }
                isAfterTurbulence.set(false);
            }, consumerTag -> {});
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}
