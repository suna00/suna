package net.ion.ice.cjmwave.votePrtcptHst;

import net.ion.ice.core.context.ExecuteContext;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeService;
import net.ion.ice.core.node.NodeUtils;
import net.ion.ice.schedule.ScheduleController;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.xml.crypto.Data;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by latinus.hong on 2017. 9. 20.
 */
@Service("voteStatisticsService")
public class VoteStatisticsService {

    public static final String VOTE_BAS_INFO = "voteBasInfo";
    public static final String VOTE_ITEM_INFO = "voteItemInfo";
    public static final String SERS_VOTE_ITEM_INFO = "sersVoteItemInfo" ;
    public static final String VOTE_ITEM_STATS = "voteItemStats";

    @Autowired
    NodeService nodeService;

    JdbcTemplate jdbcTemplate;

    public void IFMwave101(ExecuteContext context) {

        if (jdbcTemplate==null) {
            jdbcTemplate = NodeUtils.getNodeBindingService().getNodeBindingInfo(VOTE_BAS_INFO).getJdbcTemplate();
        }

        // 통계를 위한 대상 voteSeq List
        Date now = new Date();
        String voteDate = DateFormatUtils.format(now, "yyyyMMddHHmmss");

        //}&pstngStDt_above={{:concatStr(voteYear,0101)}}&pstngStDt_below={{:concatStr(voteYear,1231)}}
        List<Node> voteBasInfoList = NodeUtils.getNodeList(VOTE_BAS_INFO, "pstngStDt_below=" + voteDate + "&pstngFnsDt_above="+ voteDate);
        List<Node> voteBasInfoList2 = new ArrayList<>();    // TODO - For Test
        voteBasInfoList2.add( NodeUtils.getNode(VOTE_BAS_INFO, "800033"));

        voteBasInfoList = new ArrayList<>();
        for (Node voteBasInfo : voteBasInfoList2) {

            // 각 voteItem에 해당하는 투표수
            List<Map<String, Object>> voteNumInfoList = getVoteNumByVoteItemList(voteBasInfo.getId());

            int rankNum = 1;
            int totalVoteNum = selectTotalVoteCnt(voteBasInfo.getId());
            for (Map<String, Object> voteNumInfo : voteNumInfoList) {

                // 각 voteSeq과 voteItemSeq에 해당하는 voteItemStatus 항목 생성
                Map<String, Object> voteItemStats = selectVoteItemStats(voteNumInfo.get("voteSeq").toString(), rankNum);
                if (voteItemStats == null) {
                    // TODO - create Node
                    Map<String, Object> voteItemStatsMap = new ConcurrentHashMap<>();
                    voteItemStatsMap.put("voteSeq", voteNumInfo.get("voteSeq"));
                    voteItemStatsMap.put("voteItemSeq", voteNumInfo.get("voteItemSeq"));
                    voteItemStatsMap.put("rankNum", rankNum);
                    voteItemStatsMap.put("created", now);
                    //voteItemStats = nodeService.createNode(voteItemStatsMap, VOTE_ITEM_STATS);

                    // insert
                    insertVoteItemStats(voteItemStatsMap);
                    voteItemStats = selectVoteItemStats(voteItemStatsMap.get("voteSeq").toString(),
                                                    Integer.parseInt(voteItemStatsMap.get("rankNum").toString()));
                }

                voteItemStats.put("voteSeq", voteNumInfo.get("voteSeq"));
                voteItemStats.put("voteItemSeq", voteNumInfo.get("voteItemSeq"));
                voteItemStats.put("created", now);
                voteItemStats.put("rankNum", rankNum++);
                // Rank Gap - pass
                // TODO - VoteRate
                Double voteNumDouble = Double.parseDouble(voteNumInfo.get("voteNum").toString());
                voteItemStats.put("voteRate", voteNumDouble/totalVoteNum);
                voteItemStats.put("voteNum", voteNumInfo.get("voteNum"));
                voteItemStats.put("created", now);

                //totalVoteNum += Integer.parseInt(voteNumInfo.get("voteNum").toString());
                // TODO - 3. 생성된 항목 voteItemStatus에 등록 또는 업데이트
                updateVoteItemStats(voteItemStats);
            }
        }
        context.setResult(voteBasInfoList);
    }

    private Integer selectTotalVoteCnt(String voteSeq) {
        if (jdbcTemplate==null) {
            jdbcTemplate = NodeUtils.getNodeBindingService().getNodeBindingInfo(VOTE_BAS_INFO).getJdbcTemplate();
        }

        String query = "SELECT count(*) AS voteNum " +
                "FROM " + voteSeq + "_voteItemHstByMbr " ;

        Map retMap = jdbcTemplate.queryForMap(query);
        return Integer.parseInt(retMap.get("voteNum").toString());
    }

    private List<Map<String, Object>> getVoteNumByVoteItemList(String voteSeq) {

        if (jdbcTemplate==null) {
            jdbcTemplate = NodeUtils.getNodeBindingService().getNodeBindingInfo(VOTE_BAS_INFO).getJdbcTemplate();
        }

        String query = "SELECT vi.voteSeq, vihbm.voteItemSeq, count(*) AS voteNum " +
                        "FROM " + voteSeq + "_voteItemHstByMbr vihbm, voteItemInfo vi " +
                        "WHERE vihbm.voteItemSeq=vi.voteItemSeq " +
                        "GROUP BY vihbm.voteItemSeq " +
                        "ORDER BY voteNum DESC";

        return jdbcTemplate.queryForList(query);
    }

    private Map<String, Object> selectVoteItemStats(String voteSeq, Integer rankNum) {
        if (jdbcTemplate==null) {
            jdbcTemplate = NodeUtils.getNodeBindingService().getNodeBindingInfo(VOTE_BAS_INFO).getJdbcTemplate();
        }
        String selectQuery = "SELECT voteSeq, voteItemSeq, rankNum, rankGapNum, voteRate, voteNum, owner, created " +
                            "FROM voteItemStats WHERE voteSeq=? AND rankNum=?";

        return jdbcTemplate.queryForMap(selectQuery, voteSeq, rankNum);
    }

    private void insertVoteItemStats(Map<String, Object> voteItemStats) {
        if (jdbcTemplate==null) {
            jdbcTemplate = NodeUtils.getNodeBindingService().getNodeBindingInfo(VOTE_BAS_INFO).getJdbcTemplate();
        }

        String insertQuery = "INSERT INTO voteItemStats (voteSeq, voteItemSeq, rankNum, rankGapNum, voteRate, voteNum, owner, created) VALUES(?,?,?,?,?,?,?,?)";
        jdbcTemplate.update(insertQuery,
                voteItemStats.get("voteSeq"),voteItemStats.get("voteItemSeq"),voteItemStats.get("rankNum"),0,
                voteItemStats.get("voteRate"),voteItemStats.get("voteNum"),"anonymous",voteItemStats.get("created"));
    }

    private void updateVoteItemStats(Map<String, Object> voteItemStats) {
        if (jdbcTemplate==null) {
            jdbcTemplate = NodeUtils.getNodeBindingService().getNodeBindingInfo(VOTE_BAS_INFO).getJdbcTemplate();
        }

        String updateQuery = "UPDATE voteItemStats SET voteItemSeq= ?, voteRate= ?, voteNum= ? WHERE voteSeq=? AND rankNum=?";

        jdbcTemplate.update(updateQuery, voteItemStats.get("voteItemSeq"), voteItemStats.get("voteRate"), voteItemStats.get("voteNum"),
                            voteItemStats.get("voteSeq"), voteItemStats.get("rankNum"));

    }
}
