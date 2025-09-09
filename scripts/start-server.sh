#!/bin/bash
echo "Starting Calculator Server"
cd "$(dirname "$0")/.."
mvn -q compile exec:java -Dexec.mainClass="CalculatorServer"