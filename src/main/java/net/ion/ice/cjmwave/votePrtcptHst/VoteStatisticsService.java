package net.ion.ice.cjmwave.votePrtcptHst;

import net.ion.ice.core.context.ExecuteContext;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeService;
import net.ion.ice.core.node.NodeUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.xml.crypto.Data;
import java.math.BigDecimal;
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

        // 투표 기간안에 있는 모든 VoteBasInfo 조회
        List<Node> voteBasInfoList = NodeUtils.getNodeList(VOTE_BAS_INFO, "pstngStDt_below=" + voteDate + "&pstngFnsDt_above="+ voteDate);
        for (Node voteBasInfo : voteBasInfoList) {
            // 총 투표수
            Integer totalVoteNum = selectTotalVoteCnt(voteBasInfo.getId());

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
                Double voteNumDouble = Double.parseDouble(voteNumInfo.get("voteNum").toString());
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

        try {
            Map retMap = jdbcTemplate.queryForMap(selectQuery, voteSeq, rankNum);
            return retMap;
        } catch (Exception e) {
            return null;
        }
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

        String updateQuery = "UPDATE voteItemStats SET voteItemSeq= ?, voteRate= ?, voteNum= ?, created= ? WHERE voteSeq=? AND rankNum=?";

        jdbcTemplate.update(updateQuery,
                    voteItemStats.get("voteItemSeq"), voteItemStats.get("voteRate"), voteItemStats.get("voteNum"), voteItemStats.get("created"),
                    voteItemStats.get("voteSeq"), voteItemStats.get("rankNum"));
    }


    public void voteItemStatsBySex(ExecuteContext context) {
        if (jdbcTemplate==null) {
            jdbcTemplate = NodeUtils.getNodeBindingService().getNodeBindingInfo(VOTE_BAS_INFO).getJdbcTemplate();
        }

        // 통계를 위한 대상 voteSeq List
        Date now = new Date();
        String voteDate = DateFormatUtils.format(now, "yyyyMMddHHmmss");

        // 투표 기간안에 있는 모든 VoteBasInfo 조회
        List<Node> voteBasInfoList = NodeUtils.getNodeList(VOTE_BAS_INFO, "pstngStDt_below=" + voteDate + "&pstngFnsDt_above="+ voteDate);

        for (Node voteBasInfo :voteBasInfoList) {
            String voteSeq = voteBasInfo.getId();
            Integer totalVoteNum = selectTotalVoteCnt(voteBasInfo.getId());
            //getVoteNumByVoteItemList(voteSeq);

            // 각 투표가 진행된 voteItem 정보 및 Count 조회
            List<Map<String, Object>> voteItemInfoList = getVoteItemListByVoteSeq(voteSeq);
            // 국가별
            List<Map<String, Object>> voteItemStatsByCntryList = new ArrayList<>();
            // 성별
            List<Map<String, Object>> voteItemStatsBySexList = new ArrayList<>();
            // 주간별
            List<Map<String, Object>> voteItemStatsByWlyList = new ArrayList<>();

            int rankNum = 1;
            List<String> checkVoteItemSeqList = new ArrayList<>();
            for (Map<String, Object> voteItemMap : voteItemInfoList) {

                String owner = "anonymous";
                String voteItemSeq = voteItemMap.get("voteItemSeq").toString();
                String voteNum = voteItemMap.get("voteNum")==null ? "0" : voteItemMap.get("voteNum").toString();

                Double voteNumDouble = Double.parseDouble(voteNum);
                BigDecimal voteCnt = new BigDecimal(voteNumDouble*100);
                BigDecimal totalCnt = new BigDecimal(totalVoteNum);
                BigDecimal voteRate = voteCnt.divide(totalCnt, 1, BigDecimal.ROUND_HALF_UP);

                String mbrId = voteItemMap.get("mbrId") != null ? voteItemMap.get("mbrId").toString() : "";
                String[] mbrIdArr = mbrId.split(">");
                if (mbrIdArr.length > 1) {

                    List<Node> mbrInfoList = NodeUtils.getNodeList("mbrInfo",
                            "snsTypeCd_matching=" + mbrIdArr[0] + "&snsKey_matching=" + mbrIdArr[1]);
                    if (mbrInfoList.size()>0) {
                        Node mbrInfo = mbrInfoList.get(0);

                        Map<String, Object> voteItemStatsBySexMap = new ConcurrentHashMap<>();
                        voteItemStatsBySexMap.put("voteSeq", voteSeq);
                        voteItemStatsBySexMap.put("voteItemSeq", voteItemSeq);

                        String sexCd = mbrInfo.get("sexCd")==null ? null : mbrInfo.get("sexCd").toString();
                        if (sexCd!=null) {
                            String[] sexCdArr = sexCd.split(">");
                            if (sexCdArr.length==2) {
                                voteItemStatsBySexMap.put("sexCd", sexCdArr[1]);
                            }
                        }
                        voteItemStatsBySexMap.put("voteRate", voteRate);
                        voteItemStatsBySexMap.put("voteNum", voteNum);
                        voteItemStatsBySexMap.put("owner", "anonymous");
                        voteItemStatsBySexMap.put("created", now);

                        // 결과 값 등록
                        voteItemStatsBySexList.add(voteItemStatsBySexMap);
                        //insertVoteItemStatsBySex(voteItemStatsBySexMap);
                    }
                }
            }

            // Hazelcase 등록 대상 Data - rtVoteItemStatsList
            // Insert or Update
            for (Map<String, Object> voteItemStatsBySexMap : voteItemStatsBySexList) {
                String sexCd = voteItemStatsBySexMap.get("sexCd")==null ? null : voteItemStatsBySexMap.get("sexCd").toString();
                if (sexCd!=null) {
                    Map<String, Object> checkVoteItemStats = selectVoteItemStatsBySex(voteItemStatsBySexMap.get("voteSeq").toString(),
                            voteItemStatsBySexMap.get("voteItemSeq").toString(), sexCd);
                    if (checkVoteItemStats == null) {
                        //String sexCd = mbrInfo.get("sexCd")==null? null : mbrInfo.get("sexCd").toString();
                        insertVoteItemStatsBySex(voteItemStatsBySexMap);
                    } else {
                        updateVoteItemStatsBySex(voteItemStatsBySexMap);
                    }
                }
            }

        }
        context.setResult(voteBasInfoList);
    }

    private Map<String,Object> selectVoteItemStatsBySex(String voteSeq, String voteItemSeq, String sexCd) {
        String selectQuery = "SELECT voteSeq, voteItemSeq, sexCd, voteNum, voteRate, created "
                            + "FROM voteItemStatsBySex WHERE voteSeq=? AND voteItemSeq=? AND sexCd=?";
        Map retMap = null;
        try {
            retMap = jdbcTemplate.queryForMap(selectQuery, voteSeq, voteItemSeq, sexCd);
        } catch (Exception e) {
            return null;
        }
        return retMap;
    }

    private List<Map<String, Object>> getVoteItemListByVoteSeq(String voteSeq) {

        String voteItemSql = "SELECT voteItemSeq, mbrId, count(seq) AS voteNum "
                            + "FROM "+ voteSeq + "_voteItemHstByMbr "
                            + "GROUP BY voteItemSeq, mbrId ";
        return jdbcTemplate.queryForList(voteItemSql);
    }

    private void insertVoteItemStatsBySex(Map<String, Object> voteItemStatsBySexMap) {
        if (jdbcTemplate==null) {
            jdbcTemplate = NodeUtils.getNodeBindingService().getNodeBindingInfo(VOTE_BAS_INFO).getJdbcTemplate();
        }
        String sexCd = null;
        if (voteItemStatsBySexMap.get("sexCd") != null) {
            sexCd = voteItemStatsBySexMap.get("sexCd").toString();
        }
        String insertQuery = "INSERT INTO voteItemStatsBySex (voteSeq, voteItemSeq, sexCd, voteRate, voteNum, owner, created) VALUES(?,?,?,?,?,?,?)";
        jdbcTemplate.update(insertQuery,
                voteItemStatsBySexMap.get("voteSeq"),voteItemStatsBySexMap.get("voteItemSeq"),sexCd,
                voteItemStatsBySexMap.get("voteRate"),voteItemStatsBySexMap.get("voteNum"),"anonymous",voteItemStatsBySexMap.get("created"));
    }

    private void updateVoteItemStatsBySex(Map<String, Object> voteItemStatsBySexMap) {
        if (jdbcTemplate==null) {
            jdbcTemplate = NodeUtils.getNodeBindingService().getNodeBindingInfo(VOTE_BAS_INFO).getJdbcTemplate();
        }

        String updateQuery = "UPDATE voteItemStatsBySex SET voteRate= ?, voteNum= ?, created= ? WHERE voteSeq=? AND voteItemSeq=? AND sexCd=?";

        jdbcTemplate.update(updateQuery,
                voteItemStatsBySexMap.get("voteRate"), voteItemStatsBySexMap.get("voteNum"), voteItemStatsBySexMap.get("created"),
                voteItemStatsBySexMap.get("voteSeq"), voteItemStatsBySexMap.get("voteItemSeq"), voteItemStatsBySexMap.get("sexCd"));
    }

}
