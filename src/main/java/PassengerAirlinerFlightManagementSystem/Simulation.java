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
//public abstract class Simulation implements Runnable {
//    protected final Channel channel;
//    protected final String queueName;
//    protected final ScheduledExecutorService service;
//    private boolean firstTime = true;
//
//    public Simulation(Connection connection, ScheduledExecutorService service) throws IOException,
//            TimeoutException {
//        channel = connection.createChannel();
//        channel.exchangeDeclare(Exchanges.SENSOR_INPUT, "direct");
//        queueName = channel.queueDeclare().getQueue();
//        this.service = service;
//    }
//
//    @Override
//    public void run() {
//        simulateWrapper();
//    }
//
//    protected void publishChange(String outputSensor, byte[] change) {
//        try {
//            channel.basicPublish(Exchanges.SENSOR_INPUT, outputSensor, null, change);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    private void simulateWrapper() {
//        if (toContinue()) {
//            if (!firstTime) {
//                simulate();
//            }
//            firstTime = false;
//            service.schedule(this::simulateWrapper, getIntervalInMillis(), TimeUnit.MILLISECONDS);
//        }
//    }
//
//    protected abstract void simulate();
//
//    protected abstract boolean toContinue();
//
//    protected abstract int getIntervalInMillis();
//}
