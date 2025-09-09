import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

//Implementation of the calculator RMI interface with per client stack
//Provides a thread safe calculator operations with isolated stacks for each client
//Uses ThreadLocal storage for session isolation and ConcurrentHashMap for thread safe session storage
public class CalculatorImplementation extends UnicastRemoteObject implements Calculator{

    //List of valid operators accepted by the calculator
    private final List<String> validOperators = Arrays.asList("min", "max", "gcd", "lcm");

    //Map to store client stacks with session id as key
    private final Map<String, Deque<Integer>> clientStacks;

    //ThreadLocal to store current session id for each thread
    private final ThreadLocal<String> currentSession;

    //Constructor to initialise the calculator implementation
    public CalculatorImplementation() throws RemoteException{
        super();

        clientStacks = new ConcurrentHashMap<>();
        currentSession = new ThreadLocal<>();
    }

    //Pushes a value onto the client's personal stack
    @Override
    public synchronized void pushValue(int val) throws RemoteException{
        Deque<Integer> clientStack = getCurrentClientStack();
        clientStack.push(val);
        System.out.println("Pushed " + val + " to session " + currentSession.get());

    }

    //Pops the top value from the client's stack
    @Override
    public synchronized int pop() throws RemoteException{
        Deque<Integer> clientStack = getCurrentClientStack();

        if(clientStack.isEmpty()){
            throw new RemoteException("Cannot pop from an empty stack");
        }

        int value = clientStack.pop();
        System.out.println("Popped " + value + " from session " + currentSession.get());
        return value;
    }

    //Checks if the client's stack is empty
    @Override
    public synchronized boolean isEmpty() throws RemoteException{
        Deque<Integer> clientStack = getCurrentClientStack();
        return clientStack.isEmpty();
    }

    //Pops the top value from the client's stack after a delay
    @Override
    public synchronized int delayPop(int millis) throws RemoteException{
        try{
            Thread.sleep(millis);
        }catch(InterruptedException e){
            Thread.currentThread().interrupt();
            throw new RemoteException("Delay pop was interrupted", e);
        }
        return pop();
    }

    //Performs a mathematical operation on all values on client's stack
    @Override
    public synchronized void pushOperation(String operator) throws RemoteException {
        if (!validOperators.contains(operator)) {
            throw new RemoteException("Invalid operator " + operator);
        }

        Deque<Integer> clientStack = getCurrentClientStack();

        if (clientStack.isEmpty()) {
            throw new RemoteException("Cannot perform operations on an empty stack");
        }

        List<Integer> values = new ArrayList<>();
        while (!clientStack.isEmpty()) {
            values.add(clientStack.pop());
        }

        int result;
        switch (operator) {
            case "min":
                result = Collections.min(values);
                break;
            case "max":
                result = Collections.max(values);
                break;
            case "gcd":
                result = gcdMultiple(values);
                break;
            case "lcm":
                long lcmResult = lcmMultiple(values);
                if (lcmResult > Integer.MAX_VALUE) {
                    throw new RemoteException("LCM result too large for integer " + lcmResult);
                }
                result = (int) lcmResult;
                break;
            default:
                throw new RemoteException("Unknown operator " + operator);
        }

        clientStack.push(result);

        System.out.println("Operation " + operator + " completed for session " + currentSession.get());
    }

    //Creates a new session for the client
    @Override
    public synchronized String createSession() throws RemoteException{
        String sessionId = UUID.randomUUID().toString();
        clientStacks.put(sessionId, new ArrayDeque<>());
        System.out.println("\nNew session created " + sessionId);

        return sessionId;
    }

    //Sets the current session for the client
    @Override
    public synchronized void setSession(String sessionId) throws RemoteException {
        if (sessionId == null) {
            throw new RemoteException("Session ID cannot be null");
        }

        if (!clientStacks.containsKey(sessionId)) {
            throw new RemoteException("Invalid session ID " + sessionId);
        }

        currentSession.set(sessionId);
        System.out.println("Client set session to " + sessionId);
    }

    //Helper methods for GCD calculations
    private int gcd(int a, int b){
        if(b == 0) return a;
        else{
            return gcd(b, a%b);
        }
    }

    //Helper method to calculate GCD of multiple numbers
    private int gcdMultiple(List<Integer> numbers){
        int res = Math.abs(numbers.get(0));
        for(int i = 1; i < numbers.size(); i++){
            res = gcd(res, Math.abs(numbers.get(i)));
        }
        return res;
    }

    //Helper methods for LCM calculations
    private long lcm(int a, int b){
        if(a == 0 || b == 0){
            return 0;
        }
        return Math.abs((long)a * b) / gcd(a, b); 
    }

    //Helper method to calculate LCM of multiple numbers
    private long lcmMultiple(List<Integer> numbers){
        long res = Math.abs(numbers.get(0));
        for(int i = 1; i < numbers.size(); i++){
            res = lcm((int)res, Math.abs(numbers.get(i)));
            
            if (res > Integer.MAX_VALUE) {
                throw new ArithmeticException("LCM result too large");
            }
        }
        return res;
    }

    //Helper method to get the current client's stack
    private Deque<Integer> getCurrentClientStack() throws RemoteException{
        String sessionId = currentSession.get();

        if(sessionId == null){
            throw new RemoteException("No session set, create and set session first");
        }

        Deque<Integer> clientStack = clientStacks.get(sessionId);

        if(clientStack == null){
            throw new RemoteException("Invalid session. Expired or incorrect session id");
        }

        return clientStack;
    }
}