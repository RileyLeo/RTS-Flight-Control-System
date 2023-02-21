package PassengerAirlinerFlightManagementSystem.Sensors;

import java.util.Random;

public class AltitudeSensor implements Runnable {
    Random rand = new Random();
    public static int CurrentAlt = 9500;

    @Override
    public void run() {
        // adjust the altitude
        int change = rand.nextInt(100);
        if (rand.nextBoolean()) {
            change *= -1;
        }
        CurrentAlt += change;

        System.out.println("altitiude = " + CurrentAlt);
    }

}
