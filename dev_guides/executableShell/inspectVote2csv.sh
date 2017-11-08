#! /bin/bash

# 0. log 폴더 리스트 추출해서 ice_*.log 파일을 다 잡아서 확장자 뺀 파일명 리스트를 만듬
# 1. cat 명령어로 특정 키워드(사용자 Input)가 포함된 문자열을 찾아서 대상 폴더에 같은 이름으로 리디렉션
# 2. csv 로 만들기 (how)
# 파라미터는 003 혹은 007
# 003 의 경우는 http 파라미터를 전부 끊어서 순차적으로 csv 로 만든다
# 007 의 경우는 json 파라미터를 통짜로 저장할 수 있도록 처리한다

# export LOG_DIR=/Users/juneyoungoh/Downloads/testlog
export LOG_DIR=/data/ion/logs/icelogs
export TARGET_DIR=$LOG_DIR/vote_abusing
export SEARCH_TEXT="/api/vote/IfMwv003"
export VERSION=0.1
export TIMESTAMP=`date +%Y-%m-%d`
export HEADER003="log_date,voteQueiSeq,langCd,voteSeq,snsKey,snsTypeCd,connIpAdr"
export HEADER007="log_date,voteResult,langCd,connIpAdr"

echo -e "Start FilterLogging Ver.$VERSION ... : execute on $TIMESTAMP"

date

function mergAllTargetCsv {
	TARGET_CSV_NAME="$1"/"$2"
	if [[ "$SEARCH_TEXT" == *"003"* ]]; then
		TARGET_CSV_NAME="$TARGET_CSV_NAME"_003_merge.csv
		echo "$HEADER003" > $TARGET_CSV_NAME
	elif [[ "$SEARCH_TEXT" == *"007"* ]]; then
		TARGET_CSV_NAME="$TARGET_CSV_NAME"_007_merge.csv
		echo "$HEADER007" > $TARGET_CSV_NAME
	fi

	IFS=$'\n' FILES=(`ls $1 | grep -e "csv" | grep -e "ice_"`)
	for i in "${!FILES[@]}"; do
	    FILE_IDX=$i
	    FILE=${FILES[$FILE_IDX]}
	    echo "cat $1/$FILE >> $TARGET_CSV_NAME"
	    cat $1/$FILE >> $TARGET_CSV_NAME
	done
}

