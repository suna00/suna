#! /bin/bash

export LOG_DIR=/data/ion/logs/icelogs
export FILE_CNT=0
export MAX_CNT=70

echo "Delete file except ice.log and recent $MAX_CNT files"

if [ ! -d $LOG_DIR ]; then
    echo "Making log directory $LOG_DIR"
    mkdir -p $LOG_DIR
else
    echo "A log directory already exists. $LOG_DIR "s
fi


FILE_CNT=`ls $LOG_DIR | wc -l`
if [ $FILE_CNT -gt $MAX_CNT ]; then
	echo "File count is more than max. Remove old files : $MAX_CNT"
	IFS=$'\n' FILES=(`ls -tr $LOG_DIR`)

	OFFSET=$(($FILE_CNT - $MAX_CNT))
	echo "offset :: $OFFSET"

	for i in "${!FILES[@]}"; do
        FILE_IDX=$i
        FILE=${FILES[$FILE_IDX]}
        FILE_NUM=$(($FILE_IDX + 1)) #ice.log 를 살려두기 위해서 1 더함
        echo "$FILE_IDX :: $FILE :: $FILE_NUM"
        # printf "%s\t%s\n" "$i" "$FILE"
        if [ $FILE_NUM -lt $OFFSET ]; then
            echo "DELETE FILE :: $FILE"
            rm $LOG_DIR/$FILE
        fi
    done
else
	echo 'File count is less then max. Nothing to do : $MAX_CNT'
fi

ls $LOG_DIR
exit 0