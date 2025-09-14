#!/bin/bash

# Multi-Server Startup Script for Weather Aggregation System
# This script starts 3 independent AggregationServer instances for fault tolerance

echo "=== Starting Multiple Weather Aggregation Servers ==="

# Clean up any existing server processes
echo "Cleaning up existing server processes..."
pkill -f "AggregationServer" 2>/dev/null || true

# Create data directories for each server
mkdir -p data_4567 data_4568 data_4569

# Start Server 1 (Primary) on port 4567
echo "Starting Server 1 on port 4567..."
java -cp target/classes com.weathersystem.server.AggregationServer 4567 > server_4567.log 2>&1 &
SERVER1_PID=$!
echo "Server 1 started with PID: $SERVER1_PID"

# Wait a moment for server to initialize
sleep 2

# Start Server 2 (Backup) on port 4568
echo "Starting Server 2 on port 4568..."
java -cp target/classes com.weathersystem.server.AggregationServer 4568 > server_4568.log 2>&1 &
SERVER2_PID=$!
echo "Server 2 started with PID: $SERVER2_PID"

# Wait a moment for server to initialize
sleep 2

# Start Server 3 (Backup) on port 4569
echo "Starting Server 3 on port 4569..."
java -cp target/classes com.weathersystem.server.AggregationServer 4569 > server_4569.log 2>&1 &
SERVER3_PID=$!
echo "Server 3 started with PID: $SERVER3_PID"

echo ""
echo "=== All Servers Started Successfully ==="
echo "Server 1: localhost:4567 (PID: $SERVER1_PID)"
echo "Server 2: localhost:4568 (PID: $SERVER2_PID)"
echo "Server 3: localhost:4569 (PID: $SERVER3_PID)"
echo ""
echo "Log files:"
echo "  - server_4567.log"
echo "  - server_4568.log"
echo "  - server_4569.log"
echo ""
echo "To test multiple servers with clients, use:"
echo "  Content Server: java -cp target/classes com.weathersystem.client.ContentServer \"localhost:4567,localhost:4568,localhost:4569\" data/weather1.txt"
echo "  GET Client:     java -cp target/classes com.weathersystem.client.GETClient \"localhost:4567,localhost:4568,localhost:4569\""
echo ""
echo "To stop all servers, run: ./stop_servers.sh"

# Keep script running to monitor servers
echo "Press Ctrl+C to stop all servers..."

# Function to cleanup on script exit
cleanup() {
    echo ""
    echo "Stopping all servers..."
    kill $SERVER1_PID 2>/dev/null || true
    kill $SERVER2_PID 2>/dev/null || true
    kill $SERVER3_PID 2>/dev/null || true

    # Wait for graceful shutdown
    sleep 2

    # Force kill if still running
    kill -9 $SERVER1_PID 2>/dev/null || true
    kill -9 $SERVER2_PID 2>/dev/null || true
    kill -9 $SERVER3_PID 2>/dev/null || true

    echo "All servers stopped."
}

# Set up signal handlers
trap cleanup INT TERM

# Wait for all background processes
wait