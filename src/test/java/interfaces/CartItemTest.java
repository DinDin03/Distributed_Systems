package interfaces;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

public class CartItemTest {
    private CartItem laptop;
    private CartItem book;
    
    @BeforeEach
    void setUp() {
        laptop = new CartItem("Laptop", 2, 999.99);
        book = new CartItem("Book", 5, 15.50);
    }
    
    @Test
    @DisplayName("constructor should set all fields correctly")
    void testConstructor() {
        assertEquals("Laptop", laptop.getItemName());
        assertEquals(2, laptop.getQuantity());
        assertEquals(999.99, laptop.getPricePerUnit(), 0.01);
    }
    
    @Test
    @DisplayName("total price should calculate quantity times price per unit")
    void testTotalPrice() {
        assertEquals(1999.98, laptop.totalPrice(), 0.01);
        assertEquals(77.50, book.totalPrice(), 0.01);
    }
    
    @Test
    @DisplayName("handle zero quantity")
    void testZeroQuantity() {
        CartItem zeroItem = new CartItem("Test", 0, 10.0);
        assertEquals(0.0, zeroItem.totalPrice(), 0.01);
    }
    
    @Test
    @DisplayName("handle zero price")
    void testZeroPrice() {
        CartItem freeItem = new CartItem("Free Sample", 3, 0.0);
        assertEquals(0.0, freeItem.totalPrice(), 0.01);
    }
    
    @Test
    @DisplayName("getters should return correct values")
    void testGetters() {
        assertEquals("Book", book.getItemName());
        assertEquals(5, book.getQuantity());
        assertEquals(15.50, book.getPricePerUnit(), 0.01);
    }
}