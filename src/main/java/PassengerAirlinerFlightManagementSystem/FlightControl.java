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

    public FlightControl(Connection connection) throws IOException {
        inputChannel = connection.createChannel();
        inputChannel.exchangeDeclare(FCSMain.flightControlExchangeName, "fanout");
        queueName = inputChannel.queueDeclare().getQueue();
        inputChannel.queueBind(queueName, FCSMain.flightControlExchangeName, "");

        outputChannel = connection.createChannel();
    }

    //Dependencies variables
    boolean isMasksDeployed = false;
    boolean isGearDeployed = false;
    boolean isLanding = false;
    int upperboundAltitudeDuringFlight = 38000;
    int middleboundAltitudeDuringFlight = 34500;
    int lowerboundAltitudeDuringFlight = 31000;
    int middleboundThreshold = 2000; //34500 +- 2000 = (32500 to 36500)
    String flapPosition = "neutral";


    @Override
    public void run() {

        try {

//            System.out.println("Flight Control is listening for messages");
            inputChannel.basicConsume(queueName, true, (consumerTag, delivery) -> {
                String message = new String(delivery.getBody(), "UTF-8");
                System.out.println("Flight Control received - " + message);
                if (message.contains("CP:")) {
                    //Cabin Pressure
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
                } else if (message.contains("OM:")) {
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
                } else if (message.contains("AL")) {
                    // altitude
                    String[] parts = message.split(":");
                    int altitude = Integer.parseInt(parts[1]);
                    if(altitude > upperboundAltitudeDuringFlight && !isLanding && !flapPosition.equals("low")){
                        System.out.println("Altitude is high, adjusting wing flaps to a lower angle");
                        // adjust wing flaps to a lower angle
                        // send a message to wing flaps to adjust
                        outputChannel.exchangeDeclare(FCSMain.wingFlapsExchangeName, "fanout");
                        String wingFlapsMessage = "Adjust:Lower";
                        outputChannel.basicPublish(FCSMain.wingFlapsExchangeName, "", null, wingFlapsMessage.getBytes("UTF-8"));
                    } else if(altitude < lowerboundAltitudeDuringFlight && !isLanding && !flapPosition.equals("high")){
                        System.out.println("Altitude is low, adjusting wing flaps to a higher angle");
                        // adjust wing flaps to a higher angle
                        // send a message to wing flaps to adjust
                        outputChannel.exchangeDeclare(FCSMain.wingFlapsExchangeName, "fanout");
                        String wingFlapsMessage = "Adjust:Higher";
                        outputChannel.basicPublish(FCSMain.wingFlapsExchangeName, "", null, wingFlapsMessage.getBytes("UTF-8"));
                    } else if (altitude >= middleboundAltitudeDuringFlight - middleboundThreshold && altitude <= middleboundAltitudeDuringFlight + middleboundThreshold && !isLanding && !flapPosition.equals("neutral")){
                        System.out.println("Altitude is in the middle, adjusting wing flaps to a middle angle");
                        // adjust wing flaps to a normal angle
                        // send a message to wing flaps to adjust
                        outputChannel.exchangeDeclare(FCSMain.wingFlapsExchangeName, "fanout");
                        String wingFlapsMessage = "Adjust:Normal";
                        outputChannel.basicPublish(FCSMain.wingFlapsExchangeName, "", null, wingFlapsMessage.getBytes("UTF-8"));
                    }
                } else if (message.contains("WF:")) {
                    // wing flaps status
                    String[] parts = message.split(":");
                    String wingFlapsStatus = parts[1];
                    if (wingFlapsStatus.equals("Wing flaps adjusted lower")) {
                        System.out.println("Wing flaps adjusted lower successfully");
                        flapPosition = "low";
                    } else if (wingFlapsStatus.equals("Wing flaps adjusted higher")) {
                        System.out.println("Wing flaps adjusted higher successfully");
                        flapPosition = "high";
                    } else if (wingFlapsStatus.equals("Wing flaps adjusted neutral position")) {
                        System.out.println("Wing flaps adjusted to normal successfully");
                        flapPosition = "neutral";
                    }
                    //Inform Altitude sensor of the adjustment
                    outputChannel.exchangeDeclare(FCSMain.altitudeExchangeName, "fanout");
                    String altitudeControlMessage = wingFlapsStatus;
                    outputChannel.basicPublish(FCSMain.altitudeExchangeName, "", null, altitudeControlMessage.getBytes("UTF-8"));
                }
            }, consumerTag -> {
            });
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    public void landingSequence() {


    }


}
