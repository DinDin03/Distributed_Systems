#!/bin/bash
echo "Running Calculator Client"
cd "$(dirname "$0")/.."
mvn -q compile exec:java -Dexec.mainClass="CalculatorClient"