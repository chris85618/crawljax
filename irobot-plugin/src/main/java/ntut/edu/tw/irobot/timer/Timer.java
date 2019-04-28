package ntut.edu.tw.irobot.timer;

import java.util.concurrent.TimeUnit;

public class Timer {
    private long startTime = 0;
    private long endTime = 0;
    private long totalCost = 0;


    public Timer() {
    }

    public void start() {
        startTime = System.currentTimeMillis();
    }

    public void stop() {
        endTime = System.currentTimeMillis();
        totalCost += endTime - startTime;
    }

    public void reset() {
        startTime = 0;
        endTime = 0;
        totalCost = 0;
    }

    public long getTotalCostTime() {
        return totalCost;
    }

    public String getDurationTime() {
        return getDurationTime(totalCost);
    }

    private String getDurationTime(long time) {
        if(time < 0) {
            throw new IllegalArgumentException("Duration must be greater than zero!");
        }
        long temp = time;

        long hours = TimeUnit.MILLISECONDS.toHours(temp);
        temp -= TimeUnit.HOURS.toMillis(hours);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(temp);
        temp -= TimeUnit.MINUTES.toMillis(minutes);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(temp);

        StringBuilder sb = new StringBuilder(64);
        sb.append(hours);
        sb.append(":");
        sb.append(minutes);
        sb.append(":");
        sb.append(seconds);

        return sb.toString();
    }
}
