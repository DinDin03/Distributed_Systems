package com.weathersystem.server.network;

import com.weathersystem.server.services.RequestOrderingService;
import com.weathersystem.shared.clock.LamportClock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for RequestOrderingService class.
 * Tests Lamport timestamp ordering, concurrent processing, and various edge cases.
 */
class RequestOrderingServiceTest {

    private RequestOrderingService orderingService;
    private List<TimestampedRequest> processedRequests;
    private LamportClock lamportClock;

    @BeforeEach
    void setUp() {
        lamportClock = new LamportClock();
        processedRequests = new ArrayList<>();
        
        // Create ordering service with a processor that records requests
        orderingService = new RequestOrderingService(request -> {
            synchronized (processedRequests) {
                processedRequests.add(request);
            }
        });
    }

    private TimestampedRequest createMockRequest(String method, long lamportTime) {
        // Create mock socket and streams
        Socket mockSocket = new Socket();
        BufferedReader mockReader = new BufferedReader(new StringReader(""));
        PrintWriter mockWriter = new PrintWriter(new StringWriter());
        
        return new TimestampedRequest(mockSocket, method, 0, lamportTime, mockReader, mockWriter);
    }

    // === CORE FUNCTIONALITY TESTS ===

    @Test
    @Timeout(5)
    void testBasicRequestOrdering() throws InterruptedException {
        System.out.println("Testing basic request ordering...");
        
        orderingService.start();
        
        // Submit requests in reverse order
        TimestampedRequest request1 = createMockRequest("PUT", 10L);
        TimestampedRequest request2 = createMockRequest("GET", 5L);
        TimestampedRequest request3 = createMockRequest("PUT", 15L);
        TimestampedRequest request4 = createMockRequest("GET", 3L);
        
        orderingService.submitRequest(request1);
        orderingService.submitRequest(request2);
        orderingService.submitRequest(request3);
        orderingService.submitRequest(request4);
        
        // Wait for all requests to be processed
        Thread.sleep(1000);
        
        orderingService.stop();
        
        // Verify requests were processed in Lamport time order
        synchronized (processedRequests) {
            assertEquals(4, processedRequests.size(), "All requests should be processed");
            assertEquals(3L, processedRequests.get(0).getLamportTime(), "First request should have lowest time");
            assertEquals(5L, processedRequests.get(1).getLamportTime(), "Second request should have second lowest time");
            assertEquals(10L, processedRequests.get(2).getLamportTime(), "Third request should have third lowest time");
            assertEquals(15L, processedRequests.get(3).getLamportTime(), "Fourth request should have highest time");
        }
        
        System.out.println("✓ Basic request ordering test passed");
    }

    @Test
    @Timeout(5)
    void testConcurrentRequestSubmission() throws InterruptedException {
        System.out.println("Testing concurrent request submission...");
        
        orderingService.start();
        
        final int numRequests = 20;
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch completionLatch = new CountDownLatch(numRequests);
        final AtomicInteger submittedCount = new AtomicInteger(0);
        
        // Submit requests concurrently
        for (int i = 0; i < numRequests; i++) {
            final int requestId = i;
            Thread thread = new Thread(() -> {
                try {
                    startLatch.await(); // Wait for start signal
                    
                    TimestampedRequest request = createMockRequest("PUT", requestId * 2L);
                    orderingService.submitRequest(request);
                    submittedCount.incrementAndGet();
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    completionLatch.countDown();
                }
            });
            thread.start();
        }
        
        // Start all threads simultaneously
        startLatch.countDown();
        
        // Wait for all submissions to complete
        assertTrue(completionLatch.await(3, TimeUnit.SECONDS), "All requests should be submitted within timeout");
        
        // Wait for processing to complete
        Thread.sleep(1000);
        
        orderingService.stop();
        
        // Verify all requests were submitted and processed
        assertEquals(numRequests, submittedCount.get(), "All requests should be submitted");
        synchronized (processedRequests) {
            assertEquals(numRequests, processedRequests.size(), "All requests should be processed");
        }
        
        System.out.println("✓ Concurrent request submission test passed");
    }

