package com.weathersystem.shared.clock;

import java.util.concurrent.atomic.AtomicLong;

public class LamportClock {
    private final AtomicLong clock;

    public LamportClock() {
        this.clock = new AtomicLong(0);
    }

    // Increment clock for local events
    public long tick() {
        return clock.incrementAndGet();
    }

    // Update clock when receiving message from another component
    public long update(long receivedTime) {
        return clock.updateAndGet(current -> Math.max(current, receivedTime) + 1);
    }

    // Get current time without incrementing
    public long getTime() {
        return clock.get();
    }

    @Override
    public String toString() {
        return "LamportClock{time=" + clock.get() + "}";
    }
}