package server;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;
import java.rmi.RemoteException;

public class AreaCalculatorImplTest {
    private AreaCalculatorImpl calculator;

    @BeforeEach
    void setUp() throws RemoteException {
        calculator = new AreaCalculatorImpl();
    }

    @Test
    @DisplayName("calculate rectangle area for positive numbers")
    void testBasicAreaCalculation() throws RemoteException {
        assertEquals(15.0, calculator.calculateArea(5.0, 3.0), 0.001);
        assertEquals(100.0, calculator.calculateArea(10.0, 10.0), 0.001);
        assertEquals(24.5, calculator.calculateArea(7.0, 3.5), 0.001);
    }

    @Test
    @DisplayName("handle decimal values")
    void testDecimalCalculation() throws RemoteException {
        assertEquals(15.75, calculator.calculateArea(4.5, 3.5), 0.001);
        assertEquals(999.9999, calculator.calculateArea(99.99, 10.001), 0.001);
    }

    @Test
    @DisplayName("handle zero dimensions")
    void testZeroDimensions() throws RemoteException {
        assertEquals(0.0, calculator.calculateArea(0.0, 5.0), 0.001);
        assertEquals(0.0, calculator.calculateArea(5.0, 0.0), 0.001);
        assertEquals(0.0, calculator.calculateArea(0.0, 0.0), 0.001);
    }

    @Test
    @DisplayName("handle very small numbers")
    void testSmallNumbers() throws RemoteException {
        assertEquals(0.0001, calculator.calculateArea(0.01, 0.01), 0.0001);
        assertEquals(0.1, calculator.calculateArea(0.1, 1.0), 0.001);
    }

    @Test
    @DisplayName("handle large numbers")
    void testLargeNumbers() throws RemoteException {
        assertEquals(1000000.0, calculator.calculateArea(1000.0, 1000.0), 0.001);
        assertEquals(10000000.0, calculator.calculateArea(10000.0, 1000.0), 0.1);
    }

    @Test
    @DisplayName("mathematically consistent")
    void testMathematicalProperties() throws RemoteException {
        double result1 = calculator.calculateArea(7.0, 11.0);
        double result2 = calculator.calculateArea(11.0, 7.0);
        assertEquals(result1, result2, 0.001);

        assertEquals(5.5, calculator.calculateArea(5.5, 1.0), 0.001);
        assertEquals(8.0, calculator.calculateArea(1.0, 8.0), 0.001);
    }
}