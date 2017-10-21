#!/bin/bash
export PROJECT_HOME=/home/ion/cms
export LOG_HOME=$PROJECT_HOME/core
#export MAX_LOGSIZE=$((1024*1024*50))
export MAX_LOGSIZE=1024
export INTERVAL="1m"
export LOG_DESTINATION=$PROJECT_HOME/logs
export PRESERVE_FILE_CNT=10
export LOG_FILE
export ICE_PID
export LOGGING_PID
export DIRCHECK_PID
export TAIL_PID

function doJob() {

  tail -f /proc/$ICE_PID/fd/1 2>&1 >> $LOG_FILE &
  # TAIL_PID=`ps -ef --sort=start_time | grep "tail -f /proc/$ICE_PID/fd/1" | awk {'print $2'} | head -1`
  TAIL_PID=$!
  echo "INITIAL TAIL PID :: $TAIL_PID"

  while true; do
    sleep $INTERVAL
    SIZE=0
    echo "LOGFILE SIZE CHECK :: $LOG_FILE"
    if [ -f $LOG_FILE ]; then
      SIZE=$(wc -c < $LOG_FILE)
      echo "CURRENT SIZE $SIZE"

      if [ $SIZE -ge $MAX_LOGSIZE ]; then
        echo -e "REACHED FILE MAX :: REPLACE FILE
        MAX :: $MAX_LOGSIZE
        CURRENT :: $SIZE"

        # prepare next loop
        LOG_FILE=$LOG_DESTINATION/ice.$(date '+%Y%m%d%H%M%S').log
        tail -f /proc/$ICE_PID/fd/1 2>&1 >> $LOG_FILE &
        # TEMP_TAIL_PID=`ps -ef --sort=start_time | grep "tail -f /proc/$ICE_PID/fd/1" | awk {'print $2'} | head -2 | sed -n 2p`
        TEMP_TAIL_PID=$!

        echo -e "START NEW JOB :: $TEMP_TAIL_PID
        KILL FORMER JOB :: $TAIL_PID"
        kill -9 $TAIL_PID
        TAIL_PID=$TEMP_TAIL_PID
        echo -e "ROTATE ENDS"
      fi
    else
      echo "NO FILE FOUND. CREATE ONE"
      tail -f /proc/$ICE_PID/fd/1 2>&1 >> $LOG_FILE &
      TAIL_PID=`ps -ef --sort=start_time | grep "tail -f /proc/$ICE_PID/fd/1" | awk {'print $2'} | head -1`
      echo -e "START NEW JOB :: $TAIL_PID"
    fi
  done
}

function manageCounts() {
  while true; do
    sleep $INTERVAL
    CURRENT=`date`
    echo -e "CHECK AND DELETING FILES :: $CURRENT"
    RM_IDX=`expr $PRESERVE_FILE_CNT -1`
    ls -t1 $LOG_DESTINATION/ice.*.log | tail -n +$RM_IDX | xargs rm -f
  done
}

function killChildren() {
  PPID=$1
  IFS=$'\n' ARR=(`pgrep -P $1`)
  for SUBPID in "${ARR[@]}"; do
    IFS=$'\n' ARR2=(`pgrep -P $SUBPID`)
    if [ ${#ARR2[@]} -gt 0 ]; then
      killChildren $SUBPID
    fi
    echo "REMOVE PID :: $SUBPID"
    kill -9 $SUBPID
  done
}


ICE_PID=`ps -ef | grep ice2-core-0.0.1-SNAPSHOT.war | awk '{print $2}' 2>&1 | head -1`

# make home folder
if [ ! -d "$LOG_DESTINATION" ]; then
  echo "MAKE LOGS HOME DIRECTORY :: $LOG_DESTINATION"
  mkdir $LOG_DESTINATION
fi


# dojob by command
case $1 in
start)
  echo "LOG MANAGER STARTED :: ICE_PID = $ICE_PID"
  LOG_FILE=$LOG_DESTINATION/ice.$(date '+%Y%m%d%H%M%S').log
  doJob &
  LOGGING_PID=$!
  manageCounts &
  DIRCHECK_PID=$!
  echo -e "Sub Processs
  Logging : $LOGGING_PID
  Dir Checking : $DIRCHECK_PID"
;;
stop)
  #echo $$
  echo "DELETE MANUAL, SINCE WE DOES NOT BELONG TO SAME TRANSACTION"
  exit 0
;;
esac

exit 0