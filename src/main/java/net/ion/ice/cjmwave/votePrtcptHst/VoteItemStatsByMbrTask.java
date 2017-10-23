package net.ion.ice.cjmwave.votePrtcptHst;

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

@Service("voteItemStatsByMbrTask")
public class VoteItemStatsByMbrTask {

    private static Logger logger = LoggerFactory.getLogger(VoteItemStatsByMbrTask.class);

    public static final String VOTE_BAS_INFO = "voteBasInfo";

    @Autowired
    NodeService nodeService;

    JdbcTemplate jdbcTemplate;

    // 성별.
    public void execVoteItemStatsBySex() {

        logger.info("start schedule task - execVoteItemStatsBySex");

        if (jdbcTemplate == null) {
            jdbcTemplate = NodeUtils.getNodeBindingService().getNodeBindingInfo(VOTE_BAS_INFO).getJdbcTemplate();
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
            List<Map<String,Object>> voteItemStatsBySexList = selectItemStatsBySexList(voteBasInfo.getId(), DateFormatUtils.format(new Date(), "yyyyMMdd"));
            List<Map<String,Object>> insertItemStatsBySexList = new ArrayList<>();
            Integer totalVoteNum = 0;
            for (Map voteItemStatsBySex : voteItemStatsBySexList) {
                Integer voteNum =
                        voteItemStatsBySex.get("voteNum")==null ? 0 : Integer.parseInt(voteItemStatsBySex.get("voteNum").toString());
                totalVoteNum += voteNum;

                voteItemStatsBySex.put("voteRate",0);
                voteItemStatsBySex.put("owner","anonymous");
                voteItemStatsBySex.put("created",DateFormatUtils.format(new Date(), "yyyyMMddHHmmss"));
                insertItemStatsBySexList.add(voteItemStatsBySex);
            }

            for (Map insertVoteItemStats : insertItemStatsBySexList) {
                String voteNum =
                        insertVoteItemStats.get("voteNum")==null ? "0" : insertVoteItemStats.get("voteNum").toString();
                Double voteNumDouble = Double.parseDouble(voteNum);
                BigDecimal voteCnt = new BigDecimal(voteNumDouble*100);
                BigDecimal totalCnt = new BigDecimal(totalVoteNum);

                BigDecimal voteRate = voteCnt.divide(totalCnt, 1, BigDecimal.ROUND_HALF_UP);
                insertVoteItemStats.put("voteRate", voteRate.doubleValue());
                logger.info("vote item sex rate schedule task - {} - {} {} {}", voteBasInfo.getId(), voteCnt, totalCnt, voteRate);

                String searchText = "voteSeq_matching=" + voteBasInfo.getId()
                        + "&voteItemSeq_matching=" + insertVoteItemStats.get("voteItemSeq").toString()
                        + "&sexCd_matching=" + insertVoteItemStats.get("sexCd").toString();
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

    private List<Map<String,Object>> selectItemStatsBySexList(String voteSeq, String voteDate) {
        String selectQuery = "SELECT voteSeq, voteItemSeq, sexCd, sum(voteNum) AS voteNum, voteDate " +
                "FROM (" +
                "  SELECT" +
                "    voteSeq, voteItemSeq, mbrId, v.snsTypeCd, v.snsKey, sexCd, cntryCd, voteNum, hstSeq, v.voteDate, v.created " +
                "  FROM voteItemStatsHstByMbr v LEFT JOIN mbrInfo m " +
                "  ON v.snsTypeCd = m.snsTypeCd AND v.snsKey = m.snsKey) t " +
                "WHERE voteDate=? AND voteSeq=? AND sexCd IS NOT NULL AND cntryCd IS NOT NULL " +
                "GROUP BY voteDate, voteSeq, voteItemSeq, sexCd ";
        return jdbcTemplate.queryForList(selectQuery, voteDate, voteSeq);
    }

    // 국가.
    public void execVoteItemStatsByCntry() {

        logger.info("start schedule task - execVoteItemStatsByCntry");

        if (jdbcTemplate == null) {
            jdbcTemplate = NodeUtils.getNodeBindingService().getNodeBindingInfo(VOTE_BAS_INFO).getJdbcTemplate();
        }
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
            List<Map<String,Object>> voteItemStatsByCntryList = selectItemStatsByCntryList(voteBasInfo.getId(), DateFormatUtils.format(new Date(), "yyyyMMdd"));
            List<Map<String,Object>> insertItemStatsByCntryList = new ArrayList<>();
            Integer totalVoteNum = 0;
            for (Map voteItemStatsByCntry : voteItemStatsByCntryList) {

                Integer voteNum =
                        voteItemStatsByCntry.get("voteNum")==null ? 0 : Integer.parseInt(voteItemStatsByCntry.get("voteNum").toString());
                totalVoteNum += voteNum;

                voteItemStatsByCntry.put("voteRate",0);
                voteItemStatsByCntry.put("owner","anonymous");
                voteItemStatsByCntry.put("created",DateFormatUtils.format(new Date(), "yyyyMMddHHmmss"));

                insertItemStatsByCntryList.add(voteItemStatsByCntry);
            }

            for (Map insertVoteItemStats : insertItemStatsByCntryList) {
                String voteNum =
                        insertVoteItemStats.get("voteNum")==null ? "0" : insertVoteItemStats.get("voteNum").toString();
                Double voteNumDouble = Double.parseDouble(voteNum);
                BigDecimal voteCnt = new BigDecimal(voteNumDouble*100);
                BigDecimal totalCnt = new BigDecimal(totalVoteNum);

                BigDecimal voteRate = voteCnt.divide(totalCnt, 1, BigDecimal.ROUND_HALF_UP);
                insertVoteItemStats.put("voteRate", voteRate.doubleValue());
                logger.info("vote item sex rate schedule task - {} - {} {} {}", voteBasInfo.getId(), voteCnt, totalCnt, voteRate);

                String searchText = "voteSeq_matching=" + voteBasInfo.getId()
                        + "&voteItemSeq_matching=" + insertVoteItemStats.get("voteItemSeq").toString()
                        + "&cntryCd_matching=" + insertVoteItemStats.get("cntryCd").toString();
                List<Node> voteItemInfoList = NodeUtils.getQueryList("voteItemStatsByCntry", searchText);

                if (voteItemInfoList.size()>0) {
                    logger.info("vote item sex insert - {} ", voteBasInfo.getId());
                    Node voteItemStats = nodeService.updateNode(insertVoteItemStats, "voteItemStatsByCntry");
                } else {
                    logger.info("vote item sex insert - {} ", voteBasInfo.getId());
                    Node voteItemStats = nodeService.createNode(insertVoteItemStats, "voteItemStatsByCntry");
                }
            }
        }

        logger.info("complete schedule task - execVoteItemStatsByCntry");
    }

    private List<Map<String,Object>> selectItemStatsByCntryList(String voteSeq, String voteDate) {

        String selectQuery = "SELECT voteSeq, voteItemSeq, cntryCd, sum(voteNum) AS voteNum, voteDate " +
                "FROM (" +
                "  SELECT" +
                "    voteSeq, voteItemSeq, mbrId, v.snsTypeCd, v.snsKey, sexCd, cntryCd, voteNum, hstSeq, v.voteDate, v.created " +
                "  FROM voteItemStatsHstByMbr v LEFT JOIN mbrInfo m " +
                "  ON v.snsTypeCd = m.snsTypeCd AND v.snsKey = m.snsKey) t " +
                "WHERE voteDate=? AND voteSeq=? AND sexCd IS NOT NULL AND cntryCd IS NOT NULL " +
                "GROUP BY voteDate, voteSeq, voteItemSeq, cntryCd ";
        return jdbcTemplate.queryForList(selectQuery, voteDate, voteSeq);
    }

}
