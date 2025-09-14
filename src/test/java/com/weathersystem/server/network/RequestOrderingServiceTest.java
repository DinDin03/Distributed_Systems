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

    @Test
    @Timeout(5)
    void testRequestOrdering() throws InterruptedException {
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
    }

    @Test
    @Timeout(5)
    void testConcurrentRequestSubmission() throws InterruptedException {
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
    }

    @Test
    @Timeout(5)
    void testRequestOrderingWithSameLamportTime() throws InterruptedException {
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
    }

    @Test
    @Timeout(5)
    void testRequestOrderingWithNegativeLamportTime() throws InterruptedException {
        orderingService.start();
        
        // Submit requests with negative Lamport time
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
    }

    @Test
    @Timeout(5)
    void testRequestOrderingWithLargeLamportTime() throws InterruptedException {
        orderingService.start();
        
        // Submit requests with large Lamport times
        TimestampedRequest request1 = createMockRequest("PUT", Long.MAX_VALUE);
        TimestampedRequest request2 = createMockRequest("GET", Long.MAX_VALUE - 1);
        TimestampedRequest request3 = createMockRequest("PUT", Long.MAX_VALUE - 2);
        
        orderingService.submitRequest(request1);
        orderingService.submitRequest(request2);
        orderingService.submitRequest(request3);
        
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
    }

    @Test
    @Timeout(5)
    void testRequestOrderingWithZeroLamportTime() throws InterruptedException {
        orderingService.start();
        
        // Submit requests with zero Lamport time
        TimestampedRequest request1 = createMockRequest("PUT", 0L);
        TimestampedRequest request2 = createMockRequest("GET", 0L);
        TimestampedRequest request3 = createMockRequest("PUT", 0L);
        
        orderingService.submitRequest(request1);
        orderingService.submitRequest(request2);
        orderingService.submitRequest(request3);
        
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
    }

    @Test
    @Timeout(5)
    void testRequestOrderingWithMixedMethods() throws InterruptedException {
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
    }

    @Test
    @Timeout(5)
    void testRequestOrderingWithDuplicateLamportTimes() throws InterruptedException {
        orderingService.start();
        
        // Submit requests with duplicate Lamport times
        TimestampedRequest request1 = createMockRequest("PUT", 10L);
        TimestampedRequest request2 = createMockRequest("GET", 10L);
        TimestampedRequest request3 = createMockRequest("PUT", 10L);
        TimestampedRequest request4 = createMockRequest("GET", 10L);
        
        orderingService.submitRequest(request1);
        orderingService.submitRequest(request2);
        orderingService.submitRequest(request3);
        orderingService.submitRequest(request4);
        
        // Wait for processing
        Thread.sleep(1000);
        
        orderingService.stop();
        
        // Verify all requests were processed
        synchronized (processedRequests) {
            assertEquals(4, processedRequests.size(), "All requests should be processed");
            for (TimestampedRequest request : processedRequests) {
                assertEquals(10L, request.getLamportTime(), "All requests should have same Lamport time");
            }
        }
    }

    @Test
    @Timeout(5)
    void testRequestOrderingWithRapidSubmission() throws InterruptedException {
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
    }

    @Test
    @Timeout(5)
    void testRequestOrderingWithGapsInLamportTime() throws InterruptedException {
        orderingService.start();
        
        // Submit requests with gaps in Lamport time
        TimestampedRequest request1 = createMockRequest("PUT", 1L);
        TimestampedRequest request2 = createMockRequest("GET", 10L);
        TimestampedRequest request3 = createMockRequest("PUT", 100L);
        TimestampedRequest request4 = createMockRequest("GET", 1000L);
        
        orderingService.submitRequest(request1);
        orderingService.submitRequest(request2);
        orderingService.submitRequest(request3);
        orderingService.submitRequest(request4);
        
        // Wait for processing
        Thread.sleep(1000);
        
        orderingService.stop();
        
        // Verify requests were processed in order
        synchronized (processedRequests) {
            assertEquals(4, processedRequests.size(), "All requests should be processed");
            assertEquals(1L, processedRequests.get(0).getLamportTime(), "First request should have lowest time");
            assertEquals(10L, processedRequests.get(1).getLamportTime(), "Second request should have second lowest time");
            assertEquals(100L, processedRequests.get(2).getLamportTime(), "Third request should have third lowest time");
            assertEquals(1000L, processedRequests.get(3).getLamportTime(), "Fourth request should have highest time");
        }
    }

    @Test
    @Timeout(5)
    void testRequestOrderingWithNegativeAndPositiveTimes() throws InterruptedException {
        orderingService.start();
        
        // Submit requests with both negative and positive Lamport times
        TimestampedRequest request1 = createMockRequest("PUT", -5L);
        TimestampedRequest request2 = createMockRequest("GET", 0L);
        TimestampedRequest request3 = createMockRequest("PUT", 5L);
        TimestampedRequest request4 = createMockRequest("GET", -10L);
        
        orderingService.submitRequest(request1);
        orderingService.submitRequest(request2);
        orderingService.submitRequest(request3);
        orderingService.submitRequest(request4);
        
        // Wait for processing
        Thread.sleep(1000);
        
        orderingService.stop();
        
        // Verify requests were processed in order
        synchronized (processedRequests) {
            assertEquals(4, processedRequests.size(), "All requests should be processed");
            assertEquals(-10L, processedRequests.get(0).getLamportTime(), "First request should have most negative time");
            assertEquals(-5L, processedRequests.get(1).getLamportTime(), "Second request should have second most negative time");
            assertEquals(0L, processedRequests.get(2).getLamportTime(), "Third request should have zero time");
            assertEquals(5L, processedRequests.get(3).getLamportTime(), "Fourth request should have positive time");
        }
    }

    @Test
    @Timeout(5)
    void testRequestOrderingWithVerySmallDifferences() throws InterruptedException {
        orderingService.start();
        
        // Submit requests with very small differences in Lamport time
        TimestampedRequest request1 = createMockRequest("PUT", 1L);
        TimestampedRequest request2 = createMockRequest("GET", 2L);
        TimestampedRequest request3 = createMockRequest("PUT", 3L);
        TimestampedRequest request4 = createMockRequest("GET", 4L);
        
        orderingService.submitRequest(request1);
        orderingService.submitRequest(request2);
        orderingService.submitRequest(request3);
        orderingService.submitRequest(request4);
        
        // Wait for processing
        Thread.sleep(1000);
        
        orderingService.stop();
        
        // Verify requests were processed in order
        synchronized (processedRequests) {
            assertEquals(4, processedRequests.size(), "All requests should be processed");
            assertEquals(1L, processedRequests.get(0).getLamportTime(), "First request should have lowest time");
            assertEquals(2L, processedRequests.get(1).getLamportTime(), "Second request should have second lowest time");
            assertEquals(3L, processedRequests.get(2).getLamportTime(), "Third request should have third lowest time");
            assertEquals(4L, processedRequests.get(3).getLamportTime(), "Fourth request should have highest time");
        }
    }

    @Test
    @Timeout(5)
    void testRequestOrderingWithInterleavedSubmission() throws InterruptedException {
        orderingService.start();
        
        // Submit requests in interleaved order
        TimestampedRequest request1 = createMockRequest("PUT", 1L);
        TimestampedRequest request2 = createMockRequest("GET", 3L);
        TimestampedRequest request3 = createMockRequest("PUT", 2L);
        TimestampedRequest request4 = createMockRequest("GET", 4L);
        
        orderingService.submitRequest(request1);
        Thread.sleep(10); // Small delay
        orderingService.submitRequest(request2);
        Thread.sleep(10); // Small delay
        orderingService.submitRequest(request3);
        Thread.sleep(10); // Small delay
        orderingService.submitRequest(request4);
        
        // Wait for processing
        Thread.sleep(1000);
        
        orderingService.stop();
        
        // Verify requests were processed in Lamport time order
        synchronized (processedRequests) {
            assertEquals(4, processedRequests.size(), "All requests should be processed");
            assertEquals(1L, processedRequests.get(0).getLamportTime(), "First request should have lowest time");
            assertEquals(2L, processedRequests.get(1).getLamportTime(), "Second request should have second lowest time");
            assertEquals(3L, processedRequests.get(2).getLamportTime(), "Third request should have third lowest time");
            assertEquals(4L, processedRequests.get(3).getLamportTime(), "Fourth request should have highest time");
        }
    }

    @Test
    @Timeout(5)
    void testRequestOrderingWithEmptyQueue() throws InterruptedException {
        orderingService.start();
        
        // Don't submit any requests
        Thread.sleep(1000);
        
        orderingService.stop();
        
        // Verify no requests were processed
        synchronized (processedRequests) {
            assertEquals(0, processedRequests.size(), "No requests should be processed");
        }
    }

    @Test
    @Timeout(5)
    void testRequestOrderingWithSingleRequest() throws InterruptedException {
        orderingService.start();
        
        // Submit single request
        TimestampedRequest request = createMockRequest("PUT", 5L);
        orderingService.submitRequest(request);
        
        // Wait for processing
        Thread.sleep(1000);
        
        orderingService.stop();
        
        // Verify single request was processed
        synchronized (processedRequests) {
            assertEquals(1, processedRequests.size(), "Single request should be processed");
            assertEquals(5L, processedRequests.get(0).getLamportTime(), "Request should have correct Lamport time");
            assertEquals("PUT", processedRequests.get(0).getMethod(), "Request should have correct method");
        }
    }
}
