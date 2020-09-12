package io.netty.example.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Timer {

    private static final long UNIT = 1_000_000L;
    private List<Long> times;

    private Timer() {
        times = new ArrayList<>();
        times.add(System.nanoTime());
    }

    public static Timer init() {
        return new Timer();
    }

    public synchronized Timer mark() {
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
            millis.add((next - last) / UNIT);
            last = next;
        }
        return millis;
    }

    public long cost() {
        if (times.size() >= 2) {
            int last = times.size() - 1;
            return (times.get(last) - times.get(last - 1)) / UNIT;
        }
        return 0L;
    }

    public long total() {
        if (times.size() >= 2) {
            int last = times.size() - 1;
            return (times.get(last) - times.get(0)) / UNIT;
        }
        return 0L;
    }

    public int size() {
        return times.size() - 1;
    }
}
