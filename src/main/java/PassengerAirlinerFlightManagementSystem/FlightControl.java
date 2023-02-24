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
    public static volatile boolean isLanding;

    public FlightControl(Connection connection) throws IOException {
        inputChannel = connection.createChannel();
        inputChannel.exchangeDeclare(FCSMain.flightControlExchangeName, "fanout");
        queueName = inputChannel.queueDeclare().getQueue();
        inputChannel.queueBind(queueName, FCSMain.flightControlExchangeName, "");

        outputChannel = connection.createChannel();
        isLanding = false;
    }

    //Dependencies variables
    boolean isMasksDeployed = false;
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
        //test
        try {
//            System.out.println("Flight Control is listening for messages");
            inputChannel.basicConsume(queueName, true, (consumerTag, delivery) -> {
                if (isLanding) {
                    //Landing sequence
                    landingSequence();
                } else {
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
                        if (altitude > upperboundAltitudeDuringFlight && !flapPosition.equals("low")) {
                            System.out.println("Altitude is high, adjusting wing flaps to a lower angle");
                            // adjust wing flaps to a lower angle
                            // send a message to wing flaps to adjust
                            outputChannel.exchangeDeclare(FCSMain.wingFlapsExchangeName, "fanout");
                            String wingFlapsMessage = "Adjust:Lower";
                            outputChannel.basicPublish(FCSMain.wingFlapsExchangeName, "", null, wingFlapsMessage.getBytes("UTF-8"));
                        } else if (altitude < lowerboundAltitudeDuringFlight && !flapPosition.equals("high")) {
                            System.out.println("Altitude is low, adjusting wing flaps to a higher angle");
                            // adjust wing flaps to a higher angle
                            // send a message to wing flaps to adjust
                            outputChannel.exchangeDeclare(FCSMain.wingFlapsExchangeName, "fanout");
                            String wingFlapsMessage = "Adjust:Higher";
                            outputChannel.basicPublish(FCSMain.wingFlapsExchangeName, "", null, wingFlapsMessage.getBytes("UTF-8"));
                        } else if (altitude >= middleboundAltitudeDuringFlight - middleboundAltitudeThreshold && altitude <= middleboundAltitudeDuringFlight + middleboundAltitudeThreshold && !flapPosition.equals("neutral")) {
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
                    } else if (message.contains("SP:")) {
                        //Speed Status
                        String[] parts = message.split(":");
                        int speed = Integer.parseInt(parts[1]);
                        if (speed > upperboundSpeedDuringFlight && !engineMode.equals("Decelerated")) {
                            System.out.println("Speed is high, decelerate engine to slow down");
                            // decelerate engine to slow down
                            // send a message to engine to decelerate
                            outputChannel.exchangeDeclare(FCSMain.engineExchangeName, "fanout");
                            String engineMessage = "Decelerate";
                            outputChannel.basicPublish(FCSMain.engineExchangeName, "", null, engineMessage.getBytes("UTF-8"));
                        } else if (speed < lowerboundSpeedDuringFlight && !engineMode.equals("Accelerated")) {
                            System.out.println("Speed is low, accelerate engine to speed up");
                            // accelerate engine to speed up
                            // send a message to engine to accelerate
                            outputChannel.exchangeDeclare(FCSMain.engineExchangeName, "fanout");
                            String engineMessage = "Accelerate";
                            outputChannel.basicPublish(FCSMain.engineExchangeName, "", null, engineMessage.getBytes("UTF-8"));
                        } else if (speed >= middleboundSpeedDuringFlight - middleboundSpeedThreshold && speed <= middleboundSpeedDuringFlight + middleboundSpeedThreshold && !engineMode.equals("Moderate")) {
                            System.out.println("Normal Speed, putting engine to moderate speed");
                            // maintain engine speed
                            // send a message to engine to maintain speed
                            outputChannel.exchangeDeclare(FCSMain.engineExchangeName, "fanout");
                            String engineMessage = "Moderate";
                            outputChannel.basicPublish(FCSMain.engineExchangeName, "", null, engineMessage.getBytes("UTF-8"));
                        }
                    } else if (message.contains("EN:")) {
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
                    } else if (message.contains("WE:")) {
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
                }
            }, consumerTag -> {
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static volatile boolean isLandingGearDeployed = false;
    public static volatile boolean isSpeedZero = false;
    public static volatile boolean isAltitudeZero = false;
    boolean landingMessageSent = false;

    public void landingSequence() {
        try {
            if (landingMessageSent == false) {// send a message to wing flaps to adjust
                //send a message to weather environment to stop sending weather updates
                outputChannel.exchangeDeclare(FCSMain.weatherEnvironmentExchangeName, "fanout");
                String weatherMessage = "Landing";
                outputChannel.basicPublish(FCSMain.weatherEnvironmentExchangeName, "", null, weatherMessage.getBytes("UTF-8"));

                //send a message to the cabin to stop sending cabin updates
                outputChannel.exchangeDeclare(FCSMain.cabinPressureExchangeName, "fanout");
                String cabinMessage = "Landing";
                outputChannel.basicPublish(FCSMain.cabinPressureExchangeName, "", null, cabinMessage.getBytes("UTF-8"));

                outputChannel.exchangeDeclare(FCSMain.wingFlapsExchangeName, "fanout");
                String wingFlapsMessage = "Adjust:Lower";
                outputChannel.basicPublish(FCSMain.wingFlapsExchangeName, "", null, wingFlapsMessage.getBytes("UTF-8"));
                // send a message to altitude sensor to decelerate
                outputChannel.exchangeDeclare(FCSMain.altitudeExchangeName, "fanout");
                String altitudeControlMessage = "Landing mode activate, Wing flaps adjusted lower";
                outputChannel.basicPublish(FCSMain.altitudeExchangeName, "", null, altitudeControlMessage.getBytes("UTF-8"));

                // send a message to engine to decelerate
                outputChannel.exchangeDeclare(FCSMain.engineExchangeName, "fanout");
                String engineMessage = "Decelerate";
                outputChannel.basicPublish(FCSMain.engineExchangeName, "", null, engineMessage.getBytes("UTF-8"));
                // send a message to speed sensor to adjust
                outputChannel.exchangeDeclare(FCSMain.speedExchangeName, "fanout");
                String engineStatus = "Landing mode activate, Engine decelerated";
                outputChannel.basicPublish(FCSMain.speedExchangeName, "", null, engineStatus.getBytes("UTF-8"));

                // send a message to landing gear to deploy
                outputChannel.exchangeDeclare(FCSMain.landingGearExchangeName, "fanout");
                String landingGearMessage = "Deploy Landing Gear";
                outputChannel.basicPublish(FCSMain.landingGearExchangeName, "", null, landingGearMessage.getBytes("UTF-8"));

                System.out.println("Landing...");
                landingMessageSent = true;
            }

            // receive message to ensure everything is in order
            inputChannel.basicConsume(queueName, true, (consumerTag, delivery) -> {
                String message = new String(delivery.getBody(), "UTF-8");
                if (message.contains("LG:")) {
                    String[] parts = message.split(":");
                    String landingGearStatus = parts[1];
                    if (landingGearStatus.equals("Landing Gear Deployed")) {
                        System.out.println("Landing gear deployed successfully");
                        isLandingGearDeployed = true;
                        //send message to landing gear, acknowledging its message
                        outputChannel.exchangeDeclare(FCSMain.landingGearExchangeName, "fanout");
                        String landingGearMessage = "Message Acknowledged";
                        outputChannel.basicPublish(FCSMain.landingGearExchangeName, "", null, landingGearMessage.getBytes("UTF-8"));
                    }
                } else if (message.contains("SP:")) {
                    //Speed Status
                    String[] parts = message.split(":");
                    int speed = Integer.parseInt(parts[1]);
                    if (speed == 0) {
                        System.out.println("Speed is zero, deceleration successful");
                        isSpeedZero = true;
                        //send message to speed sensor, acknowledging its message
                        outputChannel.exchangeDeclare(FCSMain.speedExchangeName, "fanout");
                        String speedMessage = "Message Acknowledged";
                        outputChannel.basicPublish(FCSMain.speedExchangeName, "", null, speedMessage.getBytes("UTF-8"));
                    }
                } else if (message.contains("AL:")) {
                    //Altitude Status
                    String[] parts = message.split(":");
                    int altitude = Integer.parseInt(parts[1]);
                    if (altitude == 0) {
                        System.out.println("Altitude is zero, altitude reduction successful");
                        isAltitudeZero = true;
                        //send message to altitude sensor, acknowledging its message
                        outputChannel.exchangeDeclare(FCSMain.altitudeExchangeName, "fanout");
                        String altitudeMessage = "Message Acknowledged";
                        outputChannel.basicPublish(FCSMain.altitudeExchangeName, "", null, altitudeMessage.getBytes("UTF-8"));
                    }
                }
            }, consumerTag -> {
            });
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
            String altitudeMessage = "Landing mode activate, Wing flaps adjusted lower";
            outputChannel.basicPublish(FCSMain.altitudeExchangeName, "", null, altitudeMessage.getBytes("UTF-8"));

            // send a message to engine to decelerate
            outputChannel.exchangeDeclare(FCSMain.engineExchangeName, "fanout");
            String engineMessage = "Decelerate";
            outputChannel.basicPublish(FCSMain.engineExchangeName, "", null, engineMessage.getBytes("UTF-8"));
            // send a message to speed sensor to adjust
            outputChannel.exchangeDeclare(FCSMain.speedExchangeName, "fanout");
            String engineStatus = "Landing mode activate, Engine decelerated";
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
