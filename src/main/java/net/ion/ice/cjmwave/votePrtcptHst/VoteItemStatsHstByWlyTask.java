package net.ion.ice.cjmwave.votePrtcptHst;

import net.ion.ice.core.data.DBService;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeService;
import net.ion.ice.core.node.NodeUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import net.ion.ice.cjmwave.external.utils.CommonDateUtils;

@Service("voteItemStatsHstByWlyTask")
public class VoteItemStatsHstByWlyTask {
    private static Logger logger = LoggerFactory.getLogger(VoteItemStatsHstByWlyTask.class);

    public static final String VOTE_BAS_INFO = "voteBasInfo";

    @Autowired
    NodeService nodeService;

    @Autowired
    private DBService dbService ;

    private JdbcTemplate jdbcTemplate;
    private JdbcTemplate jdbcTemplate_replica;

    // 주간 ( 월 ~ 일 )
    public void execVoteItemStatsHstByWly() {

        logger.info("start execVoteItemStatsHstByWly");

        Date now = new Date();
        String targetDt = DateFormatUtils.format(now, "yyyyMMdd");

        // 이전주 통계
        String prevMonDay = getPrevMonday(targetDt) ;
        String prevSunday = getPrevSunday(targetDt) ;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        logger.info("prev weekly date mon:{}, sun:{}", prevMonDay, prevSunday);
        execVoteItemStatsHstByTargetWly(prevMonDay.replaceAll("-", ""));

        // 실행한 주의 통계
        String perdStDate = getCurMonday(targetDt) ;     // CommonDateUtils.getCurMondayOrSunday("Monday","");
        String perdFnsDate = getCurSunday(targetDt) ;    // CommonDateUtils.getCurMondayOrSunday("Sunday","");
        logger.info("weekly date mon:{}, sun:{}", perdStDate, perdFnsDate);
        execVoteItemStatsHstByTargetWly(targetDt);

        logger.info("complete execVoteItemStatsHstByWly");
    }

    public void execVoteItemStatsHstByTargetWly(String targetDate) {

        logger.info("start execVoteItemStatsHstByTargetWly");
        if (jdbcTemplate==null) {
            jdbcTemplate = dbService.getJdbcTemplate("authDb");
        }
        if (jdbcTemplate_replica==null) {
            jdbcTemplate_replica = dbService.getJdbcTemplate("authDbReplica");
        }

        Date now = new Date();
        String voteDate = DateFormatUtils.format(now, "yyyyMMddHHmmss");
        // 투표 기간안에 있는 모든 VoteBasInfo 조회
        List<Node> voteBasInfoList = NodeUtils.getNodeList(VOTE_BAS_INFO, "pstngStDt_below=" + voteDate + "&pstngFnsDt_above="+ voteDate);

        for (Node voteBasInfo : voteBasInfoList) {

            String perdStDate = getCurMonday(targetDate);       // CommonDateUtils.getCurMondayOrSunday("Monday",targetDate);
            String perdFnsDate = getCurSunday(targetDate);      // CommonDateUtils.getCurMondayOrSunday("Sunday",targetDate);

            List<Map<String, Object>> voteItemCntInfoList
                    = voteItemHstList(voteBasInfo.getId(), perdStDate.replaceAll("-", ""), perdFnsDate.replaceAll("-", ""));
            List<Map<String, Object>> insertVoteItemCntInfoList = new ArrayList<>();

            for (Map voteItemCntInfo : voteItemCntInfoList) {
                voteItemCntInfo.put("voteSeq", voteBasInfo.getId());
                voteItemCntInfo.put("perdStDate", perdStDate);
                voteItemCntInfo.put("perdFnsDate", perdFnsDate);
                voteItemCntInfo.put("owner", "anonymous");
                voteItemCntInfo.put("created", now);

                insertVoteItemCntInfoList.add(voteItemCntInfo);
            }

            for (Map<String, Object> insertVoteItem : insertVoteItemCntInfoList) {

                Map<String, Object> voteItemStatsHstMap  = selectVoteItemStatsHstMap(voteBasInfo.getId(),
                        insertVoteItem.get("voteItemSeq").toString(),
                        insertVoteItem.get("voteDate").toString());

                if (voteItemStatsHstMap==null) {
                    insertVoteItemStatsHst(insertVoteItem);
                } else {
                    updateVoteItemStatsHst(insertVoteItem);
                }
            }
        }
        logger.info("complete execVoteItemStatsHstByTargetWly");
    }

    private List<Map<String, Object>> voteItemHstList(String voteSeq, String perdStDate, String perdFnsDate) {
        String selectList = "SELECT voteItemSeq, voteDate, count(seq) AS voteNum " +
                "FROM ( " +
                "  SELECT seq, voteDate, voteItemSeq, mbrId, created " +
                "  FROM " + voteSeq + "_voteItemHstByMbr " +
                "  WHERE voteDate >= ? AND voteDate <= ? ) t " +
                "GROUP BY voteItemSeq, voteDate";
        return jdbcTemplate_replica.queryForList(selectList, perdStDate, perdFnsDate);
    }

