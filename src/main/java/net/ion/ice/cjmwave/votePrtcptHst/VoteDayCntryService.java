package net.ion.ice.cjmwave.votePrtcptHst;

import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeService;
import net.ion.ice.core.node.NodeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

@Service("voteDayCntryService")
public class VoteDayCntryService {

    public static final String VOTE_BAS_INFO = "voteBasInfo";
    public static final String VOTE_ITEM_INFO = "voteItemInfo";
    public static final String MBR_INFO = "mbrInfo";

    private JdbcTemplate jdbcTemplate;

    @Autowired
    private NodeService nodeService;

    private Logger logger = Logger.getLogger(VoteDayCntryService.class);


    //voteSeq 기준으로 매주(월~일) 결과를 다시 주간 투표별 국가현황 테이블에 쌓는다.
    public void voteBasStatsCntryJob(){

        if (jdbcTemplate==null) {
            jdbcTemplate = NodeUtils.getNodeBindingService().getNodeBindingInfo(VOTE_BAS_INFO).getJdbcTemplate();
        }

        Date now = new Date();
        Calendar cal = Calendar.getInstance();

        Integer weekNum = cal.get(Calendar.DAY_OF_WEEK); //1.일요일 ~ 7.토요일 이다.
        //String toDay = DateFormatUtils.format(cal, "yyyyMMdd"); //오늘 날짜 - yyyyMMdd형식

        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        String monDay = DateFormatUtils.format(cal, "yyyyMMdd");
        String saveMon = DateFormatUtils.format(cal, "yyyy-MM-dd");

        cal.add(Calendar.DATE, 7);
        cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
        String sunDay = DateFormatUtils.format(cal, "yyyyMMdd");
        String saveSun = DateFormatUtils.format(cal, "yyyy-MM-dd");

        try {

            //1. voteBasStatsByDayToCntry 테이블 전체 delete
            Integer deleteCnt = jdbcTemplate.update("DELETE FROM cntryVoteStatsByVote");
            logger.info("===============> delete1  cntryVoteStatsByVote:: " + deleteCnt);

            //2. voteBasStatsByDayToCntry 테이블에서 월~일요일 날짜 까지 가져온다
            String totalListQuery = "SELECT a.voteSeq, a.cntryCd, a.voteNum, b.totalVoteNum "+
                    " FROM ( SELECT voteSeq, cntryCd, sum(voteNum) AS voteNum " +
                    " FROM voteBasStatsByDayToCntry WHERE voteDay >=? AND voteDay <=? " +
                    " GROUP BY voteSeq, cntryCd) a LEFT OUTER JOIN " +
                    " (SELECT voteSeq, sum(voteNum) AS totalVoteNum " +
                    " FROM voteBasStatsByDayToCntry WHERE voteDay >=? AND voteDay <=? " +
                    " GROUP BY voteSeq) b " +
                    " ON a.voteSeq = b.voteSeq " +
                    " ORDER BY a.voteSeq, a.voteNum desc ";
            List<Map<String, Object>> voteCntryStatsList = jdbcTemplate.queryForList(totalListQuery, monDay, sunDay, monDay, sunDay);
            logger.info("===============> voteCntryStatsList :: " + voteCntryStatsList);

            if(voteCntryStatsList != null && voteCntryStatsList.size() > 0){

                Integer rankNum = 0;
                String voteSeq = "";
                for(int i=0; i<voteCntryStatsList.size(); i++){
                    Map<String, Object> mapData = voteCntryStatsList.get(i);

                    if(i == 0){
                        voteSeq = mapData.get("voteSeq").toString();
                        rankNum = 1;
                    }else{
                        if(!voteSeq.equals(mapData.get("voteSeq").toString())){
                            voteSeq = mapData.get("voteSeq").toString();
                            rankNum = 1;
                        }
                    }

                    String cntryCd = mapData.get("cntryCd").toString();
                    Integer voteNum = Integer.parseInt(mapData.get("voteNum").toString());
                    Integer totalVoteNum = Integer.parseInt(mapData.get("totalVoteNum").toString());

                    Double voteNumDouble = Double.parseDouble(mapData.get("voteNum").toString());
                    BigDecimal voteCnt = new BigDecimal(voteNumDouble*100);
                    BigDecimal totalCnt = new BigDecimal(totalVoteNum);
                    BigDecimal voteRate = voteCnt.divide(totalCnt, 1, BigDecimal.ROUND_HALF_UP);

                    //3. voteBasStatsByDayToCntry 테이블에 insert
                    String insertVoteCntryQuery = "INSERT INTO cntryVoteStatsByVote "+
                            "(perdStDate, perdFnsDate, voteSeq, cntryCd, rankNum, voteRate, voteNum, owner, created) "+
                            "VALUES(?,?,?,?,?,?,?,?,?)";
                    Integer insertCnt = jdbcTemplate.update(insertVoteCntryQuery,
                            saveMon, saveSun, Integer.parseInt(voteSeq), cntryCd, rankNum, voteRate, voteNum, "system", now);

                    rankNum++; //다음을 위해 +1 새로운 voteSeq가 왔을땐 다시 1부터 시작
                }
            }else{
                logger.info("===============> voteBasStatsByDayToCntry에서 해당 날짜 조건검색으로 데이터 없음");
            }

        }catch (Exception e) {
            logger.error("Failed to voteDayCntryService.voteBasStatsCntryJob");
        }


    }


