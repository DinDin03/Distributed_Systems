package interfaces;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface AreaCalculator extends Remote{
    double calculateArea(double length, double width) throws RemoteException;
}