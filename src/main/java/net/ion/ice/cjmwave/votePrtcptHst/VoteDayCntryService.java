package net.ion.ice.cjmwave.votePrtcptHst;

import net.ion.ice.core.context.ExecuteContext;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeService;
import net.ion.ice.core.node.NodeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

        Integer limitCnt = 100;

        Date now = new Date();
        //String voteDay = DateFormatUtils.format(now, "yyyyMMdd");
        String voteDay = "20171014"; //test 용
        String voteDateTime = DateFormatUtils.format(now, "yyyyMMddHHmmss");

        List<Node> voteBasInfoList = new ArrayList<>();

        try {

            // 투표 기간안에 있는 모든 VoteBasInfo 조회
            voteBasInfoList = NodeUtils.getNodeList(VOTE_BAS_INFO, "pstngStDt_below=" + voteDateTime + "&pstngFnsDt_above="+ voteDateTime);
            logger.info("===============> voteBasInfoList size :: " + voteBasInfoList.size() );

            for (Node voteBasInfo : voteBasInfoList) {

                Integer voteSeq = Integer.parseInt(voteBasInfo.getId().toString());
                logger.info("===============> voteBasInfo - voteSeq :: " + voteSeq );

                String tableNm = voteSeq.toString()+"_voteHstByMbr";

                //voteSeq,voteDay별로 스케쥴 돌리고 나서 해당 _voteHstByMbr 테이블에 마지막 seq를 voteBasStatsByDayLastSeq에 저장한다.
                //스케쥴 시작시에 voteBasStatsByDayLastSeq에에 저장된 seq가 있는지 조회하고 없을때는 신규!
                Integer lastSeq = 0;
                Map<String, Object> lastSeqInfo = getVoteBasByDayLstSeq(voteSeq, voteDay);
                if(lastSeqInfo != null){
                    lastSeq = Integer.parseInt(lastSeqInfo.get("seq").toString());
                }

                //List<Map<String, Object>> voteMbrList = getVoteHstByMbrList(tableNm, voteDay, lastSeq, limitCnt);
                //해당 voteSeq_voteHstByMbr 테이블에서 리스트 조회
                List<Map<String, Object>> voteMbrList =
                        jdbcTemplate.queryForList("SELECT seq, voteDate, mbrId FROM " + tableNm + " WHERE voteDate=? AND seq>? ORDER BY seq LIMIT ?"
                                , voteDay, lastSeq, limitCnt);

                //String selectQuery = "SELECT seq, voteDate, mbrId FROM ? WHERE voteDate=? AND seq>? ORDER BY seq LIMIT ?";
                //List<Map<String, Object>> voteMbrList = jdbcTemplate.queryForList(selectQuery, tableNm, voteDay, lastSeq, limitCnt);
                logger.info("===============> voteMbrList :: " + voteMbrList);

                if(voteMbrList != null){

                    Integer voteMbrListSize = voteMbrList.size();

                    //1. voteSeq&일자별 테이블 voteNum 업데이트
                    /*if(lastSeqInfo == null){
                        //insertVoteBasDay(voteMbrListSize, voteSeq, voteDay, "anonymous", now);

                        String insertQuery = "INSERt INTO voteBasStatsByDay (voteSeq, voteDay, voteNum, owner, created) "
                                + "VALUES(?,?,?,?,?)";
                        jdbcTemplate.update(insertQuery,  voteSeq, voteDay, voteMbrListSize, "anonymous", now);

                        logger.info("===============> insertVoteBasDay :: " + insertQuery);
                    }else{
                        updateVoteBasDay(voteMbrListSize, now, voteSeq, voteDay);
                        logger.info("===============> updateVoteBasDay :: " + voteMbrListSize);
                    }*/

                    //2. voteSeq&일자&국가별 테이블 voteNum 업데이트
                    logger.info("===============> voteMbrList 돌릴거다 :: ");

                    for(Map<String, Object> mapData : voteMbrList){
                        String mbrId = mapData.get("mbrId").toString();
                        logger.info("===============> mbrId :: " + mbrId);

                        //회원정보의 국가 정보 가져온다 - but 회원정보의 국가코드 필수입력사항 아니므로 혹시몰라서~ cntryCd null아닐때만 테이블 insert하게 체크
                        Node mbrInfo = NodeUtils.getNodeService().read("mbrInfo", mbrId);
                        logger.info("===============> mbrInfo :: " + mbrInfo);

                        if(mbrInfo != null){
                            String cntryCd = mbrInfo.getStringValue("cntryCd");
                            logger.info("===============> cntryCd :: " + cntryCd);

                            if(!StringUtils.isEmpty(cntryCd)){
                                Integer cntryCheckCnt = getVoteBasCntryCount(voteSeq, voteDay, cntryCd);
                                //voteSeq&일자&국가별 테이블 voteNum 업데이트
                                if(cntryCheckCnt > 0){
                                    updateVoteCntryDay(now, voteSeq, voteDay, cntryCd);
                                    logger.info("===============> updateVoteCntryDay :: " + cntryCheckCnt);
                                }else{
                                    insertVoteCntryDay(voteSeq, voteDay, cntryCd, 1, "anonymous", now);
                                    logger.info("===============> insertVoteCntryDay :: " + cntryCheckCnt);
                                }
                            }
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

            Map<String, Object> returnMap = new ConcurrentHashMap<>();
            returnMap.put("voteDay", voteDay);
            returnMap.put("voteBasInfoList", voteBasInfoList);
            context.setResult(returnMap);

        }catch (Exception e) {
            logger.error("Failed to voteDayCntryService voteBasStatsDaySetNum");
        }

    }



    //voteSeq&투표일자(yyyymmdd)별 카운트한 마지막 seq
    private Map<String, Object> getVoteBasByDayLstSeq(Integer voteSeq, String voteDay) {
        String selectQuery = "SELECT voteDay, voteSeq, seq FROM voteBasStatsByDayLastSeq WHERE voteSeq=? AND voteDay=?";
        try {
            Map retMap = jdbcTemplate.queryForMap(selectQuery, voteSeq, voteDay);
            return retMap;
        } catch (Exception e) {
            return null;
        }
    }

    //voteSeq&투표일자(yyyymmdd)별 카운트한 마지막 seq insert
    private void insertVoteBasByDayLstSeq(Integer seq, Integer voteSeq, String voteDay) {
        String insertQuery = "INSERT INTO voteBasStatsByDayLastSeq (seq, voteSeq, voteDay) VALUES(?,?,?)";
        jdbcTemplate.update(insertQuery, seq, voteSeq, voteDay);
    }

    //voteSeq&투표일자(yyyymmdd)별 카운트한 마지막 seq update
    private void updateVoteBasByDayLstSeq(Integer seq, Integer voteSeq, String voteDay) {
        String updateQuery = "UPDATE voteBasStatsByDayLastSeq SET seq= ? WHERE voteSeq=? AND voteDay=?";
        jdbcTemplate.update(updateQuery, seq, voteSeq, voteDay);
    }


    //투표일련번호&투표일자별 :: 투표수 넣는 테이블 조건으로 데이터 있는지 카운트 조회 없을땐 insert 할꺼다
    private Integer getVoteBasDayCount(Integer voteSeq, String voteDay) {
        String selectQuery = "SELECT count(*) AS count FROM voteBasStatsByDay WHERE voteSeq=? AND voteDay=?";
        Map retMap = jdbcTemplate.queryForMap(selectQuery, voteSeq, voteDay);
        return Integer.parseInt(retMap.get("count").toString());
    }

    //투표일련번호&투표일자별 :: 투표수 넣는 테이블 처음 투표수 생성
    private void insertVoteBasDay(Integer addNum, Integer voteSeq, String voteDay, String owner, Date created) {
        String insertQuery = "INSERT INTO voteBasStatsByDay (voteNum, voteSeq, voteDay,  owner, created) VALUES(?,?,?,?,?)";
        jdbcTemplate.update(insertQuery,addNum, voteSeq, voteDay,  owner, created);
    }


    //투표일련번호&투표일자별 :: 투표수 넣는 테이블 처음 투표수+1
    private void updateVoteBasDay(Integer addNum, Date created, Integer voteSeq, String voteDay) {
        String updateQuery = "UPDATE voteBasStatsByDay SET voteNum=voteNum+?, created= ? WHERE voteSeq=? AND voteDay=?";
        jdbcTemplate.update(updateQuery, addNum, created, voteSeq, voteDay);
    }



    //투표일련번호&투표일자별&국가코드 :: 투표수 넣는 테이블 조건으로 데이터 있는지 카운트 조회 없을땐 insert 할꺼다
    private Integer getVoteBasCntryCount(Integer voteSeq, String voteDay, String cntryCd) {
        String selectQuery = "SELECT count(*) AS count FROM voteBasStatsByDayToCntry WHERE voteSeq=? AND voteDay=? AND cntryCd=?";
        Map retMap = jdbcTemplate.queryForMap(selectQuery, voteSeq, voteDay, cntryCd);
        return Integer.parseInt(retMap.get("count").toString());
    }

    //투표일련번호&투표일자별&국가코드 :: 투표수 넣는 테이블 처음 투표수 0으로 생성
    private void insertVoteCntryDay(Integer voteSeq, String voteDay, String cntryCd, Integer voteNum, String owner, Date created) {
        String insertQuery = "INSERT INTO voteBasStatsByDayToCntry (voteSeq, voteDay, cntryCd, voteNum, owner, created) VALUES(?,?,?,?,?,?)";
        jdbcTemplate.update(insertQuery,voteSeq, voteDay, cntryCd, voteNum, owner, created);
    }


    //투표일련번호&투표일자별&국가코드 :: 투표수 넣는 테이블 처음 투표수+1
    private void updateVoteCntryDay(Date created, Integer voteSeq, String voteDay, String cntryCd) {
        String updateQuery = "UPDATE voteBasStatsByDayToCntry SET voteNum=voteNum+1, created= ? WHERE voteSeq=? AND voteDay=? AND cntryCd=?";
        jdbcTemplate.update(updateQuery, created, voteSeq, voteDay, cntryCd);

    }


}
