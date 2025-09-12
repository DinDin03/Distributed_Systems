#!/bin/bash

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Move to project root (script is inside scripts/)
cd "$(dirname "$0")/.." || exit 1

printf "${BLUE}=== Weather System Expiry Test ===${NC}\n"
printf "Testing file persistence and 30-second data expiry\n\n"

# Kill any process already using port 4567
if lsof -i :4567 >/dev/null 2>&1; then
    echo "Port 4567 already in use, killing process..."
    lsof -ti :4567 | xargs kill -9
    sleep 2
fi

# Function to run GET client and show results
run_get_client() {
    printf "${GREEN}--- Current Weather Data ---${NC}\n"
    mvn exec:java -Dexec.mainClass="com.weathersystem.client.GETClient" -q
    printf "\n"
}

# Function to send weather data
send_weather_data() {
    station=$1
    printf "${YELLOW}Sending data from weather station $station...${NC}\n"
    mvn exec:java -Dexec.mainClass="com.weathersystem.client.ContentServer" \
        -Dexec.args="localhost:4567 input/weather$station.txt" -q
    printf "Weather$station data sent\n\n"
}

# Check if Maven is available
if ! command -v mvn &> /dev/null; then
    printf "${RED}Maven not found. Please install Maven or use java -cp commands instead.${NC}\n"
    exit 1
fi

printf "${BLUE}Step 1: Starting aggregation server in background...${NC}\n"
printf "Starting server (this may take a few seconds)\n"
mvn exec:java -Dexec.mainClass="com.weathersystem.server.AggregationServer" > server.log 2>&1 &
SERVER_PID=$!

# Wait for server to start
sleep 5

# Check if server started successfully
if ! ps -p $SERVER_PID > /dev/null; then
    printf "${RED}Server failed to start. Check server.log for details.${NC}\n"
    exit 1
fi

printf "${GREEN}Server started (PID: $SERVER_PID)${NC}\n\n"

printf "${BLUE}Step 2: Adding initial weather data from all stations...${NC}\n"
send_weather_data 1  # Adelaide
send_weather_data 2  # Melbourne
send_weather_data 3  # Sydney

printf "${BLUE}Step 3: Checking initial data...${NC}\n"
run_get_client

printf "${BLUE}Step 4: Waiting 10 seconds, then refreshing Adelaide...${NC}\n"
sleep 10
printf "Refreshing Adelaide data (resets its expiry timer)...\n"
send_weather_data 1  # Refresh Adelaide

printf "${BLUE}Step 5: Monitoring data over time...${NC}\n"
for i in {1..6}; do
    printf "${YELLOW}Check $i (at $(($i * 10)) seconds since Melbourne/Sydney last update):${NC}\n"
    run_get_client

    if [ $i -eq 3 ]; then
        printf "${YELLOW}At 30 seconds - Melbourne and Sydney should expire soon...${NC}\n"
    fi

    if [ $i -lt 6 ]; then
        printf "Waiting 10 seconds...\n"
        sleep 10
    fi
done

printf "${BLUE}Step 6: Final verification - only Adelaide should remain${NC}\n"
run_get_client

printf "${BLUE}Step 7: Testing server restart with persistence...${NC}\n"
printf "Stopping server...\n"
kill $SERVER_PID
sleep 3

printf "Restarting server...\n"
mvn exec:java -Dexec.mainClass="com.weathersystem.server.AggregationServer" > server.log 2>&1 &
SERVER_PID=$!
sleep 5

printf "Checking if data persisted after restart...\n"
run_get_client

printf "${BLUE}Test completed! Cleaning up...${NC}\n"
kill $SERVER_PID 2>/dev/null

printf "${GREEN}=== Test Summary ===${NC}\n"
printf "✓ File persistence tested\n"
printf "✓ Data expiry (30-second rule) tested\n"
printf "✓ Selective expiry tested (some expire, some remain)\n"
printf "✓ Server restart recovery tested\n\n"
printf "Check server.log for detailed server output\n"
