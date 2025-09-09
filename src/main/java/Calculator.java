import java.rmi.Remote;
import java.rmi.RemoteException;

/*RMI interface for the distributed calculator server
* provides stack operations and mathematical operations with per client session management*/
public interface Calculator extends Remote {

    //pushes a value onto the client's personal stack
    void pushValue(int val) throws RemoteException;

    //performs a mathematical operation on all values on client's stack
    void pushOperation(String operation) throws RemoteException;

    //pops the top value from the client's stack
    int pop() throws RemoteException;

    //checks if the client's stack is empty
    boolean isEmpty() throws RemoteException;

    //pops the top value from the client's stack after a delay
    int delayPop(int millis) throws RemoteException;

    //creates a new session for the client
    String createSession() throws RemoteException;

    //sets the current session for the client
    void setSession(String sessionId) throws RemoteException;
     
}
