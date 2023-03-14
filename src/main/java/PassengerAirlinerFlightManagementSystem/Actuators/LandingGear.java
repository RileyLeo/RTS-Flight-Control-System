package PassengerAirlinerFlightManagementSystem.Actuators;

import PassengerAirlinerFlightManagementSystem.FCSMain;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class LandingGear implements Runnable {

    Channel inputChannel;
    Channel outputChannel;
    String queueName;
    ScheduledExecutorService timer;
    public LandingGear(Connection connection) throws IOException {
//        timer = Executors.newScheduledThreadPool(1);
//        timer.scheduleAtFixedRate(new LandingGearInput(this), 0, 1000, TimeUnit.MILLISECONDS);
        inputChannel = connection.createChannel();
        inputChannel.exchangeDeclare(FCSMain.landingGearExchangeName, "fanout");
        queueName = inputChannel.queueDeclare().getQueue();
        inputChannel.queueBind(queueName, FCSMain.landingGearExchangeName, "");

        outputChannel = connection.createChannel();
        outputChannel.exchangeDeclare(FCSMain.flightControlExchangeName, "fanout");
    }

    boolean isLandingGearDeployed = false;
    boolean isMessageAcknowledged = false;
    boolean firstRun = true;

    @Override
    public void run() {
        if (firstRun){
            LandingGearInput landingGearInput = new LandingGearInput(this);
            Thread thread = new Thread(landingGearInput);
            thread.start();
        }
        if (isLandingGearDeployed && !isMessageAcknowledged){
            try {
                String message = FCSMain.landingGearExchangeName + ":Landing Gear Deployed";
                outputChannel.basicPublish(FCSMain.flightControlExchangeName, "", null, message.getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

class LandingGearInput implements Runnable {
    LandingGear landingGear;

    public LandingGearInput(LandingGear landingGear) {
        this.landingGear = landingGear;
    }

    @Override
    public void run() {
        try {
            landingGear.inputChannel.basicConsume(landingGear.queueName, true, (consumerTag, delivery) -> {
                String message = new String(delivery.getBody(), "UTF-8");
                System.out.println("\u001B[38;2;255;192;203m" + "LandingGear received - " + message + "\u001B[0m");
                if (message.equals("Deploy Landing Gear")) {
                    System.out.println("\u001B[38;2;255;192;203m" + "LandingGear deployed" + "\u001B[0m");
                    String landingGearMessage = "LG:Landing Gear deployed";
                    landingGear.isLandingGearDeployed = true;
                    landingGear.outputChannel.basicPublish(FCSMain.flightControlExchangeName, "", null, landingGearMessage.getBytes("UTF-8"));
                    System.out.println("\u001B[38;2;255;192;203m" + "Landing Gear sent: " + landingGearMessage + " to flight control" + "\u001B[0m");
                } else if (message.equals("Message Acknowledged")) {
                    landingGear.isMessageAcknowledged = true;
                    System.out.println("\u001B[38;2;255;192;203m" + "LandingGear message acknowledged, Stop sending messages" + "\u001B[0m");
                }
            }, consumerTag -> {
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}


