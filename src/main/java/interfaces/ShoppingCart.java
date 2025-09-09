package interfaces;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface ShoppingCart extends Remote{
    void addItem(CartItem item) throws RemoteException;

    void removeItem(String itemName) throws RemoteException;
    void removeItem(String itemName, int quantity) throws RemoteException;

    List<CartItem> getCartContents() throws RemoteException;

    double getTotalPrice() throws RemoteException;
    int getTotalItems() throws RemoteException;

    void clearCart() throws RemoteException;
}

