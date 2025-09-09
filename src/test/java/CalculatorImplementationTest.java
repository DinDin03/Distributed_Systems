import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;
import java.rmi.RemoteException;

//Unit tests for CalculatorImplementation class testing all mathematical operations and
public class CalculatorImplementationTest {
    private CalculatorImplementation calculator;

    //Setup a new calculator instance with a session before each test
    @BeforeEach
    void setUp() throws RemoteException {
        calculator = new CalculatorImplementation();
        String testSessionId = calculator.createSession();
        calculator.setSession(testSessionId);
    }

    //Test GCD calculation
    @Test
    @DisplayName("Should calculate GCD of multiple numbers correctly")
    void testGCDCalculation() throws RemoteException {
        calculator.pushValue(12);
        calculator.pushValue(18);
        calculator.pushValue(24);
        
        calculator.pushOperation("gcd");
        
        int result = calculator.pop();
        assertEquals(6, result, "GCD of 12, 18, 24 should be 6");
        assertTrue(calculator.isEmpty(), "Stack should be empty after operation");
    }

    //Test LCM calculation
    @Test
    @DisplayName("Should calculate LCM of two numbers correctly")
    void testLCMCalculation() throws RemoteException {
        calculator.pushValue(4);
        calculator.pushValue(6);
        
        calculator.pushOperation("lcm");
        
        int result = calculator.pop();
        assertEquals(12, result, "LCM of 4, 6 should be 12");
        assertTrue(calculator.isEmpty(), "Stack should be empty after operation");
    }

    //Test min operation
    @Test
    @DisplayName("Should find minimum value correctly")
    void testMinOperation() throws RemoteException {
        calculator.pushValue(15);
        calculator.pushValue(5);
        calculator.pushValue(25);
        calculator.pushValue(10);
        
        calculator.pushOperation("min");
        
        int result = calculator.pop();
        assertEquals(5, result, "Min of 15, 5, 25, 10 should be 5");
        assertTrue(calculator.isEmpty(), "Stack should be empty after operation");
    }

    //Test max operation
    @Test
    @DisplayName("Should find maximum value correctly")
    void testMaxOperation() throws RemoteException {
        calculator.pushValue(15);
        calculator.pushValue(5);
        calculator.pushValue(25);
        calculator.pushValue(10);
        
        calculator.pushOperation("max");
        
        int result = calculator.pop();
        assertEquals(25, result, "Max of 15, 5, 25, 10 should be 25");
        assertTrue(calculator.isEmpty(), "Stack should be empty after operation");
    }

    //Test basic stack operations
    @Test
    @DisplayName("Should handle basic stack operations correctly")
    void testBasicStackOperations() throws RemoteException {
        assertTrue(calculator.isEmpty(), "New calculator should be empty");
        
        calculator.pushValue(10);
        calculator.pushValue(20);
        calculator.pushValue(30);
        
        assertFalse(calculator.isEmpty(), "Calculator should not be empty after pushes");
        
        assertEquals(30, calculator.pop(), "Should pop 30 (LIFO)");
        assertEquals(20, calculator.pop(), "Should pop 20 (LIFO)");
        assertEquals(10, calculator.pop(), "Should pop 10 (LIFO)");
        
        assertTrue(calculator.isEmpty(), "Calculator should be empty after all pops");
    }

    //Test delayPop
    @Test
    @DisplayName("Should handle delayPop correctly")
    void testDelayPop() throws RemoteException {
        calculator.pushValue(42);
        
        long startTime = System.currentTimeMillis();
        int result = calculator.delayPop(1000);
        long endTime = System.currentTimeMillis();
        
        assertEquals(42, result, "DelayPop should return correct value");
        assertTrue(endTime - startTime >= 1000, "DelayPop should wait at least 1 second");
        assertTrue(calculator.isEmpty(), "Stack should be empty after delayPop");
    }

    //Test invalid operations
    @Test
    @DisplayName("Should throw exception for invalid operations")
    void testInvalidOperations() throws RemoteException {
        calculator.pushValue(10);
        
        RemoteException exception = assertThrows(RemoteException.class, () -> {
            calculator.pushOperation("invalid");
        }, "Should throw exception for invalid operation");
        
        assertTrue(exception.getMessage().contains("Invalid operator"), 
                  "Exception should mention invalid operator");
        
        assertFalse(calculator.isEmpty(), "Stack should still contain original value after failed operation");
    }

    //Test popping from empty stack
    @Test
    @DisplayName("Should throw exception when popping from empty stack")
    void testPopFromEmptyStack() {
        RemoteException exception = assertThrows(RemoteException.class, () -> {
            calculator.pop();
        }, "Should throw exception when popping from empty stack");
        
        assertTrue(exception.getMessage().contains("empty"), 
                  "Exception message should mention empty stack");
    }

    //Test operation on empty stack
    @Test
    @DisplayName("Should throw exception when performing operation on empty stack")
    void testOperationOnEmptyStack() {
        RemoteException exception = assertThrows(RemoteException.class, () -> {
            calculator.pushOperation("min");
        }, "Should throw exception when performing operation on empty stack");
        
        assertTrue(exception.getMessage().contains("empty"), 
                  "Exception message should mention empty stack");
    }

    //Test single value operations
    @Test
    @DisplayName("Should handle single value operations correctly")
    void testSingleValueOperations() throws RemoteException {
        calculator.pushValue(42);
        
        calculator.pushOperation("min");
        assertEquals(42, calculator.pop(), "Min of single value should be the value itself");
        
        calculator.pushValue(42);
        calculator.pushOperation("max");
        assertEquals(42, calculator.pop(), "Max of single value should be the value itself");
        
        calculator.pushValue(42);
        calculator.pushOperation("gcd");
        assertEquals(42, calculator.pop(), "GCD of single value should be the value itself");
        
        calculator.pushValue(42);
        calculator.pushOperation("lcm");
        assertEquals(42, calculator.pop(), "LCM of single value should be the value itself");
    }
}