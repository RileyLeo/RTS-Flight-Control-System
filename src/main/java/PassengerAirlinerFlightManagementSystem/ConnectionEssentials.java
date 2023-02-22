// its very little might not need eh, lazy oop this shit

//package PassengerAirlinerFlightManagementSystem;
//
//import com.rabbitmq.client.Channel;
//import com.rabbitmq.client.Connection;
//
//import java.io.IOException;
//
//public abstract class ConnectionEssentials{
////    protected final Channel channel;
////    protected final String queueName;
////
////    public ConnectionEssentials(Connection connection, ScheduledExecutorService service) throws IOException,
////            TimeoutException {
////        channel = connection.createChannel();
////        channel.exchangeDeclare(Exchanges.SENSOR_INPUT, "direct");
////        queueName = channel.queueDeclare().getQueue();
////        this.service = service;
////    }
//
//    Channel inputChannel;
//    Channel outputChannel;
//    String queueName;
//    boolean isMaskDropped = false;
//    public ConnectionEssentials(Connection connection) throws IOException {
//        inputChannel = connection.createChannel();
//        queueName = inputChannel.queueDeclare().getQueue();
////        inputChannel.exchangeDeclare(FCSMain.oxygenMaskExchangeName, "fanout");
////        inputChannel.queueBind(queueName, FCSMain.oxygenMaskExchangeName, "");
//        outputChannel = connection.createChannel();
//    }
//}