    // === EDGE CASES TESTS ===

    @Test
    @Timeout(5)
    void testSpecialLamportTimes() throws InterruptedException {
        System.out.println("Testing special Lamport time values...");
        
        orderingService.start();
        
        // Test negative Lamport times
        TimestampedRequest request1 = createMockRequest("PUT", -5L);
        TimestampedRequest request2 = createMockRequest("GET", -10L);
        TimestampedRequest request3 = createMockRequest("PUT", -1L);
        
        orderingService.submitRequest(request1);
        orderingService.submitRequest(request2);
        orderingService.submitRequest(request3);
        
        // Wait for processing
        Thread.sleep(1000);
        
        orderingService.stop();
        
        // Verify requests were processed in order (most negative first)
        synchronized (processedRequests) {
            assertEquals(3, processedRequests.size(), "All requests should be processed");
            assertEquals(-10L, processedRequests.get(0).getLamportTime(), "First request should have most negative time");
            assertEquals(-5L, processedRequests.get(1).getLamportTime(), "Second request should have second most negative time");
            assertEquals(-1L, processedRequests.get(2).getLamportTime(), "Third request should have least negative time");
        }
        
        // Reset for zero times test
        processedRequests.clear();
        orderingService = new RequestOrderingService(request -> {
            synchronized (processedRequests) {
                processedRequests.add(request);
            }
        });
        
        orderingService.start();
        
        // Test zero Lamport times
        TimestampedRequest request4 = createMockRequest("PUT", 0L);
        TimestampedRequest request5 = createMockRequest("GET", 0L);
        TimestampedRequest request6 = createMockRequest("PUT", 0L);
        
        orderingService.submitRequest(request4);
        orderingService.submitRequest(request5);
        orderingService.submitRequest(request6);
        
        // Wait for processing
        Thread.sleep(1000);
        
        orderingService.stop();
        
        // Verify all requests were processed
        synchronized (processedRequests) {
            assertEquals(3, processedRequests.size(), "All requests should be processed");
            for (TimestampedRequest request : processedRequests) {
                assertEquals(0L, request.getLamportTime(), "All requests should have zero Lamport time");
            }
        }
        
        // Reset for large times test
        processedRequests.clear();
        orderingService = new RequestOrderingService(request -> {
            synchronized (processedRequests) {
                processedRequests.add(request);
            }
        });
        
        orderingService.start();
        
        // Test large Lamport times
        TimestampedRequest request7 = createMockRequest("PUT", Long.MAX_VALUE);
        TimestampedRequest request8 = createMockRequest("GET", Long.MAX_VALUE - 1);
        TimestampedRequest request9 = createMockRequest("PUT", Long.MAX_VALUE - 2);
        
        orderingService.submitRequest(request7);
        orderingService.submitRequest(request8);
        orderingService.submitRequest(request9);
        
        // Wait for processing
        Thread.sleep(1000);
        
        orderingService.stop();
        
        // Verify requests were processed in order
        synchronized (processedRequests) {
            assertEquals(3, processedRequests.size(), "All requests should be processed");
            assertEquals(Long.MAX_VALUE - 2, processedRequests.get(0).getLamportTime(), "First request should have smallest time");
            assertEquals(Long.MAX_VALUE - 1, processedRequests.get(1).getLamportTime(), "Second request should have middle time");
            assertEquals(Long.MAX_VALUE, processedRequests.get(2).getLamportTime(), "Third request should have largest time");
        }
        
        System.out.println("✓ Special Lamport times test passed");
    }

