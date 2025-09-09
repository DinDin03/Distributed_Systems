import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.RemoteException;

//RMI server bootstrap class for the distributed calculator
public class CalculatorServer{
    //main method to start the server
    public static void main(String[] args){
        try{
            //create the calculator implementation
            Calculator calculator = new CalculatorImplementation();

            //create the RMI registry and bind the calculator service
            Registry registry = LocateRegistry.createRegistry(1099);

            //bind the calculator service to the registry
            registry.bind("Calculator", calculator);

            System.out.println("Calculator Server is running on port 1099");

            //keep the server running
            Thread.currentThread().join();

        }catch(RemoteException e){
            System.err.println("Server failed to start: " + e.getMessage());
        }catch(InterruptedException e){
            System.out.println("Server interrupted. Shutting down");
        }catch(Exception e){
            System.err.println("Failed to bind service: " + e.getMessage());
        }
    }
}