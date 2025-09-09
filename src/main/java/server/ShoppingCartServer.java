package server;

import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.RemoteException;
import interfaces.ShoppingCart;

public class ShoppingCartServer {
    public static void main(String[] args){
        try{
            ShoppingCart shoppingCart = new ShoppingCartImpl();

            Registry registry = LocateRegistry.createRegistry(1099);

            registry.bind("ShoppingCart", shoppingCart);

            System.out.println("ShoppingCart server is running on port 1099");

            Thread.currentThread().join();
        }catch(RemoteException e){
            System.err.println("Server failed to start: " + e.getMessage());
            return;
        }catch(Exception e){
            System.err.println("Failed to bind service: " + e.getMessage());
            return;
        }
    }
}
