package PassengerAirlinerFlightManagementSystem;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

class FlightControl implements Runnable {
//    public static LinkedList<Integer> altitudeList = new LinkedList<>();
//    Connection connection;
    Channel inputChannel;
    Channel outputChannel;
    String queueName;
    String flightControlExchangeName = "FC";
    public FlightControl(Connection connection) throws IOException {
        inputChannel = connection.createChannel();
        inputChannel.exchangeDeclare(flightControlExchangeName, "fanout");
        queueName = inputChannel.queueDeclare().getQueue();
        inputChannel.queueBind(queueName, flightControlExchangeName, "");

        outputChannel = connection.createChannel();
    }
    @Override
    public void run() {

        AtomicBoolean isMasksDeployed = new AtomicBoolean(false);

        // basic exchange consume
        try {

//            ConnectionFactory factory = new ConnectionFactory();
//            Connection connection = factory.newConnection();
//            Channel channel = connection.createChannel();
//
//            channel.exchangeDeclare(flightControlExchangeName, "fanout");
//            String queueName = channel.queueDeclare().getQueue();
//            channel.queueBind(queueName, flightControlExchangeName, "");
//            String queueName = channel.queueDeclare().getQueue();

            System.out.println("Flight Control is listening for messages");
            inputChannel.basicConsume(queueName, true, (consumerTag, delivery) -> {
                String message = new String(delivery.getBody(), "UTF-8");
                System.out.println("Flight Control received - " + message);
                if (message.contains("CP:")) {
                    String[] parts = message.split(":");
                    int cabinPressure = Integer.parseInt(parts[1]);
                    if (cabinPressure < 85 && !isMasksDeployed.get()) {
                        System.out.println("Cabin pressure is low, deploying masks");
                        // deploy oxygen mask
                        // send a message to oxygen mask to come down
                        String oxygenMaskExchangeName = "OM";
//                        Channel oxygenMaskChannel = connection.createChannel();
                        outputChannel.exchangeDeclare(oxygenMaskExchangeName, "fanout");
                        String oxygenMaskMessage = "Deploy Masks";
                        outputChannel.basicPublish(oxygenMaskExchangeName, "", null, oxygenMaskMessage.getBytes("UTF-8"));
                        System.out.println("Flight Control sent: " + oxygenMaskMessage);
//                        try {
//                            outputChannel.close();
//                        } catch (TimeoutException e) {
//                            throw new RuntimeException(e);
//                        }
                    }
                } else if (message.contains("OM:")){
                    // oxygen mask status
                    String[] parts = message.split(":");
                    String oxygenMaskStatus = parts[1];
                    if (oxygenMaskStatus.equals("Deployed")) {
                        isMasksDeployed.set(true);
                    } else if (oxygenMaskStatus.equals("Retracted")) {
                        isMasksDeployed.set(false);
                    }
                }

//                //close the channel
//                try {
//                    channel.close();
//                } catch (TimeoutException e) {
//                    throw new RuntimeException(e);
//                }
            }, consumerTag -> {});
        } catch (Exception e) {
            e.printStackTrace();
        }


    }
}
