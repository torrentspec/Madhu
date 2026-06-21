#!/bin/sh

# Find the absolute root directory of the project
APP_HOME=$(pwd)
APP_BASE_NAME=$(basename "$0")

# Identify the active Java path
if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ]; then
    JAVACMD="$JAVA_HOME/bin/java"
else
    JAVACMD="java"
fi

# Safety check for Java presence
if ! command -v "$JAVACMD" >/dev/null 2>&1; then
    echo "ERROR: Java could not be found. Please ensure JDK is installed." >&2
    exit 1
fi

# Establish the path to the runner package
CLASSPATH="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"

# Trigger the native compiler wrapper process
exec "$JAVACMD" "-Dorg.gradle.appname=$APP_BASE_NAME" -jar "$CLASSPATH" "$@"
