package client;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;

import interfaces.CartItem;
import interfaces.ShoppingCart;
import java.rmi.RemoteException;

public class ShoppingCartClient {
    public static void main(String[] args) {
        try {
            Registry registry = LocateRegistry.getRegistry("localhost", 1099);

            ShoppingCart shoppingCart = (ShoppingCart) registry.lookup("ShoppingCart");

            System.out.println("Testing Add Items");
            shoppingCart.addItem(new CartItem("Laptop", 2, 999.99));
            shoppingCart.addItem(new CartItem("Mouse", 1, 25.50));
            shoppingCart.addItem(new CartItem("Book", 3, 15.00));

            System.out.println("Adding another laptop to test combining");
            shoppingCart.addItem(new CartItem("Laptop", 1, 999.99)); // Should combine to 3 laptops

            System.out.println("\nCart Contents");
            List<CartItem> contents = shoppingCart.getCartContents();
            for (CartItem item : contents) {
                System.out.println(item.getItemName() + ": " + item.getQuantity() +
                        " x $" + item.getPricePerUnit() + " = $" + item.totalPrice());
            }

            System.out.println("\nTotals");
            System.out.println("Total items: " + shoppingCart.getTotalItems());
            System.out.println("Total price: $" + shoppingCart.getTotalPrice());

            System.out.println("\nTesting Partial Remove");
            shoppingCart.removeItem("Book", 1);
            System.out.println("Removed 1 book. Books remaining: " +
                    shoppingCart.getCartContents().stream()
                            .filter(item -> item.getItemName().equals("Book"))
                            .findFirst().map(item -> item.getQuantity()).orElse(0));

            System.out.println("\nTesting Complete Remove");
            shoppingCart.removeItem("Mouse"); 
            System.out.println("Removed all mice. Cart size: " + shoppingCart.getTotalItems());

            System.out.println("\nFinal Cart State");
            contents = shoppingCart.getCartContents();
            for (CartItem item : contents) {
                System.out.println(item.getItemName() + ": " + item.getQuantity());
            }

        } catch (RemoteException e) {
            System.err.println("Failed to connect to server: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Client error: " + e.getMessage());
        }
    }
}
