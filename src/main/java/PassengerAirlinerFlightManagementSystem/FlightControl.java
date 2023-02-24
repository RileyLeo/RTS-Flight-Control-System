package PassengerAirlinerFlightManagementSystem;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
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
    int middleboundAltitudeThreshold = 2000; //34500 +- 2000 = (32500 to 36500)
    String flapPosition = "neutral";
    int upperboundSpeedDuringFlight = 1000;
    int middleboundSpeedDuringFlight = 900;
    int lowerboundSpeedDuringFlight = 800;
    int middleboundSpeedThreshold = 50; //900 +- 50 = (850 to 950)
    String engineMode = "Moderate";
    String currentWeather;


    @Override
    public void run() {

        try {

//            System.out.println("Flight Control is listening for messages");
            inputChannel.basicConsume(queueName, true, (consumerTag, delivery) -> {
                String message = new String(delivery.getBody(), "UTF-8");
//                System.out.println("Flight Control received - " + message);
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
                    } else if (altitude >= middleboundAltitudeDuringFlight - middleboundAltitudeThreshold && altitude <= middleboundAltitudeDuringFlight + middleboundAltitudeThreshold && !isLanding && !flapPosition.equals("neutral")){
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
                } else if (message.contains("SP:")){
                    //Speed Status
                    String[] parts = message.split(":");
                    int speed = Integer.parseInt(parts[1]);
                    if(speed > upperboundSpeedDuringFlight && !isLanding && !engineMode.equals("Decelerated")){
                        System.out.println("Speed is high, decelerate engine to slow down");
                        // decelerate engine to slow down
                        // send a message to engine to decelerate
                        outputChannel.exchangeDeclare(FCSMain.engineExchangeName, "fanout");
                        String engineMessage = "Decelerate";
                        outputChannel.basicPublish(FCSMain.engineExchangeName, "", null, engineMessage.getBytes("UTF-8"));
                    } else if(speed < lowerboundSpeedDuringFlight && !isLanding && !engineMode.equals("Accelerated")){
                        System.out.println("Speed is low, accelerate engine to speed up");
                        // accelerate engine to speed up
                        // send a message to engine to accelerate
                        outputChannel.exchangeDeclare(FCSMain.engineExchangeName, "fanout");
                        String engineMessage = "Accelerate";
                        outputChannel.basicPublish(FCSMain.engineExchangeName, "", null, engineMessage.getBytes("UTF-8"));
                    } else if (speed >= middleboundSpeedDuringFlight - middleboundSpeedThreshold && speed <= middleboundSpeedDuringFlight + middleboundSpeedThreshold && !isLanding && !engineMode.equals("Moderate")){
                        System.out.println("Normal Speed, putting engine to moderate speed");
                        // maintain engine speed
                        // send a message to engine to maintain speed
                        outputChannel.exchangeDeclare(FCSMain.engineExchangeName, "fanout");
                        String engineMessage = "Moderate";
                        outputChannel.basicPublish(FCSMain.engineExchangeName, "", null, engineMessage.getBytes("UTF-8"));
                    }
                } else if (message.contains("EN:")){
                    // Engine Status
                    String[] parts = message.split(":");
                    String engineStatus = parts[1];
                    if (engineStatus.equals("Engine decelerated")) {
                        System.out.println("Engine decelerated successfully");
                        engineMode = "Decelerated";
                    } else if (engineStatus.equals("Engine accelerated")) {
                        System.out.println("Engine accelerated successfully");
                        engineMode = "Accelerated";
                    } else if (engineStatus.equals("Engine moderated")) {
                        System.out.println("Engine speed maintained successfully");
                        engineMode = "Moderate";
                    }
                    //Inform Speed sensor of the adjustment
                    outputChannel.exchangeDeclare(FCSMain.speedExchangeName, "fanout");
                    String speedControlMessage = engineStatus;
                    outputChannel.basicPublish(FCSMain.speedExchangeName, "", null, speedControlMessage.getBytes("UTF-8"));
                } else if (message.contains("WE:")){
                    String[] parts = message.split(":");
                    String weatherStatus = parts[1];
                    if (weatherStatus.equals("Clear")) {
                        System.out.println("Weather is clear, no need to adjust");
                        currentWeather = "Clear";
                    } else if (weatherStatus.equals("Rain")) {
                        System.out.println("Weather is rainy, watch up for potential thunderstorm!");
                        currentWeather = "Rain";
                    } else if (weatherStatus.equals("Thunderstorm")) {
                        System.out.println("There is a thunderstorm, turbulence is expected!");
                        currentWeather = "Thunderstorm";
                        System.out.println("Activating turbulence countermeasures");
                        // activate turbulence countermeasures
                        turbulenceSequence();
                    }
                }


            }, consumerTag -> {
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void landingSequence() {
//try {
//            // send a message to wing flaps to adjust
//            outputChannel.exchangeDeclare(FCSMain.wingFlapsExchangeName, "fanout");
//            String wingFlapsMessage = "Adjust:Lower";
//            outputChannel.basicPublish(FCSMain.wingFlapsExchangeName, "", null, wingFlapsMessage.getBytes("UTF-8"));
//            // send a message to engine to decelerate
//            outputChannel.exchangeDeclare(FCSMain.engineExchangeName, "fanout");
//            String engineMessage = "Decelerate";
//            outputChannel.basicPublish(FCSMain.engineExchangeName, "", null, engineMessage.getBytes("UTF-8"));
//            // send a message to landing gear to deploy
//            outputChannel.exchangeDeclare(FCSMain.landingGearExchangeName, "fanout");
//            String landingGearMessage = "Deploy";
//            outputChannel.basicPublish(FCSMain.landingGearExchangeName, "", null, landingGearMessage.getBytes("UTF-8"));
//            // send a message to air brakes to deploy
//            outputChannel.exchangeDeclare(FCSMain.airBrakesExchangeName, "fanout");
//            String airBrakesMessage = "Deploy";
//            outputChannel.basicPublish(FCSMain.airBrakesExchangeName, "", null, airBrakesMessage.getBytes("UTF-8"));
//            // send a message to landing gear to deploy
//            outputChannel.exchangeDeclare(FCSMain.landingGearExchangeName, "fanout");
//            String landingGearMessage2 = "Deploy";
//            outputChannel.basicPublish(FCSMain.landingGearExchangeName, "", null, landingGearMessage2.getBytes("UTF-8"));
//            // send a message to air brakes to deploy
//            outputChannel.exchangeDeclare(FCSMain.airBrakesExchangeName, "fanout");
//            String airBrakesMessage2 = "Deploy";
//            outputChannel.basicPublish(FCSMain.airBrakesExchangeName, "", null, airBrakesMessage2.getBytes("UTF-8"));
//            // send a message to landing gear to deploy
//            outputChannel.exchangeDeclare(FCSMain.landingGearExchangeName, "fanout");
//            String landingGearMessage3 = "Deploy";
//            outputChannel.basicPublish(FCSMain.landingGearExchangeName, "", null, landingGearMessage3.getBytes("UTF-8"));
//            // send a message to air brakes to deploy
//            outputChannel.exchangeDeclare(FCSMain.airBrakesExchangeName, "fanout");
//            String airBrakes

    }


        public void turbulenceSequence() {
            try {
                //turbulence countermeasures
                // send a message to wing flaps to adjust
                outputChannel.exchangeDeclare(FCSMain.wingFlapsExchangeName, "fanout");
                String wingFlapsMessage = "Adjust:Lower";
                outputChannel.basicPublish(FCSMain.wingFlapsExchangeName, "", null, wingFlapsMessage.getBytes("UTF-8"));
                // send a message to altitude sensor to adjust
                outputChannel.exchangeDeclare(FCSMain.altitudeExchangeName, "fanout");
                String altitudeMessage = "Wing flaps adjusted lower";
                outputChannel.basicPublish(FCSMain.altitudeExchangeName, "", null, altitudeMessage.getBytes("UTF-8"));

                // send a message to engine to decelerate
                outputChannel.exchangeDeclare(FCSMain.engineExchangeName, "fanout");
                String engineMessage = "Decelerate";
                outputChannel.basicPublish(FCSMain.engineExchangeName, "", null, engineMessage.getBytes("UTF-8"));
                // send a message to speed sensor to adjust
                outputChannel.exchangeDeclare(FCSMain.speedExchangeName, "fanout");
                String engineStatus = "Engine decelerated";
                outputChannel.basicPublish(FCSMain.speedExchangeName, "", null, engineStatus.getBytes("UTF-8"));

                // sleep for 5 seconds
                Thread.sleep(5000);

                //turbulence solved
                // send a message to Weather Environment sensor to inform
                outputChannel.exchangeDeclare(FCSMain.weatherEnvironmentExchangeName, "fanout");
                String weatherMessage = "Turbulence resolved";
                outputChannel.basicPublish(FCSMain.weatherEnvironmentExchangeName, "", null, weatherMessage.getBytes("UTF-8"));

            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
    }

}
