#!/bin/bash

export JAVA_HOME=java
export APP_HOME=.
export APP_NAME=ice2-core
export WAR_FILE=ice2-core-0.0.1-SNAPSHOT.war
export PROFILE=dev
export APP_PORT=8080

echo "Service [$APP_NAME] - [$1]"

echo "    JAVA_HOME=$JAVA_HOME"
echo "    APP_HOME=$APP_HOME"
echo "    APP_NAME=$APP_NAME"
echo "    WAR_FILE=$WAR_FILE"
echo "    APP_PORT=$APP_PORT"
echo "    PROFILE=$PROFILE"

function start {
    if pkill -0 -f $WAR_FILE > /dev/null 2>&1
    then
        echo "Service [$APP_NAME] is already running. Ignoring startup request."
        exit 1
    fi
    echo "Starting application..."
    nohup $JAVA_HOME -jar $WAR_FILE \
        -Dspring.profiles.active=$PROFILE   \
        < /dev/null > $APP_HOME/ice.log 2>&1 &
}

function stop {
    if ! pkill -0 -f $WAR_FILE > /dev/null 2>&1
    then
        echo "Service [$APP_NAME] is not running. Ignoring shutdown request."
        exit 1
    fi

    # First, we will try to trigger a controlled shutdown using
    # spring-boot-actuator
    curl -X POST http://localhost:$APP_PORT/shutdown < /dev/null > /dev/null 2>&1

    # Wait until the server process has shut down
    attempts=0
    while pkill -0 -f $WAR_FILE > /dev/null 2>&1
    do
        attempts=$[$attempts + 1]
        if [ $attempts -gt 5 ]
        then
            # We have waited too long. Kill it.
            pkill -f $WAR_FILE > /dev/null 2>&1
        fi
        sleep 1s
    done
}

case $1 in
start)
    start
;;
stop)
    stop
;;
restart)
    stop
    start
;;
esac
exit 0