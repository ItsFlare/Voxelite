package edu.kit.scc.git.ggd.voxelite.ui;

public class Time {
    private static int hour;
    private static int minute;

    private long start;
    long deltaTime = 0;
    long counter = 0;
    boolean addOnce = true;



    public Time() {
        hour = 12;
        minute = 0;
        start = System.currentTimeMillis();
    }

    public void time() {
        long secPerSecond = 2;
        deltaTime = (System.currentTimeMillis() - start) / 1000;

        if (counter == 0 && addOnce) {
            addOnce = false;
            if(minute == 59) {
                minute = 0;
                hour++;
            } else {
                minute++;
            }
            if(hour == 23) {
                hour = 0;
            }
        }
        counter = deltaTime % secPerSecond;
        if (counter != 0) {
            addOnce = true;
        }
    }

    public static String timeToString() {
        String h = String.valueOf(hour);
        String min = String.valueOf(minute);
        if (hour < 10) {
            h = "0" + h;
        }
        if (minute < 10) {
            min = "0" + min;
        }
        return h + ":" + min;
    }

    public static int getHour() {
        return hour;
    }

    public static int getMinute() {
        return minute;
    }
}
