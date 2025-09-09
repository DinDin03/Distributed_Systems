package client;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import interfaces.AreaCalculator;
import java.rmi.RemoteException;

public class AreaCalculatorClient{
    public static void main(String[] args){
        try{
            Registry registry = LocateRegistry.getRegistry("localhost", 1099);

            AreaCalculator calculator = (AreaCalculator) registry.lookup("Calculator");

            double result = calculator.calculateArea(5.0,3.0);

            System.out.println("The area is " + result);

        }catch(RemoteException e){
            System.err.println("Failed to connect to server: " + e.getMessage());
        }catch(Exception e){
            System.err.println("Client error: " + e.getMessage());
        }
    }
}