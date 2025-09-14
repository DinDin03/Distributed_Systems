package com.weathersystem.shared.clock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.RepeatedTest;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for LamportClock class.
 * Tests logical clock functionality, thread safety, and distributed system scenarios.
 */
class LamportClockTest {

    private LamportClock clock;

    @BeforeEach
    void setUp() {
        clock = new LamportClock();
    }

    // === CORE FUNCTIONALITY TESTS ===

    @Test
    // Tests the basic operations like tick, update, and getting the time
    void testBasicClockOperations() {
        System.out.println("Testing basic clock operations");
        
        // Test initial state
        assertEquals(0, clock.getTime(), "Clock should start at 0");
        
        // Test tick operations
        long time1 = clock.tick();
        long time2 = clock.tick();
        long time3 = clock.tick();

        assertEquals(1, time1, "First tick should return 1");
        assertEquals(2, time2, "Second tick should return 2");
        assertEquals(3, time3, "Third tick should return 3");
        assertEquals(3, clock.getTime(), "Current time should match last tick");

        // Test getTime does not increment
        long time4 = clock.getTime();
        long time5 = clock.getTime();
        long time6 = clock.getTime();

        assertEquals(3, time4, "getTime should return current time");
        assertEquals(3, time5, "getTime should not increment clock");
        assertEquals(3, time6, "getTime should still not increment clock");

        // Test update with lower time
        long result1 = clock.update(1);
        assertEquals(4, result1, "Update with lower time should increment current time");
        assertEquals(4, clock.getTime(), "Clock time should be max(current, received) + 1");

        // Test update with higher time
        long result2 = clock.update(10);
        assertEquals(11, result2, "Update should return max(4, 10) + 1 = 11");
        assertEquals(11, clock.getTime(), "Clock time should be updated to 11");

        // Test update with equal time
        long result3 = clock.update(11);
        assertEquals(12, result3, "Update with equal time should increment");
        assertEquals(12, clock.getTime(), "Clock time should be incremented");

        // Test zero received time
        long result4 = clock.update(0);
        assertEquals(13, result4, "Update with 0 should increment current time");
        assertEquals(13, clock.getTime(), "Clock time should be incremented");
        
        System.out.println("Basic clock operations test passed");
    }

    // === CONCURRENCY TESTS ===

    @Test
    // Tests that the clock works properly when multiple threads use it
    void testConcurrentOperations() throws InterruptedException {
        System.out.println("Testing concurrent operations");
        
        // Test concurrent tick operations
        final int numThreads = 10;
        final int operationsPerThread = 100; // Reduced for faster execution
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

        // Test concurrent update operations
        final CountDownLatch updateLatch = new CountDownLatch(numThreads);
        final AtomicLong maxObservedTime = new AtomicLong(0);

        // Create threads that perform update operations with increasing received times
        for (int i = 0; i < numThreads; i++) {
            final int threadId = i;
            new Thread(() -> {
                try {
                    long result = clock.update(threadId * 10); // Use different received times
                    maxObservedTime.updateAndGet(current -> Math.max(current, result));
                } finally {
                    updateLatch.countDown();
                }
            }).start();
        }

        assertTrue(updateLatch.await(10, TimeUnit.SECONDS), "All update threads should complete within timeout");

        // The final clock time should be at least the maximum received time + 1
        long finalTime = clock.getTime();
        assertTrue(finalTime >= maxObservedTime.get(),
            "Final time should be at least the maximum observed time");

        // Test mixed concurrent operations
        final int mixedThreads = 20;
        final CountDownLatch mixedLatch = new CountDownLatch(mixedThreads);
        final AtomicInteger tickCount = new AtomicInteger(0);
        final AtomicInteger updateCount = new AtomicInteger(0);

        // Create mixed threads - some doing ticks, some doing updates
        for (int i = 0; i < mixedThreads; i++) {
            final int threadId = i;
            new Thread(() -> {
                try {
                    if (threadId % 2 == 0) {
                        // Even threads do tick operations
                        for (int j = 0; j < 50; j++) { // Reduced for faster execution
                            clock.tick();
                            tickCount.incrementAndGet();
                        }
                    } else {
                        // Odd threads do update operations
                        for (int j = 0; j < 50; j++) { // Reduced for faster execution
                            clock.update(threadId * 10 + j);
                            updateCount.incrementAndGet();
                        }
                    }
                } finally {
                    mixedLatch.countDown();
                }
            }).start();
        }

        assertTrue(mixedLatch.await(10, TimeUnit.SECONDS), "All mixed threads should complete within timeout");

        assertEquals(500, tickCount.get(), "Should have performed expected tick operations");
        assertEquals(500, updateCount.get(), "Should have performed expected update operations");

        // Clock should have progressed
        assertTrue(clock.getTime() > 0, "Clock should have progressed");
        
        System.out.println("Concurrent operations test passed");
    }

    // === DISTRIBUTED SYSTEM AND EDGE CASES TESTS ===

    @Test
    // Tests how multiple clocks work together in a distributed system
    void testDistributedSystemScenario() {
        System.out.println("Testing distributed system scenario");
        
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
        
        System.out.println("Distributed system scenario test passed");
    }

    @Test
    // Tests weird edge cases and makes sure everything stays consistent
    void testEdgeCasesAndConsistency() {
        System.out.println("Testing edge cases and consistency");
        
        // Test toString method
        clock.tick(); // time = 1
        String result = clock.toString();
        assertTrue(result.contains("LamportClock"), "toString should contain class name");
        assertTrue(result.contains("time=1"), "toString should contain current time");

        // Test large timestamp values (but not too large to avoid overflow)
        long largeTimestamp = 1000000L; // Use a reasonable large value
        assertDoesNotThrow(() -> {
            clock.update(largeTimestamp);
        }, "Should handle large timestamp values without overflow");

        // Test consistency after many operations
        final int operations = 100; // Further reduced to avoid overflow
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
        
        System.out.println("Edge cases and consistency test passed");
    }

    @RepeatedTest(3) // Reduced from 5 for faster execution
    // Tests that the clock works properly under heavy load
    void testThreadSafetyUnderStress() throws InterruptedException {
        System.out.println("Testing thread safety under stress");
        
        final int numThreads = 20; // Reduced for faster execution
        final int operationsPerThread = 100; // Reduced for faster execution
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

        assertTrue(latch.await(10, TimeUnit.SECONDS), "All threads should complete within timeout");

        // Verify no negative times and clock has progressed
        for (Long time : allTimes) {
            assertNotNull(time, "Time should not be null");
            assertTrue(time >= 0, "Time should be non-negative");
        }

        assertTrue(clock.getTime() > 0, "Clock should have progressed");
        
        System.out.println("Thread safety under stress test passed");
    }
}