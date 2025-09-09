# Java RMI Calculator - Assignment 1

## Features

### Main functionality
- Stack operations - `pushValue()`, `pop()`, `isEmpty()`, `delayPop()`
- Mathematical operations - `min`, `max`, `gcd`, `lcm`
- Multi client Support - Multiple clients can connect simultaneously
- Error handling - Comprehensive exception handling for all edge cases

### Bonus feature - per client stack 
- Individual stacks - Each client gets their own isolated stack
- Session switching - Clients can manage multiple sessions 
- Thread safe - Concurrent client access with proper synchronisation 
- UUID based sessions - Secure and unique session identifications 

## Files 

### Main files 

- Calculator.java - RMI interface
- CalculatorImplementation.java - Server implementation 
- CalculatorServer.java - Server bootstrap
- CalculatorClient.java - Test client

### Test files 

- CalculatorImplementationTest.java - Unit tests 
- CalculatorIntegrationTest.java - Integration tests

## Start guide

### Prerequisites
- Java 11 or higher
- Maven 3.6 or higher
- Linux environment

### Verify prerequisites
```bash

#checkout to the Assignment1 branch
git checkout Assignment1

# Check Java version
java -version

# Check Maven version.
mvn -version

#If maven is not installed you will have to install using
sudo apt install maven 
```

## Compilation instructions

### 1. Compile all Java files
```bash

# Navigate to project directory
# Compile all source files
mvn compile

# Compile test files
mvn test-compile
```

## Running the system

### Method 1: Using Maven

#### Terminal 1 - Start RMI Server
```bash

# Start the RMI server
mvn exec:java -Dexec.mainClass="CalculatorServer"

```

#### Terminal 2 - Run client
```bash

# Run the test client
mvn exec:java -Dexec.mainClass="CalculatorClient"
```

### Method 2: Using Shell Scripts

#### Make scripts executable
```bash

chmod +x scripts/start-server.sh
chmod +x scripts/run-client.sh
```

#### Terminal 1 - Start Server
```bash

./scripts/start-server.sh
```

#### Terminal 2 - Run Client
```bash

./scripts/run-client.sh
```

## Testing instructions

### Run All Tests
```bash

# Run both unit and integration tests
mvn test

# Expected output
# Tests run: 15, Failures: 0, Errors: 0, Skipped: 0
```

### Run Specific Test Suites

#### Unit Tests Only
```bash

mvn test -Dtest=CalculatorImplementationTest

# Tests mathematical operations in isolation
# Tests: 10, Failures: 0, Errors: 0
```

#### Integration Tests Only
```bash

mvn test -Dtest=CalculatorIntegrationTest

# Tests full RMI client-server communication
# Tests: 5, Failures: 0, Errors: 0
```

### Test Coverage
- Unit Tests - Mathematical operations, error handling, single client scenarios
- Integration Tests - RMI communication, multi client scenarios, bonus feature

## Multi-Client Testing

### Testing Multiple Clients Simultaneously

#### Terminal 1 - Server
```bash

mvn exec:java -Dexec.mainClass="CalculatorServer"
```

#### Terminal 2 - Client 1
```bash

mvn exec:java -Dexec.mainClass="CalculatorClient"
```

#### Terminal 3 - Client 2
```bash

mvn exec:java -Dexec.mainClass="CalculatorClient"
```

#### Terminal 4 - Client 3
```bash

mvn exec:java -Dexec.mainClass="CalculatorClient"
```

### Expected Behavior
- Each client gets a unique session ID
- Each client operates on their own isolated stack
- Operations from one client do not affect other clients
- Server logs show different session IDs for each client

## Code quality

### Design patterns
- Remote Proxy Pattern - RMI interface implementation
- Session Pattern - Per client state management
- ThreadLocal Storage - Session isolation
- Factory Pattern - Session creation

### Security Features
- Session validation - Invalid session detection
- Input validation - Parameter checking
- Error containment - Graceful exception handling
- Resource protection - Synchronised access

### Performance Optimizations
- ConcurrentHashMap - Thread safe session storage
- ThreadLocal - Efficient session retrieval
- Connection pooling - RMI optimisation
- Memory management - Proper session lifecycle

### Technical Decisions
1. UUID for Session IDs - Ensures uniqueness across distributed environment
2. Synchronized Methods - Ensures atomic operations

### Per client stack architecture
```
Client A -> RMI → Server thread 1 -> ThreadLocal[SessionA] -> Stack A
Client B -> RMI → Server thread 2 -> ThreadLocal[SessionB] -> Stack B
Client C -> RMI → Server thread 3 -> ThreadLocal[SessionC] -> Stack C
```

