#!/bin/bash

# Stop all Weather Aggregation Server instances

echo "=== Stopping All Weather Aggregation Servers ==="

# Find and kill all AggregationServer processes
SERVER_PIDS=$(pgrep -f "AggregationServer")

if [ -z "$SERVER_PIDS" ]; then
    echo "No server processes found."
else
    echo "Found server processes: $SERVER_PIDS"
    echo "Stopping servers gracefully..."

    # Send SIGTERM for graceful shutdown
    kill $SERVER_PIDS 2>/dev/null || true

    # Wait for graceful shutdown
    sleep 3

    # Check if any are still running and force kill
    REMAINING_PIDS=$(pgrep -f "AggregationServer")
    if [ ! -z "$REMAINING_PIDS" ]; then
        echo "Force killing remaining processes: $REMAINING_PIDS"
        kill -9 $REMAINING_PIDS 2>/dev/null || true
    fi

    echo "All server processes stopped."
fi

echo "Multi-server shutdown complete."