# Data like... 2017-11-06 23:50:21.437,voteQueiSeq=7000377,langCd=eng,voteSeq=800103,snsKey=2917406694,snsTypeCd=5,connIpAdr=77.247.181.162,
function getParamStrValue2commaStr {
	IFS=",";
	temp2=$1;
	temp3="";
	TOP_IDX="0";
	for KV in $temp2; do
		if [ $TOP_IDX -eq 0 ]; then
			if [ ${#KV} -gt 0 ]; then
				temp3=$KV
			fi
		else
			IFS="=";
			IDX="0";
			for V in $KV; do
				if [ $IDX -eq 1 ]; then
					temp3=$temp3,$V
				fi
				IDX=$(($IDX+1))
			done
		fi
		TOP_IDX=$(($TOP_IDX+1))
	done
	if [ ${#temp3} -gt 0 ]; then
		echo "$temp3"
	fi
}

function clearDummy {
	echo -e "Remove Dummy data"
	rm -rf $TARGET_DIR/*
}

function man {
	echo -e "convertVote2csv.sh 003
	convertVote2csv.sh 007"
}

# ==========================================================================================
# ==========================================================================================
# ========================================= BEGINS =========================================
# ==========================================================================================
# ==========================================================================================


## 0. Validate Parameter : default is "/api/vote/IfMwv003", For "/api/vote/IfMwv007", provide 007 as parameter
echo "$TARGET_DIR : $1"
if [ $# -lt 1 ]; then
	echo -e "parameter does not provided : default is $SEARCH_TEXT"
	TARGET_DIR="$TARGET_DIR"_003
else
	if [[ $1 == *"007"* ]]; then
		echo -e "set variable SEARCH_TEXT to $1"
		SEARCH_TEXT="/api/vote/IfMwv007"
		TARGET_DIR="$TARGET_DIR"_007
	elif [[ $1 == *"003"* ]]; then
		echo -e "Use default as $SEARCH_TEXT"
		TARGET_DIR="$TARGET_DIR"_003
	elif [[ $1 == "man" ]]; then
		man
		exit 0
	else
		echo -e "Uknown Pattern $1"
		exit 1
	fi
fi


## 1. Validate file system
echo -e "Validate directories..."
if [ ! -d "$LOG_DIR" ]; then
	echo -e "[ $LOG_DIR ] does not exist"
	exit 1
fi

if [ ! -d "$TARGET_DIR" ]; then
	mkdir -p "$TARGET_DIR"
	if [ $? -ne 0 ]; then
		echo "Failed to create [ $TARGET_DIR ]"
		exit 1
	fi
fi


## 2. Filtering log files with cat command
echo -e "Filter log files with String :: $SEARCH_TEXT"
IFS=$'\n' FILES=(`ls -tr $LOG_DIR`)
for i in "${!FILES[@]}"; do
    FILE_IDX="$i"
    FILE=${FILES[$FILE_IDX]}
    # "ice_*.log" is log file naming convention. For filter other files...
    if [[ $FILE == *"ice_"* ]]; then
    	# echo "cat $LOG_DIR/$FILE | grep $SEARCH_TEXT > $TARGET_DIR/$FILE"
    	cat "$LOG_DIR"/"$FILE" | grep "$SEARCH_TEXT" > "$TARGET_DIR"/"$FILE"
    fi
done

## 3. Converting filtered log data to csv form
echo -e "Convert Logs to CSVs"
IFS=$'\n' FILES=(`ls -tr $TARGET_DIR`)

if [[ $SEARCH_TEXT == *"003"* ]]; then
	echo "Executing JOB FOR 003"
	for i in "${!FILES[@]}"; do
	    FILE_IDX=$i
	    FILE=${FILES[$FILE_IDX]}
	    echo "FILE INSERTED : $FILE"
	    if [[ $FILE == *'log' ]]; then
		    # When 003, data like below ...
		    # 2017-11-06 06:48:35.484 ERROR 248933321 - [http-nio-8080-exec-9490] net.ion.ice.core.api.ApiController : api error : /api/vote/IfMwv003
			# 2017-11-06 06:48:35.509 INFO  248933346 - [http-nio-8080-exec-10051] net.ion.ice.core.context.ApiContext : api logging : internal-g-p-api-Internal-elb-1137130812.ap-northeast-2.elb.amazonaws.com /api/vote/IfMwv003 voteQueiSeq=7000377,&langCd=eng&voteSeq=800103&snsKey=2080299289&snsTypeCd=6&connIpAdr=77.247.181.162&
			IFS=$'\n' TEXT_VALUES=(`cat $TARGET_DIR/$FILE | awk '{temp=$14; gsub(",","",$14); gsub("&",",",$14); printf("%s %s,%s\n",$1,$2,$14)}'`)
			for i in "${!TEXT_VALUES[@]}"; do
			    IDX=$i
			    TEXT_VALUE=${TEXT_VALUES[$IDX]}
			    CSV_FILE=${FILE/.log/_003.csv}
			    echo `getParamStrValue2commaStr $TEXT_VALUE` >> "$TARGET_DIR"/"$CSV_FILE"
			done
		fi
		echo "FILE JOB DONE"
	done
elif [[ $SEARCH_TEXT == *"007"* ]]; then
	echo "Executing JOB FOR 007"
	for i in "${!FILES[@]}"; do
	    FILE_IDX=$i
	    FILE=${FILES[$FILE_IDX]}
	    # When 007, data like below ...
		# 2017-11-06 06:48:35.509 INFO  248933346 - [http-nio-8080-exec-10051] net.ion.ice.core.context.ApiContext : api logging : internal-g-p-api-Internal-elb-1137130812.ap-northeast-2.elb.amazonaws.com /api/vote/IfMwv007 voteResult=[{"sersVoteSeq":800100,"voteSeq":800104,"voteItemSeq":"7000406","prtcpMbrId":"2>447697713"},{"sersVoteSeq":800100,"voteSeq":800105,"voteItemSeq":"7000411","prtcpMbrId":"2>447697713"},{"sersVoteSeq":800100,"voteSeq":800106,"voteItemSeq":"7000416","prtcpMbrId":"2>447697713"},{"sersVoteSeq":800100,"voteSeq":800107,"voteItemSeq":"7000424","prtcpMbrId":"2>447697713"},{"sersVoteSeq":800100,"voteSeq":800108,"voteItemSeq":"7000431","prtcpMbrId":"2>447697713"},{"sersVoteSeq":800100,"voteSeq":800109,"voteItemSeq":"7000432","prtcpMbrId":"2>447697713"},{"sersVoteSeq":800100,"voteSeq":800110,"voteItemSeq":"7000440","prtcpMbrId":"2>447697713"},{"sersVoteSeq":800100,"voteSeq":800111,"voteItemSeq":"7000442","prtcpMbrId":"2>447697713"},{"sersVoteSeq":800100,"voteSeq":800112,"voteItemSeq":"7000451","prtcpMbrId":"2>447697713"},{"sersVoteSeq":800100,"voteSeq":800113,"voteItemSeq":"7000457","prtcpMbrId":"2>447697713"},{"sersVoteSeq":800100,"voteSeq":800114,"voteItemSeq":"7000460","prtcpMbrId":"2>447697713"},{"sersVoteSeq":800100,"voteSeq":800115,"voteItemSeq":"7000467","prtcpMbrId":"2>447697713"},{"sersVoteSeq":800100,"voteSeq":800116,"voteItemSeq":"7000468","prtcpMbrId":"2>447697713"},{"sersVoteSeq":800100,"voteSeq":800117,"voteItemSeq":"7000476","prtcpMbrId":"2>447697713"},{"sersVoteSeq":800100,"voteSeq":800118,"voteItemSeq":"7000478","prtcpMbrId":"2>447697713"},{"sersVoteSeq":800100,"voteSeq":800119,"voteItemSeq":"7000483","prtcpMbrId":"2>447697713"},{"sersVoteSeq":800100,"voteSeq":800120,"voteItemSeq":"7000492","prtcpMbrId":"2>447697713"},{"sersVoteSeq":800100,"voteSeq":800101,"voteItemSeq":"7000323","prtcpMbrId":"2>447697713"},{"sersVoteSeq":800100,"voteSeq":800102,"voteItemSeq":"7000353","prtcpMbrId":"2>447697713"}]&langCd=eng&connIpAdr=1.197.197.201&
		echo "FILE INSERTED : $FILE"
	    if [[ $FILE == *'log' ]]; then
			IFS=$'\n' TEXT_VALUES=(`cat $TARGET_DIR/$FILE | awk '{temp=$14; gsub(",","|",$14); gsub("&",",",$14); printf("%s %s,%s\n",$1,$2,$14)}'`)
			for i in "${!TEXT_VALUES[@]}"; do
			    IDX=$i
			    TEXT_VALUE=${TEXT_VALUES[$IDX]}
			    CSV_FILE=${FILE/.log/_007.csv}
			    echo `getParamStrValue2commaStr $TEXT_VALUE` >> "$TARGET_DIR"/"$CSV_FILE"
			done
		fi
		echo "FILE JOB DONE"
	done
fi

echo -e "Merging CSVs"
mergAllTargetCsv $TARGET_DIR $TIMESTAMP

echo -e "FilterLogging process finished"
date

exit 0;