    private Map<String, Object> selectVoteItemStatsHstMap(String voteSeq, String voteItemSeq, String voteDate) {
        String selectQuery = "SELECT perdStDate, perdFnsDate, voteSeq, voteItemSeq, voteDate, voteNum, owner, created " +
                "FROM voteItemStatsHstByWly WHERE voteSeq=? AND voteItemSeq=? AND voteDate=?";
        try {
            return jdbcTemplate_replica.queryForMap(selectQuery, voteSeq, voteItemSeq, voteDate);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    private void insertVoteItemStatsHst(Map<String, Object> voteItemStatsHst) {
        String insertQuery = "INSERT INTO voteItemStatsHstByWly (" +
                "perdStDate, perdFnsDate, voteSeq, voteItemSeq, " +
                "voteDate, voteNum, owner, created) " +
                "VALUES (?,?,?,?,?,?,?,?)";
        jdbcTemplate.update(insertQuery,
                voteItemStatsHst.get("perdStDate"), voteItemStatsHst.get("perdFnsDate"),
                voteItemStatsHst.get("voteSeq"), voteItemStatsHst.get("voteItemSeq"),
                voteItemStatsHst.get("voteDate"), voteItemStatsHst.get("voteNum"),
                voteItemStatsHst.get("owner"),voteItemStatsHst.get("created"));
    }

    private void updateVoteItemStatsHst(Map<String, Object> voteItemStatsHst) {
        String updateQuery = "UPDATE voteItemStatsHstByWly "
                + "SET voteNum=?, created=? "
                + "WHERE voteSeq=? AND voteItemSeq=? AND voteDate=?";
        jdbcTemplate.update(updateQuery,
                voteItemStatsHst.get("voteNum"), voteItemStatsHst.get("created"),
                voteItemStatsHst.get("voteSeq"), voteItemStatsHst.get("voteItemSeq"), voteItemStatsHst.get("voteDate"));
    }

    //선택한 날짜 월요일
    private String getCurMonday(String voteDate){
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        Calendar c = Calendar.getInstance();
        SimpleDateFormat transForm = new SimpleDateFormat("yyyyMMdd");
        Date vD = null;
        try {
            vD = transForm.parse(voteDate);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        c.setTime(vD);
        c.set(Calendar.DAY_OF_WEEK,Calendar.MONDAY);
        return formatter.format(c.getTime());
    }

    //선택한 날짜 일요일
    private String getCurSunday(String voteDate){
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        Calendar c = Calendar.getInstance();
        SimpleDateFormat transForm = new SimpleDateFormat("yyyyMMdd");
        Date vD = null;
        try {
            vD = transForm.parse(voteDate);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        c.setTime(vD);
        c.set(Calendar.DAY_OF_WEEK,Calendar.SUNDAY);
        c.add(c.DATE,7);
        return formatter.format(c.getTime());
    }


    //현재 날짜 월요일
    private String getCurMonday(){
        java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat("yyyy-MM-dd");
        Calendar c = Calendar.getInstance();
        c.set(Calendar.DAY_OF_WEEK,Calendar.MONDAY);
        return formatter.format(c.getTime());
    }

    //현재 날짜 일요일
    private String getCurSunday(){
        java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat("yyyy-MM-dd");
        Calendar c = Calendar.getInstance();

        c.set(Calendar.DAY_OF_WEEK,Calendar.SUNDAY);
        c.add(c.DATE,7);
        return formatter.format(c.getTime());
    }

    //선택한 날짜의 이전주 월요일
    private String getPrevMonday(String voteDate){
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        Calendar c = Calendar.getInstance();
        SimpleDateFormat transForm = new SimpleDateFormat("yyyyMMdd");
        Date vD = null;
        try {
            vD = transForm.parse(voteDate);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        c.setTime(vD);
        c.add(c.DATE, -7);
        c.set(Calendar.DAY_OF_WEEK,Calendar.MONDAY);
        return formatter.format(c.getTime());
    }

    //선택한 날짜의 이전주 일요일
    private String getPrevSunday(String voteDate){
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        Calendar c = Calendar.getInstance();
        SimpleDateFormat transForm = new SimpleDateFormat("yyyyMMdd");
        Date vD = null;
        try {
            vD = transForm.parse(voteDate);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        c.setTime(vD);
        c.add(c.DATE, -7);
        c.set(Calendar.DAY_OF_WEEK,Calendar.SUNDAY);
        c.add(c.DATE,7);
        return formatter.format(c.getTime());
    }

}
