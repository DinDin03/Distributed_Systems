package server;

import interfaces.CartItem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;
import java.rmi.RemoteException;
import java.util.List;

public class ShoppingCartImplTest {
    private ShoppingCartImpl cart;

    @BeforeEach
    void setUp() throws RemoteException {
        cart = new ShoppingCartImpl();
    }

    @Test
    @DisplayName("new cart should be empty")
    void testNewCartIsEmpty() throws RemoteException {
        assertEquals(0, cart.getTotalItems());
        assertEquals(0.0, cart.getTotalPrice(), 0.01);
        assertTrue(cart.getCartContents().isEmpty());
    }

    @Test
    @DisplayName("adding one item to the cart")
    void testAddSingleItem() throws RemoteException {
        CartItem Laptop = new CartItem("Laptop", 2, 999.99);
        cart.addItem(Laptop);

        assertEquals(2, cart.getTotalItems());
        assertEquals(1999.98, cart.getTotalPrice(), 0.01);
        assertEquals(1, cart.getCartContents().size());
    }

    @Test
    @DisplayName("adding duplicate items should combine quantities")
    void testAddDuplicateItems() throws RemoteException {
        cart.addItem(new CartItem("Laptop", 1, 999.99));
        cart.addItem(new CartItem("Laptop", 2, 999.99));

        assertEquals(3, cart.getTotalItems());
        assertEquals(2999.97, cart.getTotalPrice(), 0.1);
        List<CartItem> contents = cart.getCartContents();
        assertEquals(1, contents.size());
        assertEquals("Laptop", contents.get(0).getItemName());
        assertEquals(3, contents.get(0).getQuantity());
    }

    @Test
    @DisplayName("clear cart should empty everything")
    void testClearCart() throws RemoteException {
        cart.addItem(new CartItem("Laptop", 2, 999.99));
        cart.addItem(new CartItem("Mouse", 1, 25.50));

        cart.clearCart();

        assertEquals(0, cart.getTotalItems());
        assertEquals(0.0, cart.getTotalPrice(), 0.01);
        assertTrue(cart.getCartContents().isEmpty());
    }
}
