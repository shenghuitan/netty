package io.netty.example.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Timer {

    private static final long NANOS = 1_000_000L;
    private static final long MICROS = 1_000L;
    private List<Long> times;

    private Timer() {
        times = new ArrayList<>();
        times.add(System.nanoTime());
    }

    public static Timer init() {
        return new Timer();
    }

    public Timer mark() {
        times.add(System.nanoTime());
        return this;
    }

    public List<Long> getTimes() {
        return times;
    }

    public List<Long> costs() {
        List<Long> millis = new ArrayList<>(times.size());
        Iterator<Long> iterator = times.iterator();
        Long last = iterator.next();
        while (iterator.hasNext()) {
            Long next = iterator.next();
            millis.add((next - last) / NANOS);
            last = next;
        }
        return millis;
    }

    public long cost() {
        if (times.size() >= 2) {
            int last = times.size() - 1;
            return (times.get(last) - times.get(last - 1)) / NANOS;
        }
        return 0L;
    }

    public long total() {
        if (times.size() >= 2) {
            int last = times.size() - 1;
            return (times.get(last) - times.get(0)) / NANOS;
        }
        return 0L;
    }

    public long average() {
        if (times.size() >= 2) {
            return total() / (times.size() - 1);
        }
        return 0L;
    }

    public long averageMicros() {
        if (times.size() >= 2) {
            int last = times.size() - 1;
            return (times.get(last) - times.get(0)) / MICROS / last;
        }
        return 0L;
    }

    public int size() {
        return times.size() - 1;
    }


    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("size=").append(size());
        sb.append(", total=").append(total());
        sb.append(", average=").append(average());
        sb.append(", averageMicros=").append(averageMicros());
        sb.append('}');
        return sb.toString();
    }
}
