package com.weathersystem.shared.clock;

import java.util.concurrent.atomic.AtomicLong;

// Implementation of Lamport logical clock for distributed system event ordering
public class LamportClock {
    private final AtomicLong clock;

    // Constructor that initializes the clock to zero
    public LamportClock() {
        this.clock = new AtomicLong(0);
    }

    // Increment clock for local events and return new timestamp
    public long tick() {
        return clock.incrementAndGet();
    }

    // Update clock when receiving message from another component in distributed system
    public long update(long receivedTime) {
        return clock.updateAndGet(current -> Math.max(current, receivedTime) + 1);
    }

    // Get current time without incrementing the clock
    public long getTime() {
        return clock.get();
    }

    @Override
    public String toString() {
        return "LamportClock{time=" + clock.get() + "}";
    }
}