    @Test
    @Timeout(5)
    void testDuplicateLamportTimes() throws InterruptedException {
        System.out.println("Testing duplicate Lamport times...");
        
        orderingService.start();
        
        // Submit requests with same Lamport time
        TimestampedRequest request1 = createMockRequest("PUT", 5L);
        TimestampedRequest request2 = createMockRequest("GET", 5L);
        TimestampedRequest request3 = createMockRequest("PUT", 5L);
        
        orderingService.submitRequest(request1);
        orderingService.submitRequest(request2);
        orderingService.submitRequest(request3);
        
        // Wait for processing
        Thread.sleep(1000);
        
        orderingService.stop();
        
        // Verify all requests were processed (order may vary for same timestamp)
        synchronized (processedRequests) {
            assertEquals(3, processedRequests.size(), "All requests should be processed");
            for (TimestampedRequest request : processedRequests) {
                assertEquals(5L, request.getLamportTime(), "All requests should have same Lamport time");
            }
        }
        
        System.out.println("✓ Duplicate Lamport times test passed");
    }

    @Test
    @Timeout(5)
    void testMixedMethodsAndOrdering() throws InterruptedException {
        System.out.println("Testing mixed methods and ordering...");
        
        orderingService.start();
        
        // Submit requests with different methods but ordered by Lamport time
        TimestampedRequest request1 = createMockRequest("GET", 1L);
        TimestampedRequest request2 = createMockRequest("PUT", 2L);
        TimestampedRequest request3 = createMockRequest("GET", 3L);
        TimestampedRequest request4 = createMockRequest("PUT", 4L);
        
        // Submit in reverse order
        orderingService.submitRequest(request4);
        orderingService.submitRequest(request2);
        orderingService.submitRequest(request1);
        orderingService.submitRequest(request3);
        
        // Wait for processing
        Thread.sleep(1000);
        
        orderingService.stop();
        
        // Verify requests were processed in Lamport time order regardless of method
        synchronized (processedRequests) {
            assertEquals(4, processedRequests.size(), "All requests should be processed");
            assertEquals(1L, processedRequests.get(0).getLamportTime(), "First request should have lowest time");
            assertEquals("GET", processedRequests.get(0).getMethod(), "First request should be GET");
            assertEquals(2L, processedRequests.get(1).getLamportTime(), "Second request should have second lowest time");
            assertEquals("PUT", processedRequests.get(1).getMethod(), "Second request should be PUT");
            assertEquals(3L, processedRequests.get(2).getLamportTime(), "Third request should have third lowest time");
            assertEquals("GET", processedRequests.get(2).getMethod(), "Third request should be GET");
            assertEquals(4L, processedRequests.get(3).getLamportTime(), "Fourth request should have highest time");
            assertEquals("PUT", processedRequests.get(3).getMethod(), "Fourth request should be PUT");
        }
        
        System.out.println("✓ Mixed methods and ordering test passed");
    }

    // === INTEGRATION TESTS ===

    @Test
    @Timeout(5)
    void testRapidSubmissionAndOrdering() throws InterruptedException {
        System.out.println("Testing rapid submission and ordering...");
        
        orderingService.start();
        
        // Submit requests rapidly
        for (int i = 0; i < 50; i++) {
            TimestampedRequest request = createMockRequest("PUT", i * 2L);
            orderingService.submitRequest(request);
        }
        
        // Wait for processing
        Thread.sleep(2000);
        
        orderingService.stop();
        
        // Verify all requests were processed in order
        synchronized (processedRequests) {
            assertEquals(50, processedRequests.size(), "All requests should be processed");
            for (int i = 0; i < processedRequests.size(); i++) {
                assertEquals(i * 2L, processedRequests.get(i).getLamportTime(), 
                           "Request " + i + " should have correct Lamport time");
            }
        }
        
        System.out.println("✓ Rapid submission and ordering test passed");
    }

