package net.ion.ice.cjmwave.votePrtcptHst;

import net.ion.ice.core.context.ExecuteContext;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeService;
import net.ion.ice.core.node.NodeUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Service("voteDayCntryService")
public class VoteDayCntryService {

    public static final String VOTE_BAS_INFO = "voteBasInfo";
    public static final String VOTE_ITEM_INFO = "voteItemInfo";
    public static final String MBR_INFO = "mbrInfo";

    private JdbcTemplate jdbcTemplate;

    @Autowired
    private NodeService nodeService;

    private Logger logger = Logger.getLogger(VoteDayCntryService.class);


    public void voteBasStatsDaySetNum(ExecuteContext context) {

        if (jdbcTemplate==null) {
            jdbcTemplate = NodeUtils.getNodeBindingService().getNodeBindingInfo(VOTE_BAS_INFO).getJdbcTemplate();
        }

        int limitCnt = 100;

        Date now = new Date();
        //String voteDay = DateFormatUtils.format(now, "yyyyMMdd");
        String voteDay = "20171016"; //test 용
        String voteDateTime = DateFormatUtils.format(now, "yyyyMMddHHmmss");

        try {

            // 투표 기간안에 있는 모든 VoteBasInfo 조회
            List<Node> voteBasInfoList = NodeUtils.getNodeList(VOTE_BAS_INFO, "pstngStDt_below=" + voteDateTime + "&pstngFnsDt_above="+ voteDateTime);
            logger.info("===============> voteBasInfoList size :: " + voteBasInfoList.size() );

            for (Node voteBasInfo : voteBasInfoList) {

                String voteSeq = voteBasInfo.getId().toString();
                logger.info("===============> voteBasInfo - voteSeq :: " + voteSeq );

                String tableNm = voteSeq+"_voteHstByMbr";

                //voteSeq,voteDay별로 스케쥴 돌리고 나서 해당 _voteHstByMbr 테이블에 마지막 seq를 voteBasStatsByDayLastSeq에 저장한다.
                //스케쥴 시작시에 voteBasStatsByDayLastSeq에에 저장된 seq가 있는지 조회하고 없을때는 신규!
                Integer lastSeq = 0;
                Map<String, Object> lastSeqInfo = getVoteBasByDayLstSeq(voteSeq, voteDay);
                if(lastSeqInfo != null){
                    lastSeq = Integer.parseInt(lastSeqInfo.get("seq").toString());
                }

                List<Map<String, Object>> voteMbrList = getVoteHstByMbrList(tableNm, voteDay, lastSeq, limitCnt);
                if(voteMbrList != null){

                    Integer voteMbrListSize = voteMbrListSize = voteMbrList.size();
                    //1. voteSeq&일자별 테이블 voteNum 업데이트
                    if(lastSeqInfo == null){
                        insertVoteBasDay(voteMbrListSize, voteSeq, voteDay, "anonymous", voteDateTime);
                        logger.info("===============> insertVoteBasDay :: " + voteMbrListSize);
                    }else{
                        updateVoteBasDay(voteMbrListSize, voteDateTime, voteSeq, voteDay);
                        logger.info("===============> updateVoteBasDay :: " + voteMbrListSize);
                    }

                    //2. voteSeq&일자&국가별 테이블 voteNum 업데이트
                    for(int i=0; i<voteMbrListSize; i++){
                        Map<String, Object> mapData = voteMbrList.get(i);
                        String mbrId = mapData.get("mbrId").toString();

                        //회원정보의 국가 정보 가져온다
                        Node mbrInfo = NodeUtils.getNodeService().read(MBR_INFO, mbrId);
                        String cntryCd = mbrInfo.getStringValue("cntryCd");
                        Integer cntryCheckCnt = getVoteBasCntryCount(voteSeq, voteDay, cntryCd);

                        //voteSeq&일자&국가별 테이블 voteNum 업데이트
                        if(cntryCheckCnt > 0){
                            updateVoteCntryDay(voteDateTime, voteSeq, voteDay, cntryCd);
                        }else{
                            insertVoteCntryDay(voteSeq, voteDay, cntryCd, 1, "anonymous", voteDateTime);
                        }
                    }


                    //해당 _voteHstByMbr 테이블에 마지막 seq를 저장
                    Integer lastListSeq = Integer.parseInt(voteMbrList.get(voteMbrListSize-1).get("seq").toString());
                    if(lastSeqInfo == null){
                        insertVoteBasByDayLstSeq(lastListSeq, voteSeq, voteDay);
                    }else{
                        updateVoteBasByDayLstSeq(lastListSeq, voteSeq, voteDay);
                    }

                }else{
                    logger.info("===============> 해당 테이블에 일치하는 데이터 없어~~~ :: " + tableNm );
                }

            }

        }catch (Exception e) {
            logger.error("Failed to voteDayCntryService voteBasStatsDaySetNum");
        }

    }



