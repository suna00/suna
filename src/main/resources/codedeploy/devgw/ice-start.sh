#!/bin/bash

export JAVA_HOME=java
export APP_HOME=.
export APP_NAME=ice2-core
export WAR_FILE=ice2-core-0.0.1-SNAPSHOT.war
export PROFILE=devgw
export APP_PORT=8080
export WORK_DIR=/home/ion/api/core

echo "Service [$APP_NAME] - [$1] START"

echo "    JAVA_HOME=$JAVA_HOME"
echo "    APP_HOME=$APP_HOME"
echo "    APP_NAME=$APP_NAME"
echo "    WAR_FILE=$WAR_FILE"
echo "    APP_PORT=$APP_PORT"
echo "    PROFILE=$PROFILE"

if pkill -0 -f $WORK_DIR/$WAR_FILE > /dev/null 2>&1
then
    echo "Service [$APP_NAME] is already running. Ignoring startup request."
    exit 1
fi

echo "Starting application..."
nohup $JAVA_HOME -jar -XX:+UseParNewGC -XX:+UseConcMarkSweepGC -XX:-CMSParallelRemarkEnabled -Xmx4g -Xms4g -Dspring.profiles.active=$PROFILE $WORK_DIR/$WAR_FILE \
    1> /dev/null > 2>&1 &

exit 0
