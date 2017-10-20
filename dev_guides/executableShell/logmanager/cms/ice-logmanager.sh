#!/bin/bash

export CORE_HOME=/home/ion/cms
export LOG_HOME=/home/ion/cms/core
export LOG_FILE=ice
export MAX_LOGSIZE=$((1024*1024*50))
# export MAX_LOGSIZE=1024
export INTERVAL="1m"
export LOG_DESTINATION=$CORE_HOME/logs
export PRESERVE_FILE_CNT=10

function doJob(){
  while true; do
    sleep $INTERVAL

    FILE="$LOG_HOME"/"$LOG_FILE".log
    SIZE=$(wc -c < $FILE)
    echo -e "EXECUTE LOG MANAGER CURRENT FILE SIZE :: $SIZE  MAX SIZE :: $MAX_LOGSIZE"
    if [ $SIZE -ge $MAX_LOGSIZE ]; then
      cat $FILE > $LOG_DESTINATION/$LOG_FILE.$(date '+%Y%m%d%H%M%S').log
      cat /dev/null > $FILE
    fi
    CURRENT=`date`
    echo -e "DELETING FILES :: $CURRENT"
    RM_IDX=`expr $PRESERVE_FILE_CNT + 1`
    ls -t1 $LOG_DESTINATION | grep ice.*.log | tail -n +$RM_IDX | xargs rm -f
  done
}

echo -e "Started"

if [ $LOG_DESTINATION ]; then
  mkdir -p $LOG_DESTINATION
fi

echo -e "Directory Setting finished"

case $1 in
start)
  echo -e "Execute Log Job"
  doJob &
;;
stop)
  echo "STOP LOG MANAGER..."
;;
esac

exit 0