    //voteSeq&투표일자(yyyymmdd)별 카운트한 마지막 seq
    private Map<String, Object> getVoteBasByDayLstSeq(String voteSeq, String voteDay) {
        String selectQuery = "SELECT voteDay, voteSeq, seq FROM voteBasStatsByDayLastSeq WHERE voteSeq=? AND voteDay=?";
        try {
            Map retMap = jdbcTemplate.queryForMap(selectQuery, voteSeq, voteDay);
            return retMap;
        } catch (Exception e) {
            return null;
        }
    }

    //voteSeq&투표일자(yyyymmdd)별 카운트한 마지막 seq insert
    private void insertVoteBasByDayLstSeq(Integer seq, String voteSeq, String voteDay) {
        String insertQuery = "INSERT INTO voteBasStatsByDayLastSeq (seq, voteSeq, voteDay) VALUES(?,?,?)";
        jdbcTemplate.update(insertQuery, seq, voteSeq, voteDay);
    }

    //voteSeq&투표일자(yyyymmdd)별 카운트한 마지막 seq update
    private void updateVoteBasByDayLstSeq(Integer seq, String voteSeq, String voteDay) {
        String updateQuery = "UPDATE voteBasStatsByDayLastSeq SET seq= ? WHERE voteSeq=? AND voteDay=?";
        jdbcTemplate.update(updateQuery, seq, voteSeq, voteDay);
    }


    //해당 voteSeq_voteHstByMbr 테이블에서 리스트 조회
    private List<Map<String, Object>> getVoteHstByMbrList(String tableNm, String voteDate, Integer lastSeq, Integer limitCnt) {
        //select * from `800041_voteHstByMbr` WHERE voteDate='20171006' and seq > 50  ORDER BY seq LIMIT 10
        String selectQuery = "SELECT seq, voteDate, mbrId FROM ? WHERE voteDate=? AND seq>? ORDER BY seq LIMIT ?";
        try {
            List list = jdbcTemplate.queryForList(selectQuery, tableNm, voteDate, lastSeq, limitCnt);
            return list;
        } catch (Exception e) {
            return null;
        }
    }



    //투표일련번호&투표일자별 :: 투표수 넣는 테이블 조건으로 데이터 있는지 카운트 조회 없을땐 insert 할꺼다
    private Integer getVoteBasDayCount(String voteSeq, String voteDay) {
        String selectQuery = "SELECT count(*) AS count FROM voteBasStatsByDay WHERE voteSeq=? AND voteDay=?";
        Map retMap = jdbcTemplate.queryForMap(selectQuery, voteSeq, voteDay);
        return Integer.parseInt(retMap.get("count").toString());
    }

    //투표일련번호&투표일자별 :: 투표수 넣는 테이블 처음 투표수 0으로 생성
    private void insertVoteBasDay(Integer addNum, String voteSeq, String voteDay, String owner, String created) {
        String insertQuery = "INSERT INTO voteBasStatsByDay (voteNum, voteSeq, voteDay,  owner, created) VALUES(?,?,?,?,?)";
        jdbcTemplate.update(insertQuery,addNum, voteSeq, voteDay,  owner, created);
    }


    //투표일련번호&투표일자별 :: 투표수 넣는 테이블 처음 투표수+1
    private void updateVoteBasDay(Integer addNum, String created, String voteSeq, String voteDay) {
        String updateQuery = "UPDATE voteBasStatsByDay SET voteNum=voteNum+?, created= ? WHERE voteSeq=? AND voteDay=?";
        jdbcTemplate.update(updateQuery, addNum, created, voteSeq, voteDay);

    }



    //투표일련번호&투표일자별&국가코드 :: 투표수 넣는 테이블 조건으로 데이터 있는지 카운트 조회 없을땐 insert 할꺼다
    private Integer getVoteBasCntryCount(String voteSeq, String voteDay, String cntryCd) {
        String selectQuery = "SELECT count(*) AS count FROM voteBasStatsByDayToCntry WHERE voteSeq=? AND voteDay=? AND cntryCd=?";
        Map retMap = jdbcTemplate.queryForMap(selectQuery, voteSeq, voteDay, cntryCd);
        return Integer.parseInt(retMap.get("count").toString());
    }

    //투표일련번호&투표일자별&국가코드 :: 투표수 넣는 테이블 처음 투표수 0으로 생성
    private void insertVoteCntryDay(String voteSeq, String voteDay, String cntryCd, Integer voteNum, String owner, String created) {
        String insertQuery = "INSERT INTO voteBasStatsByDayToCntry (voteSeq, voteDay, voteNum, owner, created) VALUES(?,?,?,?,?)";
        jdbcTemplate.update(insertQuery,voteSeq, voteDay, cntryCd, voteNum, owner, created);
    }


    //투표일련번호&투표일자별&국가코드 :: 투표수 넣는 테이블 처음 투표수+1
    private void updateVoteCntryDay(String created, String voteSeq, String voteDay, String cntryCd) {
        String updateQuery = "UPDATE voteBasStatsByDayToCntry SET voteNum=voteNum+1, created= ? WHERE voteSeq=? AND voteDay=? AND cntryCd=?";
        jdbcTemplate.update(updateQuery, created, voteSeq, voteDay, cntryCd);

    }


}
