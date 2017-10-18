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
import java.util.concurrent.ConcurrentHashMap;

@Service("voteItemStatsTask")
public class VoteItemStatsTask {
    private static Logger logger = LoggerFactory.getLogger(VoteItemStatsTask.class);

    public static final String VOTE_BAS_INFO = "voteBasInfo";
    public static final String VOTE_ITEM_INFO = "voteItemInfo";

    private JdbcTemplate jdbcTemplate;

    @Autowired
    private NodeService nodeService;

    // Total VoteCount
    Map<String, Integer> totalVoteCntByVoteSeq;

    public void voteItemStatsJob() {

        if (jdbcTemplate==null) {
            jdbcTemplate = NodeUtils.getNodeBindingService().getNodeBindingInfo(VOTE_BAS_INFO).getJdbcTemplate();
        }

        if (totalVoteCntByVoteSeq == null) {
            totalVoteCntByVoteSeq = new ConcurrentHashMap<>();
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
            logger.info("vote item stat schedule task - {} - {} ", voteBasInfo.getId(), voteBasInfo.getStringValue("voteNm"));

            // 총 투표수
            Integer totalVoteNum = 0;
            /*// Hazelcast 사용시.
            if (totalVoteCntByVoteSeq.get(voteBasInfo.getId()) == null) {
                totalVoteNum = selectTotalVoteCnt(voteBasInfo.getId());
                totalVoteCntByVoteSeq.put(voteBasInfo.getId(), totalVoteNum);
            } else {
                totalVoteNum = totalVoteCntByVoteSeq.get(voteBasInfo.getId());
            }
            */
            totalVoteNum = getTotalVoteCnt(voteBasInfo.getId());
            logger.info("vote item total vote num schedule task - {} - {} ", voteBasInfo.getId(), totalVoteNum);

            // VoteSeq에 해당 하는 모든 voteItemInfo 가져오기
            List<Node> voteItemInfoList = NodeUtils.getNodeList(VOTE_ITEM_INFO, "voteSeq_matching=" + voteBasInfo.getId());
            logger.info("vote item list num schedule task - {} - {} ", voteBasInfo.getId(), voteItemInfoList.size());

            // 각 투표가 진행된 voteItem 정보 및 Count 조회
            List<Map<String, Object>> voteNumInfoList = getVoteNumByVoteItemList(voteBasInfo.getId());
            List<Map<String, Object>> rtVoteItemStatsList = new ArrayList<>();

            logger.info("vote item num schedule task - {} - {} ", voteBasInfo.getId(), voteNumInfoList.size());

            List<String> checkVoteItemSeqList = new ArrayList<>();
            for (Map<String, Object> voteNumInfo : voteNumInfoList) {
                if(!voteBasInfo.getId().equals(voteNumInfo.get("voteSeq"))){
                    continue;
                }
                // Rank Gap - pass
                // TODO - VoteRate

                Double voteNumDouble = Double.parseDouble(voteNumInfo.get("voteNum").toString());
                BigDecimal voteCnt = new BigDecimal(voteNumDouble*100);
                BigDecimal totalCnt = new BigDecimal(totalVoteNum);

                BigDecimal voteRate = voteCnt.divide(totalCnt, 1, BigDecimal.ROUND_HALF_UP);
                voteNumInfo.put("voteRate", voteRate.doubleValue());
                voteNumInfo.put("voteNum", voteNumInfo.get("voteNum"));
                voteNumInfo.put("created", DateFormatUtils.format(new Date(), "yyyyMMddHHmmss"));
                logger.info("vote item  rate schedule task - {} - {} {} {}", voteBasInfo.getId(), voteCnt, totalCnt, voteRate);

                // Response List에 추가
                rtVoteItemStatsList.add(voteNumInfo);
                checkVoteItemSeqList.add(voteNumInfo.get("voteItemSeq").toString());
            }

            for (Node voteItemInfo : voteItemInfoList) {
                if(!checkVoteItemSeqList.contains(voteItemInfo.get("voteItemSeq").toString())) {
                    Map<String, Object> tmpVoteItemInfoMap = new ConcurrentHashMap<>();
                    tmpVoteItemInfoMap.put("voteSeq", voteItemInfo.get("voteSeq"));
                    tmpVoteItemInfoMap.put("voteItemSeq", voteItemInfo.get("voteItemSeq"));

                    // Rank Gap - pass
                    // TODO - VoteRate
                    //Double voteNumDouble = Double.parseDouble(voteNumInfo.get("voteNum").toString());
                    tmpVoteItemInfoMap.put("voteRate", 0);
                    tmpVoteItemInfoMap.put("voteNum", 0);
                    tmpVoteItemInfoMap.put("created", DateFormatUtils.format(new Date(), "yyyyMMddHHmmss"));

                    // Response List에 추가
                    rtVoteItemStatsList.add(tmpVoteItemInfoMap);
                }
            }

            // Hazelcase 등록 대상 Data - rtVoteItemStatsList
            // Insert or Update
            int del = deleteVoteItemStats(voteBasInfo.getId());
            logger.info("vote item delete schedule task - {} - {} - insert {} ", voteBasInfo.getId(), del, rtVoteItemStatsList.size());

            int rankNum = 1;

            for (Map<String, Object> voteItemStatsMap : rtVoteItemStatsList) {
                insertVoteItemStats(voteItemStatsMap, rankNum);
                rankNum++;
            }
        }
    }

    private Integer getTotalVoteCnt(String voteSeq) {
        // TODO - Hazelcast 내용 추가.
        String query = "SELECT count(*) AS voteNum " +
                "FROM " + voteSeq + "_voteItemHstByMbr " ;

        Map retMap = jdbcTemplate.queryForMap(query);
        return Integer.parseInt(retMap.get("voteNum").toString());
    }

    private List<Map<String, Object>> getVoteNumByVoteItemList(String voteSeq) {
        // TODO - Hazelcast 내용 추가.
        String query = "SELECT vi.voteSeq, vihbm.voteItemSeq, count(*) AS voteNum " +
                "FROM " + voteSeq + "_voteItemHstByMbr vihbm, voteItemInfo vi " +
                "WHERE vihbm.voteItemSeq=vi.voteItemSeq " +
                "GROUP BY vihbm.voteItemSeq " +
                "ORDER BY voteNum DESC";

        return jdbcTemplate.queryForList(query);
    }


    private int deleteVoteItemStats(String voteSeq) {
        String deleteQuery = "DELETE  FROM voteItemStats WHERE voteSeq=?";
        try {
            return jdbcTemplate.update(deleteQuery, voteSeq);
        } catch (Exception e) {
            return 0;
        }
    }

    private void insertVoteItemStats(Map<String, Object> voteItemStats, int rankNum) {
        String insertQuery = "INSERT INTO voteItemStats (voteSeq, voteItemSeq, rankNum, rankGapNum, voteRate, voteNum, owner, created) "
                            + "VALUES(?,?,?,?,?,?,?,?)";
        jdbcTemplate.update(insertQuery,
                voteItemStats.get("voteSeq"),voteItemStats.get("voteItemSeq"),rankNum, 0,
                voteItemStats.get("voteRate"),voteItemStats.get("voteNum"),"anonymous",voteItemStats.get("created"));
    }

}
