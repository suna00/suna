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

@Service("voteItemStatsBySexTask")
public class VoteItemStatsBySexTask {

    private static Logger logger = LoggerFactory.getLogger(VoteItemStatsBySexTask.class);

    public static final String VOTE_BAS_INFO = "voteBasInfo";

    @Autowired
    NodeService nodeService;

    @Autowired
    private DBService dbService ;

    private JdbcTemplate jdbcTemplate;
    private JdbcTemplate jdbcTemplate_replica;

    // 성별.
    public void execVoteItemStatsBySex() {

        logger.info("start schedule task - execVoteItemStatsBySex");

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
            logger.info("vote item sex stat schedule task - {} - {} ", voteBasInfo.getId(), voteBasInfo.getStringValue("voteNm"));

            Integer startHstSeq = 0;
//            List<Map<String,Object>> voteItemStatsByCntryList = selectItemStatsBySexList(voteBasInfo.getId(), DateFormatUtils.format(new Date(), "yyyyMMdd"));
            List<Map<String,Object>> voteItemStatsBySexList = selectItemStatsBySexList(voteBasInfo.getId());
            List<Map<String,Object>> insertItemStatsBySexList = new ArrayList<>();

            //Integer totalVoteNum = 0;
            Map<String, Integer> voteTotalCntMap = new ConcurrentHashMap<>();

            for (Map voteItemStatsBySex : voteItemStatsBySexList) {
                String chkItemSeq = voteItemStatsBySex.get("voteItemSeq").toString();

                Integer voteNum = voteItemStatsBySex.get("voteNum")==null ? 0 : Integer.parseInt(voteItemStatsBySex.get("voteNum").toString());
                Integer chkVoteNum = voteTotalCntMap.get(chkItemSeq)==null ? voteNum : (voteTotalCntMap.get(chkItemSeq) + voteNum) ;
                //totalVoteNum += voteNum;
                voteTotalCntMap.put(chkItemSeq, chkVoteNum);

                voteItemStatsBySex.put("voteRate",0);
                voteItemStatsBySex.put("owner","anonymous");
                voteItemStatsBySex.put("created",DateFormatUtils.format(new Date(), "yyyyMMddHHmmss"));
                insertItemStatsBySexList.add(voteItemStatsBySex);
            }

            for (Map insertVoteItemStats : insertItemStatsBySexList) {

                String voteNum = insertVoteItemStats.get("voteNum")==null ? "0" : insertVoteItemStats.get("voteNum").toString();
                Integer totalVoteNum = voteTotalCntMap.get(insertVoteItemStats.get("voteItemSeq").toString()) ;

                BigDecimal voteRate = new BigDecimal("0.0") ;
                if (totalVoteNum != 0) {
                    Double voteNumDouble = Double.parseDouble(voteNum);
                    BigDecimal voteCnt = new BigDecimal(voteNumDouble*100);
                    BigDecimal totalCnt = new BigDecimal(String.valueOf(totalVoteNum)) ;

                    voteRate = voteCnt.divide(totalCnt, 1, BigDecimal.ROUND_HALF_UP);
                }
                insertVoteItemStats.put("voteRate", voteRate.doubleValue());
                logger.info("vote item sex rate schedule task - {} - {} - {} : {} : {}", voteBasInfo.getId(), insertVoteItemStats.get("voteItemSeq"), voteNum, totalVoteNum, voteRate);

                String searchText = "voteSeq_equals=" + voteBasInfo.getId()
                        + "&voteItemSeq_equals=" + insertVoteItemStats.get("voteItemSeq").toString()
                        + "&sexCd_equals=" + insertVoteItemStats.get("sexCd").toString();
                List<Node> voteItemInfoList = NodeUtils.getQueryList("voteItemStatsBySex", searchText);

                if (voteItemInfoList.size()>0) {
                    logger.info("vote item sex insert - {} ", voteBasInfo.getId());
                    Node voteItemStats = nodeService.updateNode(insertVoteItemStats, "voteItemStatsBySex");
                } else {
                    logger.info("vote item sex update - {} ", voteBasInfo.getId());
                    Node voteItemStats = nodeService.createNode(insertVoteItemStats, "voteItemStatsBySex");
                }

            }
        }
        logger.info("complete schedule task - execVoteItemStatsBySex");
    }

    private String selectItemStatsQuery =
            "SELECT voteSeq, voteItemSeq, sexCd, sum(voteNum) AS voteNum " +
                    "FROM voteItemStatsHstBySex " +
                    "WHERE voteSeq=?" +
                    "GROUP BY voteSeq, voteItemSeq, sexCd ";
    private List<Map<String,Object>> selectItemStatsBySexList(String voteSeq) {
//        String selectQuery =
//                "SELECT voteSeq, voteItemSeq, sexCd, sum(voteNum) AS voteNum, voteDate " +
//                "FROM voteItemStatsHstBySex " +
//                "WHERE voteDate=? AND voteSeq=?" +
//                "GROUP BY voteSeq, voteItemSeq, sexCd, voteDate ";

        return jdbcTemplate.queryForList(selectItemStatsQuery, voteSeq);
    }

}
