import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.RemoteException;

// RMI client for the distributed calculator
public class CalculatorClient{
    // main method to start the client
    public static void main(String[] args){
        try{
            //lookup the calculator service on the registry
            Registry registry = LocateRegistry.getRegistry("localhost", 1099);
            Calculator calculator = (Calculator) registry.lookup("Calculator");

            //create a new session
            String sessionId = calculator.createSession();
            System.out.println("Created session " + sessionId);

            //set the session
            calculator.setSession(sessionId);
            System.out.println("Session set successfully");

            //Perform basic stack operations on client's stack
            calculator.pushValue(10);
            System.out.println("Pushed 10");
            calculator.pushValue(20);
            System.out.println("Pushed 20");

            //Pop value from client's personal stack
            int result = calculator.pop();
            System.out.println("Popped " + result);

        }catch(RemoteException e){
            System.err.println("Failed to connect to server " + e.getMessage());
        }
        catch(Exception e){
            System.err.println("Client error " + e.getMessage());
        }
    }
}