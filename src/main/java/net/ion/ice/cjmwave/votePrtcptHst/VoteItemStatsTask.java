package net.ion.ice.cjmwave.votePrtcptHst;

import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeService;
import net.ion.ice.core.node.NodeUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service("voteItemStatsTask")
public class VoteItemStatsTask {

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
        Date now = new Date();
        String voteDate = DateFormatUtils.format(now, "yyyyMMddHHmmss");

        // 투표 기간안에 있는 모든 VoteBasInfo 조회
        List<Node> voteBasInfoList = NodeUtils.getNodeList(VOTE_BAS_INFO, "pstngStDt_below=" + voteDate + "&pstngFnsDt_above="+ voteDate);
        for (Node voteBasInfo : voteBasInfoList) {
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

            // VoteSeq에 해당 하는 모든 voteItemInfo 가져오기
            List<Node> voteItemInfoList = NodeUtils.getNodeList(VOTE_ITEM_INFO, "voteSeq_matching=" + voteBasInfo.getId());

            // 각 투표가 진행된 voteItem 정보 및 Count 조회
            List<Map<String, Object>> voteNumInfoList = getVoteNumByVoteItemList(voteBasInfo.getId());
            List<Map<String, Object>> rtVoteItemStatsList = new ArrayList<>();

            int rankNum = 1;
            List<String> checkVoteItemSeqList = new ArrayList<>();
            for (Map<String, Object> voteNumInfo : voteNumInfoList) {
                voteNumInfo.put("rankNum", rankNum);
                // Rank Gap - pass
                // TODO - VoteRate

                Integer voteNum = 0;
                if (voteNumInfo.get("voteNum") != null) {
                    voteNum = (Integer) voteNumInfo.get("voteNum");
                }
                Double voteNumDouble = Double.parseDouble(String.valueOf(voteNum));
                BigDecimal voteCnt = new BigDecimal(voteNumDouble*100);
                BigDecimal totalCnt = new BigDecimal(totalVoteNum);

                BigDecimal voteRate = voteCnt.divide(totalCnt, 1, BigDecimal.ROUND_HALF_UP);
                voteNumInfo.put("voteRate", voteRate.doubleValue());
                voteNumInfo.put("voteNum", voteNumInfo.get("voteNum"));
                voteNumInfo.put("created", now);

                // Response List에 추가
                rtVoteItemStatsList.add(voteNumInfo);
                checkVoteItemSeqList.add(voteNumInfo.get("voteItemSeq").toString());
                rankNum++;
            }

            for (Node voteItemInfo : voteItemInfoList) {
                if(!checkVoteItemSeqList.contains(voteItemInfo.get("voteItemSeq").toString())) {
                    Map<String, Object> tmpVoteItemInfoMap = new ConcurrentHashMap<>();
                    tmpVoteItemInfoMap.put("voteSeq", voteItemInfo.get("voteSeq"));
                    tmpVoteItemInfoMap.put("voteItemSeq", voteItemInfo.get("voteItemSeq"));

                    tmpVoteItemInfoMap.put("rankNum", rankNum);
                    // Rank Gap - pass
                    // TODO - VoteRate
                    //Double voteNumDouble = Double.parseDouble(voteNumInfo.get("voteNum").toString());
                    tmpVoteItemInfoMap.put("voteRate", 0);
                    tmpVoteItemInfoMap.put("voteNum", 0);
                    tmpVoteItemInfoMap.put("created", now);

                    // Response List에 추가
                    rtVoteItemStatsList.add(tmpVoteItemInfoMap);
                    rankNum++;
                }
            }

            // Hazelcase 등록 대상 Data - rtVoteItemStatsList
            // Insert or Update
            for (Map<String, Object> voteItemStatsMap : rtVoteItemStatsList) {
                Map<String, Object> checkVoteItemStats = selectVoteItemStats(voteItemStatsMap.get("voteSeq").toString(),
                        Integer.parseInt(voteItemStatsMap.get("rankNum").toString()));
                if (checkVoteItemStats == null) {
                    insertVoteItemStats(voteItemStatsMap);
                } else {
                    updateVoteItemStats(voteItemStatsMap);
                }
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


    private Map<String, Object> selectVoteItemStats(String voteSeq, Integer rankNum) {
        String selectQuery = "SELECT voteSeq, voteItemSeq, rankNum, rankGapNum, voteRate, voteNum, owner, created " +
                "FROM voteItemStats WHERE voteSeq=? AND rankNum=?";
        try {
            Map retMap = jdbcTemplate.queryForMap(selectQuery, voteSeq, rankNum);
            return retMap;
        } catch (Exception e) {
            return null;
        }
    }

    private void insertVoteItemStats(Map<String, Object> voteItemStats) {
        String insertQuery = "INSERT INTO voteItemStats (voteSeq, voteItemSeq, rankNum, rankGapNum, voteRate, voteNum, owner, created) "
                            + "VALUES(?,?,?,?,?,?,?,?)";
        jdbcTemplate.update(insertQuery,
                voteItemStats.get("voteSeq"),voteItemStats.get("voteItemSeq"),voteItemStats.get("rankNum"),0,
                voteItemStats.get("voteRate"),voteItemStats.get("voteNum"),"anonymous",voteItemStats.get("created"));
    }

    private void updateVoteItemStats(Map<String, Object> voteItemStats) {
        String updateQuery = "UPDATE voteItemStats SET voteItemSeq= ?, voteRate= ?, voteNum= ?, created= ? WHERE voteSeq=? AND rankNum=?";

        jdbcTemplate.update(updateQuery,
                voteItemStats.get("voteItemSeq"), voteItemStats.get("voteRate"), voteItemStats.get("voteNum"), voteItemStats.get("created"),
                voteItemStats.get("voteSeq"), voteItemStats.get("rankNum"));
    }
}
