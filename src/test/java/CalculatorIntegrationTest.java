import org.junit.jupiter.api.*;
import java.rmi.registry.Registry;
import java.time.LocalDate;
import java.rmi.registry.LocateRegistry;
import java.rmi.RemoteException;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Random;

//Integration tests for the Calculator RMI service
public class CalculatorIntegrationTest {
    private static Registry registry;
    private static Thread serverThread;
    private static final int TEST_RMI_PORT = 1099;

    //Start the RMI server before running tests
    @BeforeAll
    static void startServer() throws Exception{
        serverThread = new Thread(() -> {
            try{
                Calculator calculator = new CalculatorImplementation();

                registry = LocateRegistry.createRegistry(TEST_RMI_PORT);

                registry.bind("Calculator", calculator);

                System.out.println("Integration test server started on port " + TEST_RMI_PORT);
            } catch (Exception e) {
                System.err.println("Failed to start test server " + e.getMessage());
            }
        });

        serverThread.start();
        Thread.sleep(2000);
        System.out.println("Server startup completed");
        
    }

    //Stop the RMI server after running tests
    @AfterAll
    static void stopServer() throws Exception{
        System.out.println("Stopping integration test server");

        try{
            if(registry != null){
                registry.unbind("Calculator");
                System.out.println("Calculator service unbound from the registry");
            }
        }catch(Exception e){
            System.out.println("Error unbinding service " + e.getMessage());
        }

        try{
            if(serverThread != null && serverThread.isAlive()){
                serverThread.interrupt();
                serverThread.join(500);

                if (serverThread.isAlive()) {
                    System.err.println("Server thread did not stop");
                } else {
                    System.out.println("Server thread stopped successfully");
                }
            }
        }catch(Exception e){
            System.out.println("Error stopping server thread " + e.getMessage());
            Thread.currentThread().interrupt();
        }

        System.out.println("Server shutdown completed");
    }

    //Test per client stack isolation
    @Test
    @DisplayName("per client stack test")
    void testPerClientStackIsolation() throws Exception {
        System.out.println("Testing per client stack ");

        Registry registry1 = LocateRegistry.getRegistry("localhost", TEST_RMI_PORT);
        Calculator client1 = (Calculator) registry1.lookup("Calculator");

        String session1 = client1.createSession();
        client1.setSession(session1);
        System.out.println("Client 1 session - " + session1);

        assertTrue(client1.isEmpty(), "Client 1 should start empty");
        client1.pushValue(10);
        client1.pushValue(20);
        assertFalse(client1.isEmpty(), "Client 1 should have values");

        assertEquals(20, client1.pop(), "Client 1 should get 20");
        assertEquals(10, client1.pop(), "Client 1 should get 10");
        assertTrue(client1.isEmpty(), "Client 1 should be empty");

        System.out.println("Client 1 test completed successfully");

        Registry registry2 = LocateRegistry.getRegistry("localhost", TEST_RMI_PORT);
        Calculator client2 = (Calculator) registry2.lookup("Calculator");

        String session2 = client2.createSession();
        client2.setSession(session2);
        System.out.println("Client 2 session - " + session2);

        assertNotEquals(session1, session2, "Each client should have unique session");

        assertTrue(client2.isEmpty(), "Client 2 should start empty");
        client2.pushValue(100);
        client2.pushValue(200);
        assertFalse(client2.isEmpty(), "Client 2 should have values");

        assertEquals(200, client2.pop(), "Client 2 should get 200");
        assertEquals(100, client2.pop(), "Client 2 should get 100");
        assertTrue(client2.isEmpty(), "Client 2 should be empty");

        System.out.println("Client 2 test completed successfully");

        assertTrue(client1.isEmpty(), "Client 1 should still be empty");

        assertTrue(client2.isEmpty(), "Client 2 should be empty");

        System.out.println("Per client stack test completed successfully");
    }

    //Test all calculator operations
    @Test
    @DisplayName("Should test all calculator operations")
    void testAllCalculatorOperations() throws Exception {
        Registry registry = LocateRegistry.getRegistry("localhost", TEST_RMI_PORT);
        Calculator calc = (Calculator) registry.lookup("Calculator");

        String sessionId = calc.createSession();
        calc.setSession(sessionId);

        calc.pushValue(12);
        calc.pushValue(18);
        calc.pushValue(24);
        calc.pushOperation("gcd");
        assertEquals(6, calc.pop(), "GCD of 12, 18, 24 should be 6");

        calc.pushValue(4);
        calc.pushValue(6);
        calc.pushOperation("lcm");
        assertEquals(12, calc.pop(), "LCM of 4, 6 should be 12");

        calc.pushValue(15);
        calc.pushValue(5);
        calc.pushValue(25);
        calc.pushOperation("min");
        assertEquals(5, calc.pop(), "Min of 15, 5, 25 should be 5");

        calc.pushValue(15);
        calc.pushValue(5);
        calc.pushValue(25);
        calc.pushOperation("max");
        assertEquals(25, calc.pop(), "Max of 15, 5, 25 should be 25");

        calc.pushValue(42);
        long startTime = System.currentTimeMillis();
        int result = calc.delayPop(1000);
        long endTime = System.currentTimeMillis();
        assertEquals(42, result, "DelayPop should return correct value");
        assertTrue(endTime - startTime >= 1000, "DelayPop should wait at least 1 second");

        System.out.println("Calculator operations test completed successfully");
    }

