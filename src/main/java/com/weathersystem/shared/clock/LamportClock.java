package com.weathersystem.shared.clock;

import java.util.concurrent.atomic.AtomicLong;

// Lamport clock for ordering events in distributed systems
public class LamportClock {
    private final AtomicLong clock;

    // Sets up the clock starting at zero
    public LamportClock() {
        this.clock = new AtomicLong(0);
    }

    // Increments the clock for local events and returns the new time
    public long tick() {
        return clock.incrementAndGet();
    }

    // Updates the clock when receiving a message from another component
    public long update(long receivedTime) {
        return clock.updateAndGet(current -> Math.max(current, receivedTime) + 1);
    }

    // Gets the current time without changing the clock
    public long getTime() {
        return clock.get();
    }

    @Override
    public String toString() {
        return "LamportClock{time=" + clock.get() + "}";
    }
}