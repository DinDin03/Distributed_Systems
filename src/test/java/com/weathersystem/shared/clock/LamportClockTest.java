package com.weathersystem.shared.clock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.RepeatedTest;
import org.awaitility.Awaitility;

import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class LamportClockTest {

    private LamportClock clock;

    @BeforeEach
    void setUp() {
        clock = new LamportClock();
    }

    @Test
    void testInitialClockTime() {
        assertEquals(0, clock.getTime(), "Clock should start at 0");
    }

    @Test
    void testBasicTickOperations() {
        // Test that tick produces monotonically increasing values
        long time1 = clock.tick();
        long time2 = clock.tick();
        long time3 = clock.tick();

        assertEquals(1, time1, "First tick should return 1");
        assertEquals(2, time2, "Second tick should return 2");
        assertEquals(3, time3, "Third tick should return 3");
        assertEquals(3, clock.getTime(), "Current time should match last tick");
    }

    @Test
    void testUpdateMechanismWithLowerTime() {
        // Initialize clock to some value
        clock.tick(); // time = 1
        clock.tick(); // time = 2

        // Update with lower received time
        long result = clock.update(1);

        assertEquals(3, result, "Update with lower time should increment current time");
        assertEquals(3, clock.getTime(), "Clock time should be max(current, received) + 1");
    }

    @Test
    void testUpdateMechanismWithHigherTime() {
        // Initialize clock to some value
        clock.tick(); // time = 1

        // Update with higher received time
        long result = clock.update(5);

        assertEquals(6, result, "Update should return max(1, 5) + 1 = 6");
        assertEquals(6, clock.getTime(), "Clock time should be updated to 6");
    }

    @Test
    void testUpdateMechanismWithEqualTime() {
        clock.tick(); // time = 1
        clock.tick(); // time = 2

        // Update with equal received time
        long result = clock.update(2);

        assertEquals(3, result, "Update with equal time should increment");
        assertEquals(3, clock.getTime(), "Clock time should be incremented");
    }

    @Test
    void testGetTimeDoesNotIncrement() {
        clock.tick(); // time = 1

        long time1 = clock.getTime();
        long time2 = clock.getTime();
        long time3 = clock.getTime();

        assertEquals(1, time1, "getTime should return current time");
        assertEquals(1, time2, "getTime should not increment clock");
        assertEquals(1, time3, "getTime should still not increment clock");
    }

    @Test
    void testConcurrentTickOperations() throws InterruptedException {
        final int numThreads = 10;
        final int operationsPerThread = 1000;
        final CountDownLatch latch = new CountDownLatch(numThreads);
        final List<Long> results = Collections.synchronizedList(new ArrayList<>());

        // Create threads that perform tick operations
        for (int i = 0; i < numThreads; i++) {
            new Thread(() -> {
                try {
                    for (int j = 0; j < operationsPerThread; j++) {
                        results.add(clock.tick());
                    }
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS), "All threads should complete within timeout");

        // Verify all results are unique and sequential
        results.sort(Long::compareTo);
        assertEquals(numThreads * operationsPerThread, results.size(), "Should have all results");

        for (int i = 0; i < results.size(); i++) {
            assertEquals(i + 1, results.get(i).longValue(),
                "Results should be sequential from 1 to " + (numThreads * operationsPerThread));
        }
    }

    @Test
    void testConcurrentUpdateOperations() throws InterruptedException {
        final int numThreads = 10;
        final CountDownLatch latch = new CountDownLatch(numThreads);
        final AtomicLong maxObservedTime = new AtomicLong(0);

        // Create threads that perform update operations with increasing received times
        for (int i = 0; i < numThreads; i++) {
            final int threadId = i;
            new Thread(() -> {
                try {
                    long result = clock.update(threadId * 10); // Use different received times
                    maxObservedTime.updateAndGet(current -> Math.max(current, result));
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS), "All threads should complete within timeout");

        // The final clock time should be at least the maximum received time + 1
        long finalTime = clock.getTime();
        assertTrue(finalTime >= maxObservedTime.get(),
            "Final time should be at least the maximum observed time");
    }

    @Test
    void testMixedConcurrentOperations() throws InterruptedException {
        final int numThreads = 20;
        final CountDownLatch latch = new CountDownLatch(numThreads);
        final AtomicInteger tickCount = new AtomicInteger(0);
        final AtomicInteger updateCount = new AtomicInteger(0);

        // Create mixed threads - some doing ticks, some doing updates
        for (int i = 0; i < numThreads; i++) {
            final int threadId = i;
            new Thread(() -> {
                try {
                    if (threadId % 2 == 0) {
                        // Even threads do tick operations
                        for (int j = 0; j < 100; j++) {
                            clock.tick();
                            tickCount.incrementAndGet();
                        }
                    } else {
                        // Odd threads do update operations
                        for (int j = 0; j < 100; j++) {
                            clock.update(threadId * 10 + j);
                            updateCount.incrementAndGet();
                        }
                    }
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS), "All threads should complete within timeout");

        assertEquals(1000, tickCount.get(), "Should have performed expected tick operations");
        assertEquals(1000, updateCount.get(), "Should have performed expected update operations");

        // Clock should have progressed
        assertTrue(clock.getTime() > 0, "Clock should have progressed");
    }

    @RepeatedTest(5)
    void testThreadSafetyUnderStress() throws InterruptedException {
        final int numThreads = 50;
        final int operationsPerThread = 200;
        final CountDownLatch latch = new CountDownLatch(numThreads);
        final ConcurrentLinkedQueue<Long> allTimes = new ConcurrentLinkedQueue<>();

        for (int i = 0; i < numThreads; i++) {
            new Thread(() -> {
                try {
                    for (int j = 0; j < operationsPerThread; j++) {
                        if (j % 3 == 0) {
                            allTimes.add(clock.tick());
                        } else if (j % 3 == 1) {
                            allTimes.add(clock.update(j * 2));
                        } else {
                            allTimes.add(clock.getTime());
                        }
                    }
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        assertTrue(latch.await(15, TimeUnit.SECONDS), "All threads should complete within timeout");

        // Verify no negative times and clock has progressed
        for (Long time : allTimes) {
            assertNotNull(time, "Time should not be null");
            assertTrue(time >= 0, "Time should be non-negative");
        }

        assertTrue(clock.getTime() > 0, "Clock should have progressed");
    }

    @Test
    void testLamportClockAlgorithmCorrectness() {
        LamportClock clock1 = new LamportClock();
        LamportClock clock2 = new LamportClock();

        // Simulate distributed system scenario
        clock1.tick(); // clock1 = 1
        clock1.tick(); // clock1 = 2

        clock2.tick(); // clock2 = 1

        // clock1 sends message with timestamp 2 to clock2
        long clock2Time = clock2.update(2); // clock2 = max(1, 2) + 1 = 3
        assertEquals(3, clock2Time, "Clock2 should update correctly");

        // clock2 sends message with timestamp 3 back to clock1
        long clock1Time = clock1.update(3); // clock1 = max(2, 3) + 1 = 4
        assertEquals(4, clock1Time, "Clock1 should update correctly");

        // Both clocks should maintain proper ordering
        assertTrue(clock1.getTime() > 2, "Clock1 should have progressed beyond its initial state");
        assertTrue(clock2.getTime() > 1, "Clock2 should have progressed beyond its initial state");
    }

    @Test
    void testToStringMethod() {
        clock.tick(); // time = 1
        String result = clock.toString();

        assertTrue(result.contains("LamportClock"), "toString should contain class name");
        assertTrue(result.contains("time=1"), "toString should contain current time");
    }

    @Test
    void testZeroReceivedTime() {
        clock.tick(); // time = 1

        long result = clock.update(0);
        assertEquals(2, result, "Update with 0 should increment current time");
    }

    @Test
    void testLargeTimestampValues() {
        long largeTimestamp = Long.MAX_VALUE - 10;

        // This should not overflow
        assertDoesNotThrow(() -> {
            clock.update(largeTimestamp);
        }, "Should handle large timestamp values without overflow");
    }

    @Test
    void testConsistencyAfterManyOperations() {
        final int operations = 10000;
        long lastTime = 0;

        // Perform many sequential operations
        for (int i = 0; i < operations; i++) {
            long currentTime;
            if (i % 2 == 0) {
                currentTime = clock.tick();
            } else {
                currentTime = clock.update(i);
            }

            assertTrue(currentTime > lastTime,
                "Time should always increase: iteration " + i +
                ", last=" + lastTime + ", current=" + currentTime);
            lastTime = currentTime;
        }

        assertEquals(lastTime, clock.getTime(), "Final getTime should match last operation result");
    }
}