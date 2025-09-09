package server;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.RemoteException;
import interfaces.AreaCalculator;

public class AreaCalculatorServer{
    public static void main(String[] args){
        try{
            AreaCalculator calculator = new AreaCalculatorImpl();

            Registry registry = LocateRegistry.createRegistry(1099);

            registry.bind("Calculator", calculator);

            System.out.println("AreaCalculator Server is running on port 1099");

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