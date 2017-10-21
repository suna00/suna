#!/bin/bash

export JAVA_HOME=java
export APP_HOME=.
export APP_NAME=ice2-core
export WAR_FILE=ice2-core-0.0.1-SNAPSHOT.war
export PROFILE=multicache
export APP_PORT=8080
export WORK_DIR=/home/ion/api/core
export LOG_DIR=/data/ion/logs

echo "Service [$APP_NAME] - [$1] START"

echo "    JAVA_HOME=$JAVA_HOME"
echo "    APP_HOME=$APP_HOME"
echo "    APP_NAME=$APP_NAME"
echo "    WAR_FILE=$WAR_FILE"
echo "    APP_PORT=$APP_PORT"
echo "    PROFILE=$PROFILE"

if [ ! -d $LOG_DIR ]; then
  mkdir -p $LOG_DIR
fi

if pkill -0 -f $WORK_DIR/$WAR_FILE > /dev/null 2>&1
then
    echo "Service [$APP_NAME] is already running. Ignoring startup request."
    exit 1
fi
echo "Starting application..."
nohup $JAVA_HOME -jar -Dspring.profiles.active=$PROFILE $WORK_DIR/$WAR_FILE \
    < /dev/null > $LOG_DIR/ice.log 2>&1 &

exit 0