    //voteSeq 기준으로 매일의 voteNum을 쌓는다.
    public void voteBasStatsDayJob() {

        if (jdbcTemplate==null) {
            jdbcTemplate = NodeUtils.getNodeBindingService().getNodeBindingInfo(VOTE_BAS_INFO).getJdbcTemplate();
        }

        Integer limitCnt = 1000; //test용 리밋수 스케쥴에서 몇개씩 돌릴지 안정해짐~

        Date now = new Date();
        String voteDay = DateFormatUtils.format(now, "yyyyMMdd");
        //String voteDay = "20171014"; //test 용
        String voteDateTime = DateFormatUtils.format(now, "yyyyMMddHHmmss");

        List<Node> voteBasInfoList = new ArrayList<>();

        try {

            // 투표 기간안에 있는 모든 VoteBasInfo 조회
            voteBasInfoList = NodeUtils.getNodeList(VOTE_BAS_INFO, "pstngStDt_below=" + voteDateTime + "&pstngFnsDt_above="+ voteDateTime);
            for (Node voteBasInfo : voteBasInfoList) {

                Integer voteSeq = Integer.parseInt(voteBasInfo.getId().toString());
                String tableNm = voteSeq.toString()+"_voteHstByMbr";

                //voteSeq,voteDay별로 스케쥴 돌리고 나서 해당 _voteHstByMbr 테이블에 마지막 seq를 voteBasStatsByDayLastSeq에 저장한다.
                //스케쥴 시작시에 voteBasStatsByDayLastSeq에에 저장된 seq가 있는지 조회하고 없을때는 신규!
                Integer lastSeq = 0;
                Map<String, Object> lastSeqInfo = getVoteBasByDayLstSeq(voteSeq, voteDay);
                if(lastSeqInfo != null){
                    lastSeq = Integer.parseInt(lastSeqInfo.get("seq").toString());
                }

                //해당 voteSeq_voteHstByMbr 테이블에서 리스트 조회
                List<Map<String, Object>> voteMbrList =
                        jdbcTemplate.queryForList("SELECT seq, voteDate, mbrId FROM " + tableNm + " WHERE voteDate=? AND seq>? ORDER BY seq LIMIT ?"
                                , voteDay, lastSeq, limitCnt);
                //logger.info("===============> voteMbrList :: " + voteMbrList);

                if(voteMbrList != null && voteMbrList.size() > 0){

                    Integer voteMbrListSize = voteMbrList.size();

                    //1. voteSeq&일자별 테이블 voteNum 업데이트
                    Integer voteBasDayCnt = 0;
                    if(lastSeqInfo == null){
                        voteBasDayCnt = insertVoteBasDay(voteSeq, voteDay, voteMbrListSize, "system", now);
                        logger.info("===============> insertVoteBasDay :: " + voteBasDayCnt);
                    }else{
                        voteBasDayCnt = updateVoteBasDay(voteMbrListSize, now, voteSeq, voteDay);
                        logger.info("===============> updateVoteBasDay :: " + voteBasDayCnt);
                    }

                    //2. voteSeq&일자&국가별 테이블 voteNum 업데이트
                    //logger.info("===============> voteMbrList 돌릴거다 :: ");
                    Integer voteCntryDayCnt = 0;

                    for(Map<String, Object> mapData : voteMbrList){
                        String mbrId = mapData.get("mbrId").toString();
                        //logger.info("===============> mbrId :: " + mbrId);

                        //회원정보의 국가 정보 가져온다 - but 회원정보의 국가코드 필수입력사항 아니므로 혹시몰라서~ cntryCd null아닐때만 테이블 insert하게 체크
                        //Node mbrInfo = NodeUtils.getNodeService().read("mbrInfo", mbrId); read 로 조회 했을때 혹시라도 회원정보 없으면 무조건 에러로 감.....
                        List<Node> mbrInfos = new ArrayList<>();
                        mbrInfos = NodeUtils.getNodeService().getNodeList("mbrInfo", "snsTypeCd=" + mbrId.split(">")[0].toString() + "&snsKey="+ mbrId.split(">")[1].toString());
                        if(mbrInfos.size() > 0){
                           Node mbrInfo =  mbrInfos.get(0);
                            String cntryCd = mbrInfo.getStringValue("cntryCd");
                            //logger.info("===============> cntryCd :: " + cntryCd);
                            if(!StringUtils.isEmpty(cntryCd)){
                                Integer cntryCheckCnt = getVoteBasCntryCount(voteSeq, voteDay, cntryCd);
                                //voteSeq&일자&국가별 테이블 voteNum 업데이트
                                if(cntryCheckCnt > 0){
                                    voteCntryDayCnt = updateVoteCntryDay(now, voteSeq, voteDay, cntryCd);
                                    logger.info("===============> updateVoteCntryDay :: " + cntryCheckCnt);
                                }else{
                                    voteCntryDayCnt = insertVoteCntryDay(voteSeq, voteDay, cntryCd, 1, "system", now);
                                    logger.info("===============> insertVoteCntryDay :: " + cntryCheckCnt);
                                }
                            }
                        }
                    }


                    //해당 _voteHstByMbr 테이블에 마지막 seq를 저장
                    Integer lastListSeq = Integer.parseInt(voteMbrList.get(voteMbrListSize-1).get("seq").toString());
                    //logger.info("===============> lastListSeq :: " + lastListSeq);
                    Integer dayLstSeqCnt = 0;
                    if(lastSeqInfo == null){
                        dayLstSeqCnt = insertVoteBasByDayLstSeq(lastListSeq, voteSeq, voteDay);
                    }else{
                        dayLstSeqCnt = updateVoteBasByDayLstSeq(lastListSeq, voteSeq, voteDay);
                    }

                }else{
                    logger.info("===============> 해당 테이블에 일치하는 데이터 없음 table명은 :: " + tableNm );
                }

            }

        }catch (Exception e) {
            logger.error("Failed to voteDayCntryService.voteBasStatsDaySetNum");
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
    private Integer insertVoteBasByDayLstSeq(Integer seq, Integer voteSeq, String voteDay) {
        String insertQuery = "INSERT INTO voteBasStatsByDayLastSeq (seq, voteSeq, voteDay) VALUES(?,?,?)";
        Integer cnt = jdbcTemplate.update(insertQuery, seq, voteSeq, voteDay);
        return cnt;
    }

    //voteSeq&투표일자(yyyymmdd)별 카운트한 마지막 seq update
    private Integer updateVoteBasByDayLstSeq(Integer seq, Integer voteSeq, String voteDay) {
        String updateQuery = "UPDATE voteBasStatsByDayLastSeq SET seq= ? WHERE voteSeq=? AND voteDay=?";
        Integer cnt = jdbcTemplate.update(updateQuery, seq, voteSeq, voteDay);
        return cnt;
    }


    //투표일련번호&투표일자별 :: 투표수 넣는 테이블 조건으로 데이터 있는지 카운트 조회 없을땐 insert 할꺼다
    private Integer getVoteBasDayCount(Integer voteSeq, String voteDay) {
        String selectQuery = "SELECT count(*) AS CNT FROM voteBasStatsByDay WHERE voteSeq=? AND voteDay=?";
        Map retMap = jdbcTemplate.queryForMap(selectQuery, voteSeq, voteDay);
        return Integer.parseInt(retMap.get("CNT").toString());
    }

    //투표일련번호&투표일자별 :: 투표수 넣는 테이블 처음 투표수 생성
    private Integer insertVoteBasDay(Integer voteSeq, String voteDay, Integer addNum, String owner, Date created) {
        String insertQuery = "INSERT INTO voteBasStatsByDay (voteSeq, voteDay, voteNum, owner, created) VALUES(?,?,?,?,?)";
        Integer cnt = jdbcTemplate.update(insertQuery, voteSeq, voteDay,  addNum, owner, created);
        return cnt;
    }


    //투표일련번호&투표일자별 :: 투표수 넣는 테이블 처음 투표수+1
    private Integer updateVoteBasDay(Integer addNum, Date created, Integer voteSeq, String voteDay) {
        String updateQuery = "UPDATE voteBasStatsByDay SET voteNum=voteNum+?, created= ? WHERE voteSeq=? AND voteDay=?";
        Integer cnt = jdbcTemplate.update(updateQuery, addNum, created, voteSeq, voteDay);
        return cnt;
    }



    //투표일련번호&투표일자별&국가코드 :: 투표수 넣는 테이블 조건으로 데이터 있는지 카운트 조회 없을땐 insert 할꺼다
    private Integer getVoteBasCntryCount(Integer voteSeq, String voteDay, String cntryCd) {
        String selectQuery = "SELECT count(*) AS CNT FROM voteBasStatsByDayToCntry WHERE voteSeq=? AND voteDay=? AND cntryCd=?";
        Map retMap = jdbcTemplate.queryForMap(selectQuery, voteSeq, voteDay, cntryCd);
        return Integer.parseInt(retMap.get("CNT").toString());
    }

    //투표일련번호&투표일자별&국가코드 :: 투표수 넣는 테이블 처음 투표수 0으로 생성
    private Integer insertVoteCntryDay(Integer voteSeq, String voteDay, String cntryCd, Integer voteNum, String owner, Date created) {
        String insertQuery = "INSERT INTO voteBasStatsByDayToCntry (voteSeq, voteDay, cntryCd, voteNum, owner, created) VALUES(?,?,?,?,?,?)";
        Integer cnt = jdbcTemplate.update(insertQuery,voteSeq, voteDay, cntryCd, voteNum, owner, created);
        return cnt;
    }


    //투표일련번호&투표일자별&국가코드 :: 투표수 넣는 테이블 처음 투표수+1
    private Integer updateVoteCntryDay(Date created, Integer voteSeq, String voteDay, String cntryCd) {
        String updateQuery = "UPDATE voteBasStatsByDayToCntry SET voteNum=voteNum+1, created= ? WHERE voteSeq=? AND voteDay=? AND cntryCd=?";
        Integer cnt = jdbcTemplate.update(updateQuery, created, voteSeq, voteDay, cntryCd);
        return cnt;
    }


}
