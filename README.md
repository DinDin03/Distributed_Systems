# Distributed Weather System

A distributed client server system that aggregates and distributes weather data using a Restful API with lamport clock synchronisation. The system consists of an aggregation server, content servers for uploading weather data, and GET clients for retrieving weather information.

## Project Purpose

This system demonstrates key distributed systems concepts including:
- **RESTful API communication** using HTTP over sockets
- **Lamport clock synchronisation** for event ordering across distributed components
- **Concurrent request handling** with thread safe operations
- **Data persistence** with crash recovery capabilities
- **Fault tolerance** with retry mechanisms and failover support
- **Data expiry** with automatic cleanup of stale weather data

## System Components

### Aggregation Server (`AggregationServer.java`)
- Central server that stores and distributes weather data
- Handles PUT requests from content servers and GET requests from clients
- Implements 30 second data expiry mechanism
- Supports lamport clock ordering for request processing
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
- **Gson** for JSON processing

## Setup Steps

1. **Go to project directory**
   ```bash
   cd Distributed_Systems
   ```

2. **Install Java 8+**
   - Ensure Java 8 or higher is installed on the system
   - Verify installation: `java -version`

3. **Install Maven**
   - Download and install Maven
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

- The system uses custom HTTP implementation over sockets
- All components implement lamport clocks for distributed synchronisation
- Weather data is stored in JSON format with automatic backup mechanisms
- The system supports multiple content servers and clients simultaneously
- Data expiry is handled by a background service that runs every 30 seconds
- All operations are thread safe and handle concurrent access properly