    @Test
    @Timeout(5)
    void testEdgeCaseScenarios() throws InterruptedException {
        System.out.println("Testing edge case scenarios...");
        
        orderingService.start();
        
        // Test empty queue
        Thread.sleep(500);
        orderingService.stop();
        
        synchronized (processedRequests) {
            assertEquals(0, processedRequests.size(), "No requests should be processed");
        }
        
        // Reset for single request test
        processedRequests.clear();
        orderingService = new RequestOrderingService(request -> {
            synchronized (processedRequests) {
                processedRequests.add(request);
            }
        });
        
        orderingService.start();
        
        // Test single request
        TimestampedRequest request = createMockRequest("PUT", 5L);
        orderingService.submitRequest(request);
        
        Thread.sleep(1000);
        orderingService.stop();
        
        synchronized (processedRequests) {
            assertEquals(1, processedRequests.size(), "Single request should be processed");
            assertEquals(5L, processedRequests.get(0).getLamportTime(), "Request should have correct Lamport time");
            assertEquals("PUT", processedRequests.get(0).getMethod(), "Request should have correct method");
        }
        
        System.out.println("✓ Edge case scenarios test passed");
    }

    @Test
    @Timeout(5)
    void testServiceLifecycleManagement() throws InterruptedException {
        System.out.println("Testing service lifecycle management...");
        
        // Test that service can be started and stopped
        assertFalse(orderingService.isRunning());

        orderingService.start();
        assertTrue(orderingService.isRunning());

        // Starting again should be safe (no-op)
        orderingService.start();
        assertTrue(orderingService.isRunning());

        TimestampedRequest request = createMockRequest("PUT", 5L);
        orderingService.submitRequest(request);

        orderingService.stop();
        assertFalse(orderingService.isRunning());

        // Stopping again should be safe (no-op)
        orderingService.stop();
        assertFalse(orderingService.isRunning());

        // Should not be able to restart after shutdown - should throw exception
        assertThrows(IllegalStateException.class, () -> orderingService.start());

        synchronized (processedRequests) {
            assertEquals(1, processedRequests.size(), "Request should be processed");
        }
        
        System.out.println("✓ Service lifecycle management test passed");
    }

    @Test
    @Timeout(5)
    void testQueueManagement() {
        System.out.println("Testing queue management...");
        
        // Test queue size tracking
        assertEquals(0, orderingService.getQueueSize(), "Queue should start empty");

        TimestampedRequest request1 = createMockRequest("PUT", 1L);
        TimestampedRequest request2 = createMockRequest("GET", 2L);

        orderingService.submitRequest(request1);
        assertEquals(1, orderingService.getQueueSize(), "Queue size should be 1 after first request");

        orderingService.submitRequest(request2);
        assertEquals(2, orderingService.getQueueSize(), "Queue size should be 2 after second request");

        orderingService.start();
        orderingService.stop();

        // After processing, queue should be empty
        assertEquals(0, orderingService.getQueueSize(), "Queue should be empty after processing");
        
        System.out.println("✓ Queue management test passed");
    }

    @Test
    @Timeout(5)
    void testErrorHandling() throws InterruptedException {
        System.out.println("Testing error handling...");
        
        // Create service with processor that throws exception on first request
        List<TimestampedRequest> processedList = new ArrayList<>();
        AtomicInteger processCount = new AtomicInteger(0);

        RequestOrderingService exceptionService = new RequestOrderingService(request -> {
            int count = processCount.incrementAndGet();
            if (count == 1) {
                throw new RuntimeException("Test exception");
            }
            synchronized (processedList) {
                processedList.add(request);
            }
        });

        exceptionService.start();

        // Submit multiple requests
        TimestampedRequest request1 = createMockRequest("PUT", 1L);
        TimestampedRequest request2 = createMockRequest("GET", 2L);
        TimestampedRequest request3 = createMockRequest("PUT", 3L);

        exceptionService.submitRequest(request1);
        exceptionService.submitRequest(request2);
        exceptionService.submitRequest(request3);

        exceptionService.stop();

        // Should have processed 2 requests despite exception on first
        synchronized (processedList) {
            assertEquals(2, processedList.size(), "Should process remaining requests after exception");
            assertEquals(2L, processedList.get(0).getLamportTime(), "Second request should be processed");
            assertEquals(3L, processedList.get(1).getLamportTime(), "Third request should be processed");
        }
        
        System.out.println("✓ Error handling test passed");
    }
}
