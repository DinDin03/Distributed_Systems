# Distributed Weather System

A distributed client-server system that aggregates and distributes weather data using a RESTful API with Lamport clock synchronization. The system consists of an aggregation server, content servers for uploading weather data, and GET clients for retrieving weather information.

## Project Purpose

This system demonstrates key distributed systems concepts including:
- **RESTful API communication** using HTTP over sockets
- **Lamport clock synchronization** for event ordering across distributed components
- **Concurrent request handling** with thread-safe operations
- **Data persistence** with crash recovery capabilities
- **Fault tolerance** with retry mechanisms and failover support
- **Data expiry** with automatic cleanup of stale weather data

## System Components

### Aggregation Server (`AggregationServer.java`)
- Central server that stores and distributes weather data
- Handles PUT requests from content servers and GET requests from clients
- Implements 30-second data expiry mechanism
- Supports Lamport clock ordering for request processing
- Provides persistent storage with crash recovery

### Content Server (`ContentServer.java`)
- Uploads weather data to the aggregation server
- Reads weather data from local files and converts to JSON format
- Supports multiple server failover for fault tolerance
- Implements retry logic with exponential backoff

### GET Client (`GETClient.java`)
- Retrieves and displays weather data from the aggregation server
- Supports filtering by station ID
- Handles multiple server connections with failover

## Dependencies

- **Java 8 or higher**
- **Maven** for build management
- **JUnit 5** for testing
- **Gson** for JSON processing (optional - system includes custom JSON implementation)

## Setup Steps

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd Distributed_Systems
   ```

2. **Install Java 8+**
   - Ensure Java 8 or higher is installed on your system
   - Verify installation: `java -version`

3. **Install Maven**
   - Download and install Maven from https://maven.apache.org/
   - Verify installation: `mvn -version`

4. **Build the project**
   ```bash
   mvn clean compile
   ```

## How to Run

### Running the Aggregation Server

Start the server on the default port (4567):
```bash
mvn exec:java -Dexec.mainClass="com.weathersystem.server.AggregationServer"
```

Or specify a custom port:
```bash
mvn exec:java -Dexec.mainClass="com.weathersystem.server.AggregationServer" -Dexec.args="8080"
```

### Running a Content Server

Upload weather data from a file:
```bash
mvn exec:java -Dexec.mainClass="com.weathersystem.client.ContentServer" -Dexec.args="localhost:4567 input/weather1.txt"
```

Multiple server support:
```bash
mvn exec:java -Dexec.mainClass="com.weathersystem.client.ContentServer" -Dexec.args="localhost:4567,localhost:4568 input/weather1.txt"
```

### Running a GET Client

Retrieve all weather data:
```bash
mvn exec:java -Dexec.mainClass="com.weathersystem.client.GETClient" -Dexec.args="localhost:4567"
```

Retrieve data for a specific station:
```bash
mvn exec:java -Dexec.mainClass="com.weathersystem.client.GETClient" -Dexec.args="localhost:4567 IDS60901"
```

### Running Multiple Servers

Use the provided scripts to start multiple servers:
```bash
# Start multiple servers on different ports
./start_multiple_servers.sh

# Stop all servers
./stop_servers.sh
```

## Running Tests

### Run All Tests
```bash
mvn test
```

### Run Specific Test Classes
```bash
# Run client tests
mvn test -Dtest="com.weathersystem.client.*Test"

# Run server tests
mvn test -Dtest="com.weathersystem.server.*Test"

# Run integration tests
mvn test -Dtest="com.weathersystem.integration.*Test"
```

### Run Tests with Coverage
```bash
mvn test jacoco:report
```

## Input File Format

Weather data files should follow this format:
```
id:IDS60901
name:Adelaide (West Terrace / ngayirdapira)
state:SA
time_zone:CST
lat:-34.9
lon:138.6
local_date_time:15/04:00pm
local_date_time_full:20230715160000
air_temp:13.3
apparent_t:9.5
cloud:Partly cloudy
dewpt:5.7
press:1023.9
rel_hum:60
wind_dir:S
wind_spd_kmh:15
wind_spd_kt:8
```

## API Endpoints

### PUT /weather.json
Uploads weather data to the server
- **201 Created**: First successful upload
- **200 OK**: Subsequent successful updates
- **204 No Content**: Empty request body
- **400 Bad Request**: Invalid request
- **500 Internal Server Error**: Invalid JSON data

### GET /weather.json
Retrieves all weather data from the server
- **200 OK**: Success with weather data
- **400 Bad Request**: Invalid request

## Key Features

- **Lamport Clock Synchronization**: Ensures proper event ordering across distributed components
- **Concurrent Processing**: Thread-safe handling of multiple simultaneous requests
- **Data Persistence**: Weather data survives server restarts and crashes
- **Automatic Expiry**: Removes data from inactive content servers after 30 seconds
- **Fault Tolerance**: Retry mechanisms and multiple server support
- **Custom JSON Implementation**: Self-contained JSON processing without external dependencies

## Project Structure

```
src/
├── main/java/com/weathersystem/
│   ├── client/          # Client implementations
│   ├── server/          # Server implementation
│   ├── shared/          # Shared utilities and domain objects
│   └── utils/           # Utility classes
└── test/java/com/weathersystem/
    ├── client/          # Client tests
    ├── integration/     # Integration tests
    ├── server/          # Server tests
    └── shared/          # Shared utility tests
```

## Notes

- The system uses custom HTTP implementation over sockets (no external web frameworks)
- All components implement Lamport clocks for distributed synchronization
- Weather data is stored in JSON format with automatic backup mechanisms
- The system supports multiple content servers and clients simultaneously
- Data expiry is handled by a background service that runs every 30 seconds
- All operations are thread-safe and handle concurrent access properly

## Troubleshooting

- **Port already in use**: Try a different port number or stop existing processes
- **Connection refused**: Ensure the aggregation server is running before starting clients
- **File not found**: Check that input files exist in the correct directory
- **JSON parsing errors**: Verify input file format matches the expected structure

## Development

To contribute to this project:
1. Follow the existing code style and commenting conventions
2. Add tests for new functionality
3. Ensure all tests pass before submitting changes
4. Update documentation as needed