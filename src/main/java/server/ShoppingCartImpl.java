package server;

import interfaces.ShoppingCart;
import interfaces.CartItem;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ShoppingCartImpl extends UnicastRemoteObject implements ShoppingCart {
    private HashMap<String, CartItem> shoppingCart;

    public ShoppingCartImpl() throws RemoteException {
        super();
        shoppingCart = new HashMap<>();
    }

    @Override
    public void addItem(CartItem item) throws RemoteException {
        String itemName = item.getItemName();

        if (shoppingCart.containsKey(itemName)) {
            CartItem existingItem = shoppingCart.get(itemName);
            int newQuantity = existingItem.getQuantity() + item.getQuantity();
            CartItem combinedItem = new CartItem(itemName, newQuantity, item.getPricePerUnit());
            shoppingCart.put(itemName, combinedItem);
            System.out.println("Item added to the cart: " + itemName);
        } else {
            shoppingCart.put(itemName, item);
        }
    }

    @Override
    public void removeItem(String itemName) throws RemoteException {
        if (shoppingCart.containsKey(itemName)) {
            shoppingCart.remove(itemName);
            System.out.println("Item removed from the cart: " + itemName);

        }
    }

    @Override
    public void removeItem(String itemName, int quantity) throws RemoteException {
        if (shoppingCart.containsKey(itemName)) {
            CartItem existingItem = shoppingCart.get(itemName);
            int newQuantity = existingItem.getQuantity() - quantity;

            if (newQuantity <= 0) {
                shoppingCart.remove(itemName);
            } else {
                CartItem newItem = new CartItem(itemName, newQuantity, existingItem.getPricePerUnit());
                shoppingCart.put(itemName, newItem);
            }
            System.out.println("Item removed from the cart: " + itemName);
        }
    }

    @Override
    public List<CartItem> getCartContents() throws RemoteException {
        return new ArrayList<>(shoppingCart.values());
    }

    @Override
    public double getTotalPrice() throws RemoteException {
        return shoppingCart.values().stream()
                .mapToDouble(CartItem::totalPrice)
                .sum();
    }

    @Override
    public int getTotalItems() throws RemoteException {
        return shoppingCart.values().stream()
                .mapToInt(CartItem::getQuantity)
                .sum();
    }

    @Override
    public void clearCart() throws RemoteException {
        shoppingCart.clear();
    }

}
