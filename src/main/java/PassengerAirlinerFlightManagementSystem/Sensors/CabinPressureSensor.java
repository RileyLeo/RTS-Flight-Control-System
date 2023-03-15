package PassengerAirlinerFlightManagementSystem.Sensors;

import PassengerAirlinerFlightManagementSystem.FCSMain;
import PassengerAirlinerFlightManagementSystem.FlightControl;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class CabinPressureSensor implements Runnable {

    Channel inputChannel;
    Channel outputChannel;
    String queueName;
    Random rand = new Random();
    AtomicBoolean isDropping = new AtomicBoolean(true);
    ScheduledExecutorService timer;

    public CabinPressureSensor(Connection connection) throws IOException {
//        timer = Executors.newScheduledThreadPool(1);
//        timer.scheduleAtFixedRate(new CabinPressureSensorInput(this), 0, 1000, TimeUnit.MILLISECONDS);

        inputChannel = connection.createChannel();
        inputChannel.exchangeDeclare(FCSMain.cabinPressureExchangeName, "fanout");
        queueName = inputChannel.queueDeclare().getQueue();
        inputChannel.queueBind(queueName, FCSMain.cabinPressureExchangeName, "");

        outputChannel = connection.createChannel();
        outputChannel.exchangeDeclare(FCSMain.flightControlExchangeName, "fanout");
    }


    public Integer CabinPressurePercentage = 100;
    AtomicBoolean isLanding = new AtomicBoolean(false);
    AtomicBoolean oneMore = new AtomicBoolean(true);
    AtomicBoolean firstRun = new AtomicBoolean(true);

    @Override
    public void run() {
        if (firstRun.get()){
            CabinPressureSensorInput cabinPressureSensorInput = new CabinPressureSensorInput(this);
            Thread thread = new Thread(cabinPressureSensorInput);
            thread.start();
        }

        // adjust the cabin pressure
        int percentageDrop = rand.nextInt(5);

        if (isDropping.get()) {
            CabinPressurePercentage -= percentageDrop;
        } else if (CabinPressurePercentage < 100) {
            CabinPressurePercentage += percentageDrop;
            if (CabinPressurePercentage > 100) {
                CabinPressurePercentage = 100;
            }
        }

        //Initialize cabin pressure restoration if cabin pressure is below 40%
        if (CabinPressurePercentage <= 40 && isLanding.get() == false) {
            System.out.println("\u001B[31m" + "Cabin Pressure Sensor:" + "\u001B[0m" + "Cabin pressure system is fixed, restoring cabin pressure");
            isDropping.set(false);
        }

        try {
            if ((CabinPressurePercentage < 100 && isLanding.get() == false && FlightControl.turbulanceCountermeasures.get() == false) || oneMore.get() == true ) {
                //get time in nanoseconds
                long time = System.nanoTime();
                String message = FCSMain.cabinPressureExchangeName + ":" + CabinPressurePercentage.toString() + "-" + time;
                outputChannel.basicPublish(FCSMain.flightControlExchangeName, "", null, message.getBytes("UTF-8"));
                System.out.println("\u001B[31m" + "Cabin Pressure Sensor:" + "\u001B[0m" + CabinPressurePercentage.toString() + " % cabin pressure sent to flight control");
                if (CabinPressurePercentage == 100) {
                    oneMore.set(false);
                    System.out.println("\u001B[31m" + "Cabin Pressure Sensor:" + "\u001B[0m" + "Cabin pressure restored to 100%, stop sending messages");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

}

class CabinPressureSensorInput implements Runnable {

    CabinPressureSensor cabinPressureSensor;

    public CabinPressureSensorInput(CabinPressureSensor cabinPressureSensor) {
        this.cabinPressureSensor = cabinPressureSensor;
    }

    @Override
    public void run() {
        // retrieve updated wing flaps status from the flight control system
        try {
            cabinPressureSensor.inputChannel.basicConsume(cabinPressureSensor.queueName, true, (consumerTag, delivery) -> {
                String message = new String(delivery.getBody(), "UTF-8");
                if (message.equals("Landing")) {
                    System.out.println("\u001B[31m" + "Cabin Pressure Sensor:" + "\u001B[0m" + "Ladning, stop sending cabin pressure to flight control");
                    cabinPressureSensor.isLanding.set(true);
                }
            }, consumerTag -> {
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
