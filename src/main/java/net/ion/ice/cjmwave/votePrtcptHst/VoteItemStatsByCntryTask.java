package net.ion.ice.cjmwave.votePrtcptHst;

import net.ion.ice.core.data.DBService;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeService;
import net.ion.ice.core.node.NodeUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service("voteItemStatsByCntryTask")
public class VoteItemStatsByCntryTask {

    private static Logger logger = LoggerFactory.getLogger(VoteItemStatsByCntryTask.class);

    public static final String VOTE_BAS_INFO = "voteBasInfo";

    @Autowired
    NodeService nodeService;

    @Autowired
    private DBService dbService ;

    private JdbcTemplate jdbcTemplate;
    private JdbcTemplate jdbcTemplate_replica;

    // 성별.
    public void execVoteItemStatsByCntry() {

        logger.info("start schedule task - execVoteItemStatsByCntry");

        if (jdbcTemplate==null) {
            jdbcTemplate = dbService.getJdbcTemplate("authDb");
        }
        if (jdbcTemplate_replica==null) {
            jdbcTemplate_replica = dbService.getJdbcTemplate("authDbReplica");
        }

        // 통계를 위한 대상 voteSeq List

        Calendar cal = Calendar.getInstance() ;
        cal.add(Calendar.DATE, -1);
        Date before = cal.getTime() ;
        Calendar now = Calendar.getInstance() ;
        now.add(Calendar.DATE, +1);
        String voteDate = DateFormatUtils.format(now.getTime(), "yyyyMMddHHmmss");
        // 투표 기간안에 있는 모든 VoteBasInfo 조회
        List<Node> voteBasInfoList = NodeUtils.getNodeList(VOTE_BAS_INFO, "pstngStDt_below=" + voteDate + "&pstngFnsDt_above="+ DateFormatUtils.format(before, "yyyyMMddHHmmss"));
        for (Node voteBasInfo : voteBasInfoList) {
            logger.info("vote item cntry stat schedule task - {} - {} ", voteBasInfo.getId(), voteBasInfo.getStringValue("voteNm"));

            Integer startHstSeq = 0;
//            List<Map<String,Object>> voteItemStatsByCntryList = selectItemStatsByCntryList(voteBasInfo.getId(), DateFormatUtils.format(new Date(), "yyyyMMdd"));
            List<Map<String,Object>> voteItemStatsByCntryList = selectItemStatsByCntryList(voteBasInfo.getId());
            List<Map<String,Object>> insertItemStatsByCntryList = new ArrayList<>();

            //Integer totalVoteNum = 0;
            Map<String, Integer> voteTotalCntMap = new ConcurrentHashMap<>();

            for (Map voteItemStatsByCntry : voteItemStatsByCntryList) {
                String chkItemSeq = voteItemStatsByCntry.get("voteItemSeq").toString();

                Integer voteNum = voteItemStatsByCntry.get("voteNum")==null ? 0 : Integer.parseInt(voteItemStatsByCntry.get("voteNum").toString());
                Integer chkVoteNum = voteTotalCntMap.get(chkItemSeq)==null ? voteNum : (voteTotalCntMap.get(chkItemSeq) + voteNum) ;

                voteTotalCntMap.put(chkItemSeq, chkVoteNum) ;

                voteItemStatsByCntry.put("voteRate",0);
                voteItemStatsByCntry.put("owner","anonymous");
                voteItemStatsByCntry.put("created",DateFormatUtils.format(new Date(), "yyyyMMddHHmmss"));
                insertItemStatsByCntryList.add(voteItemStatsByCntry);
            }

            for (Map insertVoteItemStats : insertItemStatsByCntryList) {

                String voteNum = insertVoteItemStats.get("voteNum")==null ? "0" : insertVoteItemStats.get("voteNum").toString();
                Integer totalVoteNum = voteTotalCntMap.get(insertVoteItemStats.get("voteItemSeq").toString());
//                Integer totalVoteNum = selectItemCntByCntry(voteBasInfo.getId(), insertVoteItemStats.get("voteItemSeq").toString());
                BigDecimal voteRate = new BigDecimal("0.0");
                if (totalVoteNum != 0) {
                    Double voteNumDouble = Double.parseDouble(voteNum);
                    BigDecimal voteCnt = new BigDecimal(voteNumDouble*100);
                    BigDecimal totalCnt = new BigDecimal(String.valueOf(totalVoteNum));

                    voteRate = voteCnt.divide(totalCnt, 1, BigDecimal.ROUND_HALF_UP);
                }
                insertVoteItemStats.put("voteRate", voteRate.doubleValue());
                logger.info("vote item cntry rate schedule task - {} - {} - {} : {} : {}", voteBasInfo.getId(), insertVoteItemStats.get("voteItemSeq"), voteNum, totalVoteNum, voteRate);

                String searchText = "voteSeq_equals=" + voteBasInfo.getId()
                        + "&voteItemSeq_equals=" + insertVoteItemStats.get("voteItemSeq").toString()
                        + "&cntryCd_equals=" + insertVoteItemStats.get("cntryCd").toString();
                List<Node> voteItemInfoList = NodeUtils.getQueryList("voteItemStatsByCntry", searchText);

                if (voteItemInfoList.size()>0) {
                    logger.info("vote item cntry insert - {} ", voteBasInfo.getId());
                    Node voteItemStats = nodeService.updateNode(insertVoteItemStats, "voteItemStatsByCntry");
                } else {
                    logger.info("vote item cntry update - {} ", voteBasInfo.getId());
                    Node voteItemStats = nodeService.createNode(insertVoteItemStats, "voteItemStatsByCntry");
                }
            }
        }
        logger.info("complete schedule task - execVoteItemStatsByCntry");
    }

    private String selectItemStatsQuery =
            "SELECT voteSeq, voteItemSeq, cntryCd, sum(voteNum) AS voteNum " +
                    "FROM voteItemStatsHstByCntry " +
                    "WHERE voteSeq=?" +
                    "GROUP BY voteSeq, voteItemSeq, cntryCd";
    private List<Map<String,Object>> selectItemStatsByCntryList(String voteSeq) {
//        String selectQuery =
//                "SELECT voteSeq, voteItemSeq, cntryCd, sum(voteNum) AS voteNum, voteDate " +
//                        "FROM voteItemStatsHstByCntry " +
//                        "WHERE voteDate=? AND voteSeq=?" +
//                        "GROUP BY voteSeq, voteItemSeq, cntryCd, voteDate ";

        logger.info("vote item select sql - voteSeq : {}", voteSeq);

        return jdbcTemplate.queryForList(selectItemStatsQuery, voteSeq);
    }
}

