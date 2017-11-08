#! /bin/bash

# 0. log 폴더 리스트 추출해서 ice_*.log 파일을 다 잡아서 확장자 뺀 파일명 리스트를 만듬
# 1. cat 명령어로 특정 키워드(사용자 Input)가 포함된 문자열을 찾아서 대상 폴더에 같은 이름으로 리디렉션 
# 2. csv 로 만들기 (how)

export LOG_DIR=/data/ion/logs/icelogs
export TARGET_DIR=/data/ion/logs/icelogs/analytics
export SEARCH_TEXT="VotePrtcptHstService > getIpCnt"
export VERSION=0.1
export TIMESTAMP=`date +%Y-%m-%d`
echo -e "Start log2csv Ver.$VERSION ... : execute on $TIMESTAMP"

function mergAllTargetCsv {
	echo -e "$1 $2"
	IFS=$'\n' FILES=(`ls $1 | grep csv`)
	for i in "${!FILES[@]}"; do
	    FILE_IDX=$i
	    FILE=${FILES[$FILE_IDX]}
	    cat $1/$FILE >> $1/$2_merged.csv
	done
}

echo -e "Validate directories..."
# 로그 디렉토리 유효성 검사
if [ ! -d $LOG_DIR ]; then
	echo -e "[ $LOG_DIR ] does not exist"
	exit 1
fi

# 대상 디렉토리 유효성 검사 
if [ ! -d $TARGET_DIR ]; then
	mkdir -p $TARGET_DIR
	if [ $? -ne 0 ]; then
		echo "Failed to create [ $TARGET_DIR ]"
		exit 1
	fi
fi

echo -e "Filter log files with String :: $SEARCH_TEXT"
# 로그 복사
IFS=$'\n' FILES=(`ls -tr $LOG_DIR`)
for i in "${!FILES[@]}"; do
    FILE_IDX=$i
    FILE=${FILES[$FILE_IDX]}
    # ice_ 를 포함한다면 문자열 검색명령어를 실행한다
    if [[ $FILE == *"ice_"* ]]; then
    	cat $FILE | grep $SEARCH_TEXT > $TARGET_DIR/$FILE
    fi
done

echo -e "Convert Logs to CSVs"
# 해당 데이터로 CSV 추출하기
IFS=$'\n' FILES=(`ls -tr $TARGET_DIR`)
for i in "${!FILES[@]}"; do
    FILE_IDX=$i
    FILE=${FILES[$FILE_IDX]}
    # 근본적으로 원천 데이터는 이렇게 생김 
    # 2017-11-07 00:19:16.898 INFO  311495193 - [http-nio-8080-exec-19795] n.i.i.c.v.VotePrtcptHstService : VotePrtcptHstService > getIpCnt > 110.44.120.5>20171107>800103 > 0
    # 2017-11-07 00:34:21.514,139.0.54.180,20171107,800103
    CSV_FILE=${FILE/log/csv}
    cat $TARGET_DIR/$FILE | awk '{temp=$13; gsub(">",",",$13); printf("%s %s,%s\n",$1,$2,$13)}' > $TARGET_DIR/$CSV_FILE
done

echo -e "Merging CSVs"
# merge all
mergAllTargetCsv $TARGET_DIR $TIMESTAMP

echo -e "log2csv process finished"
exit 0;
