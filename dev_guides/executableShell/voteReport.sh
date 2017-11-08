#! /bin/bash

export SLACK_URL="https://hooks.slack.com/services/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"
export MYSQL_URL="authdevdb.cfwzre2kzbc1.ap-northeast-2.rds.amazonaws.com"
export MYSQL_USER="cjmwave"
export MYSQL_PW="cjmwave0!@#"
export MYSQL_SCHEMA="ice2"
export QUERY_RS=""
export DATA_PAYLOAD=""
export SLACK_MSG=""

echo -e "Sending vote report to Slack"

echo $MYSQL_USER::$MYSQL_PW
echo $MYSQL_URL

QUERY_RS=`mysql -h $MYSQL_URL -u$MYSQL_USER -p$MYSQL_PW $MYSQL_SCHEMA -e "SELECT '일자별 투표 건수 전체' FROM DUAL UNION ALL SELECT CONCAT(date_format(voteDate,'%Y-%m-%d'),' : ', CAST(format(count(*),0) AS char),'건') AS voteCnt FROM 800100_voteHstByMbr WHERE voteDate=DATE_FORMAT(DATE_ADD(NOW(), INTERVAL -1 DAY), '%Y%m%d') UNION ALL SELECT '일자별 투표 건수(Qoo10 제외)' FROM DUAL UNION ALL SELECT CONCAT(date_format(voteDate,'%Y-%m-%d'),' : ', CAST(format(count(*),0) AS char),'건') AS voteCnt FROM 800100_voteHstByMbr WHERE voteDate=DATE_FORMAT(DATE_ADD(NOW(), INTERVAL -1 DAY), '%Y%m%d') and mbrId not like '%>qoo10::%' UNION ALL SELECT '일자별 투표 건수(Qoo10)' FROM DUAL UNION ALL SELECT CONCAT(date_format(voteDate,'%Y-%m-%d'),' : ', CAST(format(count(*),0) AS char),'건') AS voteCnt FROM 800100_voteHstByMbr WHERE voteDate=DATE_FORMAT(DATE_ADD(NOW(), INTERVAL -1 DAY), '%Y%m%d') and mbrId like '%>qoo10::%' UNION ALL SELECT '일자별 회원 가입자수(Qoo10 제외)' FROM DUAL UNION ALL SELECT CONCAT(date_format(sbscDt,'%Y-%m-%d'),' : ', CAST(format(count(*),0) AS char),'명') AS mbrCnt FROM mbrInfo WHERE sbscDt BETWEEN CONCAT(DATE_FORMAT(DATE_ADD(NOW(), INTERVAL -1 DAY), '%Y%m%d'),'000000','000') and CONCAT(DATE_FORMAT(DATE_ADD(NOW(), INTERVAL -1 DAY), '%Y%m%d'),'235959','999') and snsTypeCd != '10' UNION ALL SELECT '일자별 회원 가입자수(Qoo10)' FROM DUAL UNION ALL SELECT CONCAT(date_format(sbscDt,'%Y-%m-%d'),' : ', CAST(format(count(*),0) AS char),'명') AS mbrCnt FROM mbrInfo WHERE sbscDt BETWEEN CONCAT(DATE_FORMAT(DATE_ADD(NOW(), INTERVAL -1 DAY), '%Y%m%d'),'000000','000') and CONCAT(DATE_FORMAT(DATE_ADD(NOW(), INTERVAL -1 DAY), '%Y%m%d'),'235959','999') and snsTypeCd = '10' UNION ALL SELECT 'K-POP POLL 투표수' FROM DUAL UNION ALL SELECT CONCAT(date_format(voteDate,'%Y-%m-%d'),' : ', CAST(format(count(*),0) AS char),'건') AS voteCnt FROM 800103_voteHstByMbr WHERE voteDate=DATE_FORMAT(DATE_ADD(NOW(), INTERVAL -1 DAY), '%Y%m%d') UNION ALL SELECT 'K-POP POLL 항목별 투표수' FROM DUAL UNION ALL SELECT CONCAT(date_format(voteDate,'%Y-%m-%d'), ' : ', voteItemseq, ' : ', CAST(format(count(*),0) AS char),'건') AS voteCnt FROM 800103_voteItemHstByMbr WHERE voteDate=DATE_FORMAT(DATE_ADD(NOW(), INTERVAL -1 DAY), '%Y%m%d') group by voteItemseq;"`

IFS=$'\n\t' LINES=($QUERY_RS)
for i in "${!LINES[@]}"; do
    LINE_IDX=$i
    LINE=${LINES[$LINE_IDX]}
    # echo $LINE
    DATA_PAYLOAD="$DATA_PAYLOAD"\\n"$LINE"
done

# echo $DATA_PAYLOAD
SLACK_MSG='{"text": "'$DATA_PAYLOAD'"}'
echo "curl -X POST -H 'Content-type: application/json' --data '$SLACK_MSG' $SLACK_URL"
curl -X POST -H 'Content-type: application/json' --data $SLACK_MSG $SLACK_URL

exit 0
