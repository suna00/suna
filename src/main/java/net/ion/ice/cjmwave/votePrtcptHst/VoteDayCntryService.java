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

import java.math.BigDecimal;
import java.util.*;
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

    /*
    * 스케쥴용
    * voteSeq 기준으로 매주(월~일) 결과를 다시 주간 투표별 국가현황 테이블에 쌓는다.
    * */
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



    /*
    * 이벤트용
    * voteSeq 기준으로 매주(월~일) 결과를 다시 주간 투표별 국가현황 테이블에 쌓는다.
    * */
    public void voteBasStatsCntryEvt(ExecuteContext context){

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

            Map<String, Object> returnMap = new ConcurrentHashMap<>();
            returnMap.put("event", "voteBasStatsCntryEvt 성공");
            context.setResult(returnMap);

        }catch (Exception e) {
            logger.error("Failed to voteDayCntryService.voteBasStatsCntryJob");
        }
    }


    /*
    * 스케쥴용
    * voteSeq 기준으로 매일의 voteNum을 계속 쌓는다.
    * */
    public void voteBasStatsDayJob() {

        if (jdbcTemplate==null) {
            jdbcTemplate = NodeUtils.getNodeBindingService().getNodeBindingInfo(VOTE_BAS_INFO).getJdbcTemplate();
        }

        Integer limitCnt = 1000; //test용 리밋수 스케쥴에서 몇개씩 돌릴지 안정해짐~

        Date now = new Date();
        //String voteDay = DateFormatUtils.format(now, "yyyyMMdd");
        //String voteDay = "20171014"; //test 용
        String voteDateTime = DateFormatUtils.format(now, "yyyyMMddHHmmss");

        List<Node> voteBasInfoList = new ArrayList<>();

        try {

            // 투표 기간안에 있는 모든 VoteBasInfo 조회
            voteBasInfoList = NodeUtils.getNodeList(VOTE_BAS_INFO, "pstngStDt_below=" + voteDateTime + "&pstngFnsDt_above="+ voteDateTime);
            for (Node voteBasInfo : voteBasInfoList) {

                Integer voteSeq = Integer.parseInt(voteBasInfo.getId().toString());
                String tableNm = voteSeq.toString()+"_voteHstByMbr";

                //voteSeq별로 스케쥴 돌리고 나서 해당 _voteHstByMbr 테이블에 마지막 seq를 voteBasStatsByDayLastSeq에 저장한다.
                //스케쥴 시작시에 voteBasStatsByDayLastSeq에에 저장된 seq가 있는지 조회하고 없을때는 신규!
                Integer lastSeq = 0;
                Map<String, Object> lastSeqInfo = getVoteBasByLastSeq(voteSeq);
                if(lastSeqInfo != null){
                    lastSeq = Integer.parseInt(lastSeqInfo.get("seq").toString());
                }
                logger.info("===============> voteSeq별 라스트 시퀀스 쌓는 테이블 조회결과 :: lastSeqInfo" + lastSeqInfo);
                logger.info("===============> voteSeq별 라스트 시퀀스 쌓는 테이블 조회결과 :: lastSeq" + lastSeq);

                //해당 voteSeq_voteHstByMbr 테이블에서 리스트 조회
                List<Map<String, Object>> voteMbrList =
                        jdbcTemplate.queryForList("SELECT seq, voteDate, mbrId FROM " + tableNm + " WHERE seq>? ORDER BY seq LIMIT ?"
                                , lastSeq, limitCnt);

                if(voteMbrList != null && voteMbrList.size() > 0){

                    //Integer voteMbrListSize = voteMbrList.size();
                    for(int i=0; i<voteMbrList.size(); i++){

                        Map<String, Object> mapData = voteMbrList.get(i);
                        String mbrId = mapData.get("mbrId").toString();
                        String voteDate = mapData.get("voteDate").toString();
                        Integer seq = Integer.parseInt(mapData.get("seq").toString());

                        logger.info("===============> limit으로 짜른 이력 리스트 for문 seq :: " + seq);

                        //1. voteSeq&일자 테이블 insert / update
                        Integer voteBasDayCnt = 0;
                        Integer dayCheckCnt = getVoteBasDayCount(voteSeq, voteDate);
                        if(dayCheckCnt > 0){
                            voteBasDayCnt = updateVoteBasDay(now, voteSeq, voteDate);
                            logger.info("===============> updateVoteBasDay :: " + voteBasDayCnt);
                        }else{
                            voteBasDayCnt = insertVoteBasDay(voteSeq, voteDate, 1,"system", now);
                            logger.info("===============> insertVoteBasDay :: " + voteBasDayCnt);
                        }

                        //2. voteSeq&일자&국가별 테이블 insert / update
                        //회원정보의 국가 정보 가져온다 - but 회원정보의 국가코드 필수입력사항 아니므로 혹시몰라서~ cntryCd null아닐때만 테이블 insert하게 체크
                        Node mbrInfo = new Node();
                        try{
                            mbrInfo = NodeUtils.getNode("mbrInfo", mbrId);
                            //logger.info("===============> mbrInfo :: " + mbrInfo);
                        }catch(Exception e){
                            logger.error("mbrInfo null");
                        }


                        if (mbrInfo != null && !mbrInfo.isEmpty()) {
                            //logger.info("===============> mbrInfo not null :: ");
                            Integer voteCntryDayCnt = 0;
                            String cntryCd = mbrInfo.getStringValue("cntryCd");
                            //cntryCd null아닐때만 테이블 insert하게 체크
                            if(!StringUtils.isEmpty(cntryCd)){
                                Integer cntryCheckCnt = getVoteBasCntryCount(voteSeq, voteDate, cntryCd);
                                //voteSeq&일자&국가별 테이블 voteNum 업데이트
                                if(cntryCheckCnt > 0){
                                    voteCntryDayCnt = updateVoteCntryDay(now, voteSeq, voteDate, cntryCd);
                                    logger.info("===============> updateVoteCntryDay :: " + cntryCheckCnt);
                                }else{
                                    voteCntryDayCnt = insertVoteCntryDay(voteSeq, voteDate, cntryCd, 1, "system", now);
                                    logger.info("===============> insertVoteCntryDay :: " + cntryCheckCnt);
                                }
                            }
                        }

                        //돌고나서 제일 마지막~일때 라스트 시퀀스 저장 혹은 갱신
                        if(i == voteMbrList.size()-1){
                            //해당 _voteHstByMbr 테이블에 마지막 seq를 저장
                            logger.info("===============> 마지막 시퀀스 갱신 시작 :: lastListSeq" + seq);
                            Integer dayLstSeqCnt = 0;
                            if(lastSeqInfo == null){
                                dayLstSeqCnt = insertVoteBasByLastSeq(seq, voteSeq);
                                logger.info("===============> insertVoteBasByLastSeq :: voteSeq ? " + voteSeq + " :: lastListSeq ? " + seq);
                            }else{
                                dayLstSeqCnt = updateVoteBasByLastSeq(seq, voteSeq);
                                logger.info("===============> updateVoteBasByLastSeq :: voteSeq ? " + voteSeq + " :: lastListSeq ? " + seq);
                            }
                        }
                    }

                }else{
                    logger.info("===============> 해당 테이블에 일치하는 데이터 없음 table명은 :: " + tableNm );
                }

            }

        }catch (Exception e) {
            logger.error("Failed to voteDayCntryService.voteBasStatsDaySetNum");
        }
    }


    /*
    * 이벤트용
    * voteSeq 기준으로 매일의 voteNum을 계속 쌓는다.
    * */
    public void voteBasSatasDayEvt(ExecuteContext context) {

        if (jdbcTemplate==null) {
            jdbcTemplate = NodeUtils.getNodeBindingService().getNodeBindingInfo(VOTE_BAS_INFO).getJdbcTemplate();
        }

        Integer limitCnt = 1000; //test용 리밋수 스케쥴에서 몇개씩 돌릴지 안정해짐~

        Date now = new Date();
        //String voteDay = DateFormatUtils.format(now, "yyyyMMdd");
        //String voteDay = "20171014"; //test 용
        String voteDateTime = DateFormatUtils.format(now, "yyyyMMddHHmmss");

        List<Node> voteBasInfoList = new ArrayList<>();

        try {

            // 투표 기간안에 있는 모든 VoteBasInfo 조회
            voteBasInfoList = NodeUtils.getNodeList(VOTE_BAS_INFO, "pstngStDt_below=" + voteDateTime + "&pstngFnsDt_above="+ voteDateTime);
            for (Node voteBasInfo : voteBasInfoList) {

                Integer voteSeq = Integer.parseInt(voteBasInfo.getId().toString());
                String tableNm = voteSeq.toString()+"_voteHstByMbr";

                //voteSeq별로 스케쥴 돌리고 나서 해당 _voteHstByMbr 테이블에 마지막 seq를 voteBasStatsByDayLastSeq에 저장한다.
                //스케쥴 시작시에 voteBasStatsByDayLastSeq에에 저장된 seq가 있는지 조회하고 없을때는 신규!
                Integer lastSeq = 0;
                Map<String, Object> lastSeqInfo = getVoteBasByLastSeq(voteSeq);
                if(lastSeqInfo != null){
                    lastSeq = Integer.parseInt(lastSeqInfo.get("seq").toString());
                }
                logger.info("===============> voteSeq별 라스트 시퀀스 쌓는 테이블 조회결과 :: lastSeqInfo" + lastSeqInfo);
                logger.info("===============> voteSeq별 라스트 시퀀스 쌓는 테이블 조회결과 :: lastSeq" + lastSeq);

                //해당 voteSeq_voteHstByMbr 테이블에서 리스트 조회
                List<Map<String, Object>> voteMbrList =
                        jdbcTemplate.queryForList("SELECT seq, voteDate, mbrId FROM " + tableNm + " WHERE seq>? ORDER BY seq LIMIT ?"
                                , lastSeq, limitCnt);

                if(voteMbrList != null && voteMbrList.size() > 0){

                    //Integer voteMbrListSize = voteMbrList.size();
                    for(int i=0; i<voteMbrList.size(); i++){

                        Map<String, Object> mapData = voteMbrList.get(i);
                        String mbrId = mapData.get("mbrId").toString();
                        String voteDate = mapData.get("voteDate").toString();
                        Integer seq = Integer.parseInt(mapData.get("seq").toString());

                        logger.info("===============> limit으로 짜른 이력 리스트 for문 seq :: " + seq);

                        //1. voteSeq&일자 테이블 insert / update
                        Integer voteBasDayCnt = 0;
                        Integer dayCheckCnt = getVoteBasDayCount(voteSeq, voteDate);
                        if(dayCheckCnt > 0){
                            voteBasDayCnt = updateVoteBasDay(now, voteSeq, voteDate);
                            logger.info("===============> updateVoteBasDay :: " + voteBasDayCnt);
                        }else{
                            voteBasDayCnt = insertVoteBasDay(voteSeq, voteDate, 1,"system", now);
                            logger.info("===============> insertVoteBasDay :: " + voteBasDayCnt);
                        }

                        //2. voteSeq&일자&국가별 테이블 insert / update
                        //회원정보의 국가 정보 가져온다 - but 회원정보의 국가코드 필수입력사항 아니므로 혹시몰라서~ cntryCd null아닐때만 테이블 insert하게 체크
                        Node mbrInfo = new Node();
                        try{
                            mbrInfo = NodeUtils.getNode("mbrInfo", mbrId);
                            //logger.info("===============> mbrInfo :: " + mbrInfo);
                        }catch(Exception e){
                            logger.error("mbrInfo null");
                        }


                        if (mbrInfo != null && !mbrInfo.isEmpty()) {
                            //logger.info("===============> mbrInfo not null :: ");
                            Integer voteCntryDayCnt = 0;
                            String cntryCd = mbrInfo.getStringValue("cntryCd");
                            //cntryCd null아닐때만 테이블 insert하게 체크
                            if(!StringUtils.isEmpty(cntryCd)){
                                Integer cntryCheckCnt = getVoteBasCntryCount(voteSeq, voteDate, cntryCd);
                                //voteSeq&일자&국가별 테이블 voteNum 업데이트
                                if(cntryCheckCnt > 0){
                                    voteCntryDayCnt = updateVoteCntryDay(now, voteSeq, voteDate, cntryCd);
                                    logger.info("===============> updateVoteCntryDay :: " + cntryCheckCnt);
                                }else{
                                    voteCntryDayCnt = insertVoteCntryDay(voteSeq, voteDate, cntryCd, 1, "system", now);
                                    logger.info("===============> insertVoteCntryDay :: " + cntryCheckCnt);
                                }
                            }
                        }

                        //돌고나서 제일 마지막~일때 라스트 시퀀스 저장 혹은 갱신
                        if(i == voteMbrList.size()-1){
                            //해당 _voteHstByMbr 테이블에 마지막 seq를 저장
                            logger.info("===============> 마지막 시퀀스 갱신 시작 :: lastListSeq" + seq);
                            Integer dayLstSeqCnt = 0;
                            if(lastSeqInfo == null){
                                dayLstSeqCnt = insertVoteBasByLastSeq(seq, voteSeq);
                                logger.info("===============> insertVoteBasByLastSeq :: voteSeq ? " + voteSeq + " :: lastListSeq ? " + seq);
                            }else{
                                dayLstSeqCnt = updateVoteBasByLastSeq(seq, voteSeq);
                                logger.info("===============> updateVoteBasByLastSeq :: voteSeq ? " + voteSeq + " :: lastListSeq ? " + seq);
                            }
                        }
                    }

                }else{
                    logger.info("===============> 해당 테이블에 일치하는 데이터 없음 table명은 :: " + tableNm );
                }

            }

            Map<String, Object> returnMap = new ConcurrentHashMap<>();
            returnMap.put("event", "voteBasSatasDayEvt 성공");
            context.setResult(returnMap);

        }catch (Exception e) {
            logger.error("Failed to voteDayCntryService.voteBasStatsDaySetNum");
        }
    }

    //voteSeq별 조회했던 마지막 seq들이 있는지 체크 및 마지막 seq 가져오기
    private Map<String, Object> getVoteBasByLastSeq(Integer voteSeq) {
        String selectQuery = "SELECT voteSeq, seq FROM voteBasStatsByLastSeq WHERE voteSeq=?";
        try {
            Map retMap = jdbcTemplate.queryForMap(selectQuery, voteSeq);
            return retMap;
        } catch (Exception e) {
            return null;
        }
    }

    //voteSeq&투표일자(yyyymmdd)별 카운트한 마지막 seq insert
    private Integer insertVoteBasByLastSeq(Integer seq, Integer voteSeq) {
        String insertQuery = "INSERT INTO voteBasStatsByLastSeq (voteSeq, seq) VALUES(?,?)";
        Integer cnt = jdbcTemplate.update(insertQuery, voteSeq, seq);
        return cnt;
    }

    //voteSeq&투표일자(yyyymmdd)별 카운트한 마지막 seq update
    private Integer updateVoteBasByLastSeq(Integer seq, Integer voteSeq) {
        String updateQuery = "UPDATE voteBasStatsByLastSeq SET seq= ? WHERE voteSeq=?";
        Integer cnt = jdbcTemplate.update(updateQuery, seq, voteSeq);
        return cnt;
    }


    //투표일련번호&투표일자별 :: 투표수 넣는 테이블 조건으로 데이터 있는지 카운트 조회 없을땐 insert 할꺼다
    private Integer getVoteBasDayCount(Integer voteSeq, String voteDate) {
        String selectQuery = "SELECT count(*) AS CNT FROM voteBasStatsByDay WHERE voteSeq=? AND voteDay=?";
        Map retMap = jdbcTemplate.queryForMap(selectQuery, voteSeq, voteDate);
        return Integer.parseInt(retMap.get("CNT").toString());
    }

    //투표일련번호&투표일자별 :: 투표수 넣는 테이블 처음 투표수 생성
    private Integer insertVoteBasDay(Integer voteSeq, String voteDate, Integer addNum, String owner, Date created) {
        String insertQuery = "INSERT INTO voteBasStatsByDay (voteSeq, voteDay, voteNum, owner, created) VALUES(?,?,?,?,?)";
        Integer cnt = jdbcTemplate.update(insertQuery, voteSeq, voteDate, addNum, owner, created);
        return cnt;
    }


    //투표일련번호&투표일자별 :: 투표수 넣는 테이블 처음 투표수+addNum
    private Integer updateVoteBasDay(Date created, Integer voteSeq, String voteDate) {
        String updateQuery = "UPDATE voteBasStatsByDay SET voteNum=voteNum+1, created= ? WHERE voteSeq=? AND voteDay=?";
        Integer cnt = jdbcTemplate.update(updateQuery, created, voteSeq, voteDate);
        return cnt;
    }



    //투표일련번호&투표일자별&국가코드 :: 투표수 넣는 테이블 조건으로 데이터 있는지 카운트 조회 없을땐 insert 할꺼다
    private Integer getVoteBasCntryCount(Integer voteSeq, String voteDate, String cntryCd) {
        String selectQuery = "SELECT count(*) AS CNT FROM voteBasStatsByDayToCntry WHERE voteSeq=? AND voteDay=? AND cntryCd=?";
        Map retMap = jdbcTemplate.queryForMap(selectQuery, voteSeq, voteDate, cntryCd);
        return Integer.parseInt(retMap.get("CNT").toString());
    }

    //투표일련번호&투표일자별&국가코드 :: 투표수 넣는 테이블 처음 투표수 0으로 생성
    private Integer insertVoteCntryDay(Integer voteSeq, String voteDate, String cntryCd, Integer voteNum, String owner, Date created) {
        String insertQuery = "INSERT INTO voteBasStatsByDayToCntry (voteSeq, voteDay, cntryCd, voteNum, owner, created) VALUES(?,?,?,?,?,?)";
        Integer cnt = jdbcTemplate.update(insertQuery,voteSeq, voteDate, cntryCd, voteNum, owner, created);
        return cnt;
    }


    //투표일련번호&투표일자별&국가코드 :: 투표수 넣는 테이블 처음 투표수+1
    private Integer updateVoteCntryDay(Date created, Integer voteSeq, String voteDate, String cntryCd) {
        String updateQuery = "UPDATE voteBasStatsByDayToCntry SET voteNum=voteNum+1, created= ? WHERE voteSeq=? AND voteDay=? AND cntryCd=?";
        Integer cnt = jdbcTemplate.update(updateQuery, created, voteSeq, voteDate, cntryCd);
        return cnt;
    }


}
