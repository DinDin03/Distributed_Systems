package server;

import interfaces.AreaCalculator;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class AreaCalculatorImpl extends UnicastRemoteObject implements AreaCalculator{

    public AreaCalculatorImpl() throws RemoteException{
        super();
    }

    @Override
    public double calculateArea(double length, double width) throws RemoteException {
        return length * width;
    }
}