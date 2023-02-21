package PassengerAirlinerFlightManagementSystem.Actuators;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

public class OxygenMasks implements Runnable {
    @Override
    public void run() {
//        AtomicBoolean isMaskDropped = new AtomicBoolean(false);
//        String oxygenMaskExchangeName = "OM";
//        String flightControlExchangeName = "FC";
//        try {
//            ConnectionFactory factory = new ConnectionFactory();
//            Connection connection = factory.newConnection();
//            Channel channel = connection.createChannel();
//
//            channel.exchangeDeclare(oxygenMaskExchangeName, "fanout");
//            String queueName = channel.queueDeclare().getQueue();
//            channel.queueBind(queueName, oxygenMaskExchangeName, "");
//
//            channel.basicConsume(queueName, true,  (consumerTag, delivery) -> {
//                String message = new String(delivery.getBody(), "UTF-8");
//                System.out.println("Oxygen Masks received - " + message);
//                if (message.equals("Deploy Masks")) {
//                    if (!isMaskDropped.get()) {
//                        System.out.println("Oxygen Masks are deployed");
//                        isMaskDropped.set(true);
//                    } else{
//                        System.out.println("Oxygen Masks are already deployed");
//                    }
//                } else if (message.equals("Retract Masks")) {
//                    if (isMaskDropped.get()) {
//                        System.out.println("Oxygen Masks have been retracted");
//                        isMaskDropped.set(false);
//                    } else{
//                        System.out.println("Oxygen Masks are already retracted");
//                    }
//                }
//
//                // send a message to flight control to inform that oxygen masks are deployed or retracted
//                Channel flightControlChannel = connection.createChannel();
//                flightControlChannel.exchangeDeclare(flightControlExchangeName, "fanout");
//                String status = isMaskDropped.get() ? "Deployed" : "Retracted";
//                String flightControlMessage = "OM: Masks" + status;
//                flightControlChannel.basicPublish(flightControlExchangeName, "", null, flightControlMessage.getBytes("UTF-8"));
//                System.out.println("Oxygen Masks sent: " + flightControlMessage);
//                try {
//                    flightControlChannel.close();
//                } catch (TimeoutException e) {
//                    throw new RuntimeException(e);
//                }
//            }, consumerTag -> {});
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        } catch (TimeoutException e) {
//            throw new RuntimeException(e);
//        }
    }
}

