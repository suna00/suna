#!/bin/bash

export LOG_HOME=/home/ice/projects/cj/core
export LOG_FILE=ice
export MAX_LOGSIZE=$((1024*1024*50))
# export MAX_LOGSIZE=1024
export INTERVAL="1m"
export LOG_DESTINATION=$LOG_HOME/logs
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
    ls -t1 $LOG_DESTINATION/ice.*.log | tail -n +$RM_IDX | xargs rm -f
  done
}

case $1 in
start)
  doJob &
;;
stop)
  echo "STOP LOG MANAGER..."
;;
esac

exit 1