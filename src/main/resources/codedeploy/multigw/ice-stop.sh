#!/bin/bash

export JAVA_HOME=java
export APP_HOME=.
export APP_NAME=ice2-core
export WAR_FILE=ice2-core-0.0.1-SNAPSHOT.war
export PROFILE=multigw
export APP_PORT=8080
export WORK_DIR=/home/ion/api/core

echo "Service [$APP_NAME] - [$1] STOP"

if ! pkill -0 -f $WORK_DIR/$WAR_FILE > /dev/null 2>&1
then
    echo "Service [$APP_NAME] is not running. Ignoring shutdown request."
    exit 0
fi

# First, we will try to trigger a controlled shutdown using
# spring-boot-actuator
curl -X POST http://localhost:$APP_PORT/shutdown < /dev/null > /dev/null 2>&1

# Wait until the server process has shut down
attempts=0
while pkill -0 -f $WORK_DIR/$WAR_FILE > /dev/null 2>&1
do
    attempts=$[$attempts + 1]
    if [ $attempts -gt 5 ]
    then
        # We have waited too long. Kill it.
        pkill -f $WORK_DIR/$WAR_FILE > /dev/null 2>&1
    fi
    sleep 1s
done

exit 0
