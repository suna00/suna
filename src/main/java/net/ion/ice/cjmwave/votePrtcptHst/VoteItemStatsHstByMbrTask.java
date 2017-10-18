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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Service("voteItemStatsHstByMbrTask")
public class VoteItemStatsHstByMbrTask {

    private static Logger logger = LoggerFactory.getLogger(VoteItemStatsHstByMbrTask.class);

    public static final String VOTE_BAS_INFO = "voteBasInfo";
    public static final Integer SELECT_LIST_COUNT = 1000;

    @Autowired
    NodeService nodeService;

    JdbcTemplate jdbcTemplate;

    public void execVoteItemStatsHstByMbr() {

        logger.info("start schedule task - execVoteItemStatsHstByMbr");

        if (jdbcTemplate == null) {
            jdbcTemplate = NodeUtils.getNodeBindingService().getNodeBindingInfo(VOTE_BAS_INFO).getJdbcTemplate();
        }
        // 통계를 위한 대상 voteSeq List
        Date now = new Date();
        String voteDate = DateFormatUtils.format(now, "yyyyMMddHHmmss");
        // 투표 기간안에 있는 모든 VoteBasInfo 조회
        List<Node> voteBasInfoList = NodeUtils.getNodeList(VOTE_BAS_INFO, "pstngStDt_below=" + voteDate + "&pstngFnsDt_above="+ voteDate);

        for (Node voteBasInfo : voteBasInfoList) {
            // TODO - voteItemStatsHstByMbr Table에서 조회.
            Integer startHstSeq = 0;
            String selectLastHstSeqQuery = "SELECT max(hstSeq) AS lastSeq FROM voteItemStatsHstByMbr WHERE voteSeq=?";
            Map<String, Object> startHstSeqMap = jdbcTemplate.queryForMap(selectLastHstSeqQuery, voteBasInfo.getId());
            startHstSeq = startHstSeqMap.get("lastSeq")==null ? 0 : Integer.parseInt(startHstSeqMap.get("lastSeq").toString());
            startHstSeq += 1;

            List<Map<String,Object>> voteItemHstInfoList
                    = selectVoteItemHstInfoList(voteBasInfo.getId(), startHstSeq, SELECT_LIST_COUNT + startHstSeq, DateFormatUtils.format(now, "yyyyMMdd"));

            List<Map<String,Object>> insertVoteItemStatsHstByMbrList = new ArrayList<>();
            for (Map voteItemHstInfo : voteItemHstInfoList) {
                // add to list for insert to vote count table
                Integer hstSeq = voteItemHstInfo.get("hstSeq")==null ? 0 : Integer.parseInt(voteItemHstInfo.get("hstSeq").toString());
                if (hstSeq>startHstSeq) {
                    startHstSeq = hstSeq;
                }
                insertVoteItemStatsHstByMbrList.add(voteItemHstInfo);
            }

            for (Map voteItemStatsHstByMbr : insertVoteItemStatsHstByMbrList) {

                voteItemStatsHstByMbr.put("voteSeq", voteBasInfo.getId());
                voteItemStatsHstByMbr.put("hstSeq", startHstSeq);
                voteItemStatsHstByMbr.put("created", now);

                Map<String, Object> chkVoteItemStatsHstByMbr
                        = selectVoteItemHstInfo(voteBasInfo.getId(),
                        voteItemStatsHstByMbr.get("voteItemSeq").toString(),
                        voteItemStatsHstByMbr.get("mbrId").toString());
                if (chkVoteItemStatsHstByMbr == null) {
                    String mbrId = voteItemStatsHstByMbr.get("mbrId")==null ? null : voteItemStatsHstByMbr.get("mbrId").toString();
                    String[] mbrIdArr = null;
                    if (mbrId!=null) {
                        mbrIdArr = mbrId.split(">");
                    }

                    if (mbrIdArr!=null && mbrIdArr.length>1) {
                        voteItemStatsHstByMbr.put("snsTypeCd", mbrIdArr[0]);
                        voteItemStatsHstByMbr.put("snsKey", mbrIdArr[1]);

                        insertVoteItemStatsHstByMbr(voteItemStatsHstByMbr);
                    }

                } else {
                    Integer newVoteNum = 0;
                    if (voteItemStatsHstByMbr.get("voteNum") != null) {
                        newVoteNum = Integer.parseInt(voteItemStatsHstByMbr.get("voteNum").toString());
                    }
                    Integer chkVoteNum = 0;
                    if (chkVoteItemStatsHstByMbr.get("voteNum") != null) {
                        chkVoteNum = Integer.parseInt(chkVoteItemStatsHstByMbr.get("voteNum").toString());
                    }
                    voteItemStatsHstByMbr.put("voteNum", chkVoteNum + newVoteNum);

                    updatetVoteItemStatsHstByMbr(voteItemStatsHstByMbr);
                }
            }
        }
        logger.info("complete schedule task - execVoteItemStatsHstByMbr");
    }

