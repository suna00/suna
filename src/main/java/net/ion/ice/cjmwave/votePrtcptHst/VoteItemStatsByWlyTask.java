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

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service("voteItemStatsByWlyTask")
public class VoteItemStatsByWlyTask {
    private static Logger logger = LoggerFactory.getLogger(VoteItemStatsByWlyTask.class);

    public static final String VOTE_BAS_INFO = "voteBasInfo";
    public static final String VOTE_ITEM_INFO = "voteItemInfo";

    @Autowired
    NodeService nodeService;

    JdbcTemplate jdbcTemplate;

    public void execVoteItemStatsByWly() {

        logger.info("start execVoteItemStatsByWly");

        if (jdbcTemplate == null) {
            jdbcTemplate = NodeUtils.getNodeBindingService().getNodeBindingInfo(VOTE_BAS_INFO).getJdbcTemplate();
        }

        String perdStDate = getCurMonday();
        String perdFnsDate = getCurSunday();

        Date now = new Date();
        String voteDate = DateFormatUtils.format(now, "yyyyMMddHHmmss");
        // 투표 기간안에 있는 모든 VoteBasInfo 조회
        List<Node> voteBasInfoList = NodeUtils.getNodeList(VOTE_BAS_INFO, "pstngStDt_below=" + voteDate + "&pstngFnsDt_above="+ voteDate);

        for (Node voteBasInfo : voteBasInfoList) {
            List<Map<String, Object>> voteItemStatsHstByWlyList
                                = selectVoteItemStatsHstByWlyList(voteBasInfo.getId(), perdStDate, perdFnsDate);
            List<Map<String, Object>> insertVoteItemStatsByWlyList = new ArrayList<>();
            List<String> checkVoteItemSeqList = new ArrayList<>();
            Integer rankNum = 1;
            for (Map<String, Object> voteItemStatsHstByWly : voteItemStatsHstByWlyList) {
                voteItemStatsHstByWly.put("rankNum", rankNum);
                voteItemStatsHstByWly.put("owner", "anonymous");
                voteItemStatsHstByWly.put("created", now);

                insertVoteItemStatsByWlyList.add(voteItemStatsHstByWly);
                checkVoteItemSeqList.add(voteItemStatsHstByWly.get("voteItemSeq").toString());

                rankNum++;
            }

            // TODO - 투표 내역이 없는 항목 적용.
            // VoteSeq에 해당 하는 모든 voteItemInfo 가져오기
            List<Node> voteItemInfoList = NodeUtils.getNodeList(VOTE_ITEM_INFO, "voteSeq_matching=" + voteBasInfo.getId());
            for (Node voteItemInfo : voteItemInfoList) {
                if(!checkVoteItemSeqList.contains(voteItemInfo.getStringValue("voteItemSeq"))) {

                    //perdStDate, perdFnsDate ,voteSeq, voteItemSeq
                    Map<String, Object> tmpVoteItemInfoMap = new ConcurrentHashMap<>();
                    tmpVoteItemInfoMap.put("perdStDate", perdStDate);
                    tmpVoteItemInfoMap.put("perdFnsDate", perdFnsDate);
                    tmpVoteItemInfoMap.put("voteSeq", voteItemInfo.get("voteSeq"));
                    tmpVoteItemInfoMap.put("voteItemSeq", voteItemInfo.get("voteItemSeq"));

                    tmpVoteItemInfoMap.put("rankNum", rankNum);
                    tmpVoteItemInfoMap.put("owner", "anonymous");
                    tmpVoteItemInfoMap.put("created", now);

                    // Response List에 추가
                    insertVoteItemStatsByWlyList.add(tmpVoteItemInfoMap);
                    rankNum++;
                }
            }

            for (Map<String, Object> voteItemStatsByWly: insertVoteItemStatsByWlyList) {
                String searchText = "perdStDate_matching=" + voteItemStatsByWly.get("perdStDate").toString()
                        + "&perdFnsDate_matching=" + voteItemStatsByWly.get("perdFnsDate").toString()
                        + "&voteSeq_matching=" + voteItemStatsByWly.get("voteSeq").toString()
                        + "&rankNum_matching=" + voteItemStatsByWly.get("rankNum").toString();
                List<Node> voteItemInfoNodeList = NodeUtils.getQueryList("voteItemStatsByWly", searchText);

                if (voteItemInfoNodeList.size()>0) {
                    Node voteItemStats = nodeService.updateNode(voteItemStatsByWly, "voteItemStatsByWly");
                } else {
                    Node voteItemStats = nodeService.createNode(voteItemStatsByWly, "voteItemStatsByWly");
                }
            }
        }

        logger.info("complete execVoteItemStatsByWly");

    }

    private List<Map<String, Object>> selectVoteItemStatsHstByWlyList(String voteSeq, String perdStDate, String perdFnsDate) {
        String selectListQuery = "  SELECT  perdStDate, perdFnsDate ,voteSeq, voteItemSeq, sum(voteNum) AS voteNum " +
                                "  FROM voteItemStatsHstByWly " +
                                "  WHERE voteSeq=? AND perdStDate=? AND perdFnsDate=?" +
                                "  GROUP BY voteItemSeq, voteNum " +
                                "  ORDER BY voteNum DESC ";
        return jdbcTemplate.queryForList(selectListQuery, voteSeq, perdStDate, perdFnsDate);
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
}