    //Test error handling
    @Test
    @DisplayName("Should handle error conditions properly")
    void testErrorHandling() throws Exception {
        Registry registry = LocateRegistry.getRegistry("localhost", TEST_RMI_PORT);
        Calculator calc = (Calculator) registry.lookup("Calculator");

        String sessionId = calc.createSession();
        calc.setSession(sessionId);

        RemoteException exception = assertThrows(RemoteException.class, calc::pop, "Should throw exception when popping from empty stack");
        assertTrue(exception.getMessage().contains("empty"), "Exception should mention empty stack");

        calc.pushValue(10);
        RemoteException opException = assertThrows(RemoteException.class, () -> {
            calc.pushOperation("invalid");
        }, "Should throw exception for invalid operation");
        assertTrue(opException.getMessage().contains("Invalid operator"), "Exception should mention invalid operator");

        RemoteException sessionException = assertThrows(RemoteException.class, () -> {
            calc.setSession("fake-session-id");
        }, "Should throw exception for invalid session ID");
        assertTrue(sessionException.getMessage().contains("Invalid session"), "Exception should mention invalid session");

        System.out.println("Error handling test completed successfully");
    }

    //Test per client stack isolation
    @Test
    @DisplayName("per client stack test")
    @Order(3)
    void testBonusFeatureStackIsolation() throws Exception {
        System.out.println("per client stack testing");

        Registry registry1 = LocateRegistry.getRegistry("localhost", TEST_RMI_PORT);
        Calculator client1 = (Calculator) registry1.lookup("Calculator");

        String session1 = client1.createSession();
        client1.setSession(session1);
        System.out.println("Client 1 session - " + session1);

        assertTrue(client1.isEmpty(), "Client 1 should start empty");
        client1.pushValue(10);
        client1.pushValue(20);
        assertFalse(client1.isEmpty(), "Client 1 should have values");

        assertEquals(20, client1.pop(), "Client 1 should get 20");
        assertEquals(10, client1.pop(), "Client 1 should get 10");
        assertTrue(client1.isEmpty(), "Client 1 should be empty");

        System.out.println("Client 1 completed successfully");

        Registry registry2 = LocateRegistry.getRegistry("localhost", TEST_RMI_PORT);
        Calculator client2 = (Calculator) registry2.lookup("Calculator");

        String session2 = client2.createSession();
        client2.setSession(session2);
        System.out.println("Client 2 session - " + session2);

        assertNotEquals(session1, session2, "Each client should have unique session");

        assertTrue(client2.isEmpty(), "Client 2 should start empty");
        client2.pushValue(100);
        client2.pushValue(200);
        assertFalse(client2.isEmpty(), "Client 2 should have values");

        assertEquals(200, client2.pop(), "Client 2 should get 200");
        assertEquals(100, client2.pop(), "Client 2 should get 100");
        assertTrue(client2.isEmpty(), "Client 2 should be empty");

        System.out.println("Client 2 completed successfully");

        assertTrue(client1.isEmpty(), "Client 1 should still be empty");
        assertTrue(client2.isEmpty(), "Client 2 should be empty");

        System.out.println("Per client stack test completed");
    }

    //Test session switching
    @Test
    @DisplayName("Test session switching")
    @Order(4)
    void testBonusFeatureSessionSwitching() throws Exception {
        System.out.println("testing session switching");

        Registry reg = LocateRegistry.getRegistry("localhost", TEST_RMI_PORT);
        Calculator calc = (Calculator) reg.lookup("Calculator");

        String sessionA = calc.createSession();
        String sessionB = calc.createSession();
        String sessionC = calc.createSession();

        assertNotNull(sessionA, "Session A should be created");
        assertNotNull(sessionB, "Session B should be created");
        assertNotNull(sessionC, "Session C should be created");

        assertNotEquals(sessionA, sessionB, "Sessions should be unique");
        assertNotEquals(sessionB, sessionC, "Sessions should be unique");
        assertNotEquals(sessionA, sessionC, "Sessions should be unique");

        calc.setSession(sessionA);
        calc.pushValue(111);

        calc.setSession(sessionB);
        calc.pushValue(222);

        calc.setSession(sessionC);
        calc.pushValue(333);

        calc.setSession(sessionA);
        assertEquals(111, calc.pop(), "Session A should retain its value");

        calc.setSession(sessionB);
        assertEquals(222, calc.pop(), "Session B should retain its value");

        calc.setSession(sessionC);
        assertEquals(333, calc.pop(), "Session C should retain its value");

        System.out.println("Session switching test completed successfully ");
    }
}
