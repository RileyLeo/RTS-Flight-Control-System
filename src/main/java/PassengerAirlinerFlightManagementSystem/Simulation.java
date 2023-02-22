//package PassengerAirlinerFlightManagementSystem;
//
//import com.rabbitmq.client.Channel;
//import com.rabbitmq.client.Connection;
//import com.rabbitmq.client.ConnectionFactory;
//
//import java.io.IOException;
//import java.util.concurrent.ScheduledExecutorService;
//import java.util.concurrent.TimeUnit;
//import java.util.concurrent.TimeoutException;
//
//public abstract class ConnectionEssentials{
//    protected final Channel channel;
//    protected final String queueName;
//
//    public ConnectionEssentials(Connection connection, ScheduledExecutorService service) throws IOException,
//            TimeoutException {
//        channel = connection.createChannel();
//        channel.exchangeDeclare(Exchanges.SENSOR_INPUT, "direct");
//        queueName = channel.queueDeclare().getQueue();
//        this.service = service;
//    }
//}
