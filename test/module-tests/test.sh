#!/bin/bash
JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d\" -f 2)
echo $JAVA_VERSION
if [ "$JAVA_VERSION" > "1.8" ]; then
echo "greater than 8"
fi
