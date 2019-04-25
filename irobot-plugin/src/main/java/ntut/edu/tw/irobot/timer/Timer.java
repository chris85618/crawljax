package ntut.edu.tw.irobot.timer;

import java.util.concurrent.TimeUnit;

public class Timer {
    private long startTime;
    private long endTime;
    private long episodeCost;
    private long totalCost;


    public void Timer() {
        startTime = 0;
        endTime = 0;
        totalCost = 0;
        episodeCost = 0;
    }

    public void start() {
        startTime = System.currentTimeMillis();
    }

    public void stop() {
        endTime = System.currentTimeMillis();
        episodeCost += endTime - startTime;
    }

    public void reset() {
        totalCost += episodeCost;
        startTime = 0;
        endTime = 0;
        episodeCost = 0;
    }

    public long getTotalCostTime() {
        return totalCost;
    }

    public String getDurationTime() {
        if(totalCost < 0) {
            throw new IllegalArgumentException("Duration must be greater than zero!");
        }
        long temp = totalCost;

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