    private Map<String, Object> selectVoteItemHstInfo(String voteSeq, String voteItemSeq, String mbrId) {
        String selectQuery = "SELECT voteSeq, voteItemSeq, mbrId, snsTypeCd, snsKey, voteNum, hstSeq, created "
                + "FROM voteItemStatsHstByMbr "
                + "WHERE voteSeq=? AND voteItemSeq=? AND mbrId=?";
        Map<String, Object> retMap = null;
        try {
            retMap = jdbcTemplate.queryForMap(selectQuery, voteSeq, voteItemSeq, mbrId);
        } catch (Exception e) {
            return null;
        }
        return retMap;
    }

    public void insertVoteItemStatsHstByMbr(Map<String, Object> voteItemHstByMbr) {
        String insertQuery = "INSERT INTO voteItemStatsHstByMbr (voteSeq, voteItemSeq, mbrId, snsTypeCd, snsKey, voteNum, hstSeq, voteDate, created) "
                + "VALUES (?,?,?,?,?,?,?,?,?)";
        //snsTypeCd, snsKey,
        jdbcTemplate.update(insertQuery,
                voteItemHstByMbr.get("voteSeq"), voteItemHstByMbr.get("voteItemSeq"),
                voteItemHstByMbr.get("mbrId"), voteItemHstByMbr.get("snsTypeCd"), voteItemHstByMbr.get("snsKey"),
                voteItemHstByMbr.get("voteNum"), voteItemHstByMbr.get("hstSeq"),
                voteItemHstByMbr.get("voteDate"), voteItemHstByMbr.get("created"));
    }

    public void updatetVoteItemStatsHstByMbr(Map<String, Object> voteItemHstByMbr) {
        String updateQuery = "UPDATE voteItemStatsHstByMbr "
                + "SET voteNum=?, hstSeq=?, voteDate=?, created=? "
                + "WHERE voteSeq=? AND voteItemSeq=? AND mbrId=?";
        jdbcTemplate.update(updateQuery,
                voteItemHstByMbr.get("voteNum"), voteItemHstByMbr.get("hstSeq"), voteItemHstByMbr.get("voteDate"), voteItemHstByMbr.get("created"),
                voteItemHstByMbr.get("voteSeq"), voteItemHstByMbr.get("voteItemSeq"), voteItemHstByMbr.get("mbrId"));
    }

    private List<Map<String, Object>> selectVoteItemHstInfoList(String voteSeq, Integer startHstSeq, Integer maxSeq, String voteDate) {
        String selectListQuery =
                "SELECT max(seq) AS hstSeq, voteItemSeq, mbrId, voteDate, count(seq) AS voteNum " +
                        "FROM (" +
                        "       SELECT seq, voteDate, voteItemSeq, mbrId, created " +
                        "       FROM " + voteSeq + "_voteItemHstByMbr " +
                        "       WHERE seq>=? AND seq<? AND voteDate=?" +
                        "     ) a " +
                        "GROUP BY voteItemSeq, mbrId " +
                        "ORDER BY hstSeq";
        List retList = null;
        try {
            //retList = jdbcTemplate.queryForList(selectListQuery, startHstSeq, maxSeq, "20171012");
            retList = jdbcTemplate.queryForList(selectListQuery, startHstSeq, maxSeq, voteDate);
        } catch (Exception e) {
            return null;
        }
        return retList;
    }
}
