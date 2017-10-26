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

@Service("voteItemStatsHstByMbrTask")
public class VoteItemStatsHstByMbrTask {

    private static Logger logger = LoggerFactory.getLogger(VoteItemStatsHstByMbrTask.class);

    public static final String VOTE_BAS_INFO = "voteBasInfo";
    public static final Integer SELECT_LIST_COUNT = 30;

    @Autowired
    NodeService nodeService;

    JdbcTemplate jdbcTemplate;

    public void execVoteItemStatsHstByMbr() {

        logger.info("start schedule task - execVoteItemStatsHstByMbr");

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
            // TODO - voteItemStatsHstByMbr Table에서 조회.
            logger.info("vote item stat schedule task - {} - {} ", voteBasInfo.getId(), voteBasInfo.getStringValue("voteNm"));

            Integer lastHstSeq = 0;
            String selectLastHstSeqQuery = "SELECT max(hstSeq) AS lastSeq FROM voteItemStatsHstByMbr WHERE voteSeq=?";
            Map<String, Object> startHstSeqMap = jdbcTemplate.queryForMap(selectLastHstSeqQuery, voteBasInfo.getId());
            lastHstSeq = startHstSeqMap.get("lastSeq")==null ? 0 : Integer.parseInt(startHstSeqMap.get("lastSeq").toString());

            String searchStartSeqQuery = "SELECT min(seq) AS startSeq FROM " + voteBasInfo.getId() + "_voteItemHstByMbr WHERE seq>?";
            Map<String, Object> startSeqMap = jdbcTemplate.queryForMap(searchStartSeqQuery, lastHstSeq);
            Integer startSeq = 0 ;
            if (startSeqMap!=null && startSeqMap.get("startSeq")!=null) {
                startSeq = Integer.parseInt(startSeqMap.get("startSeq").toString());
            }

            logger.info("vote item hstseq - {} - {} ", voteBasInfo.getId(), startSeq);

            List<Map<String,Object>> voteItemHstInfoList
                    = selectVoteItemHstInfoList(voteBasInfo.getId(), startSeq, SELECT_LIST_COUNT + startSeq, DateFormatUtils.format(new Date(), "yyyyMMdd"));

            logger.info("vote item infolist - {} - {} ", voteBasInfo.getId(), voteItemHstInfoList.size());

            List<Map<String,Object>> insertVoteItemStatsHstByMbrList = new ArrayList<>();
            for (Map voteItemHstInfo : voteItemHstInfoList) {
                // add to list for insert to vote count table
                Integer hstSeq = voteItemHstInfo.get("hstSeq")==null ? 0 : Integer.parseInt(voteItemHstInfo.get("hstSeq").toString());
                if (hstSeq>startSeq) {
                    startSeq = hstSeq;
                }
                insertVoteItemStatsHstByMbrList.add(voteItemHstInfo);
            }

            for (Map voteItemStatsHstByMbr : insertVoteItemStatsHstByMbrList) {

                voteItemStatsHstByMbr.put("voteSeq", voteBasInfo.getId());
                voteItemStatsHstByMbr.put("hstSeq", startSeq);
                voteItemStatsHstByMbr.put("created", now);

                String mbrId = voteItemStatsHstByMbr.get("mbrId")==null ? null : voteItemStatsHstByMbr.get("mbrId").toString();
                String[] mbrIdArr = null;
                if (mbrId!=null) {
                    mbrIdArr = mbrId.split(">");
                }

                if (mbrIdArr!=null && mbrIdArr.length>1) {
                    voteItemStatsHstByMbr.put("snsTypeCd", mbrIdArr[0]);
                    voteItemStatsHstByMbr.put("snsKey", mbrIdArr[1]);
                }

                mergeVoteItemStatsHstByMbr(voteItemStatsHstByMbr);

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

    public void mergeVoteItemStatsHstByMbr(Map<String, Object> voteItemHstByMbr) {
        String insertQuery = "INSERT INTO voteItemStatsHstByMbr (voteSeq, voteItemSeq, mbrId, snsTypeCd, snsKey, voteNum, hstSeq, voteDate, created) "
                + "VALUES (?,?,?,?,?,?,?,?,?) on DUPLICATE KEY " +
                "UPDATE voteNum = (voteNum + ?), hstSeq=?, voteDate=?, created=?";
        //snsTypeCd, snsKey,
        int com = jdbcTemplate.update(insertQuery,
                voteItemHstByMbr.get("voteSeq"), voteItemHstByMbr.get("voteItemSeq"),
                voteItemHstByMbr.get("mbrId"), voteItemHstByMbr.get("snsTypeCd"), voteItemHstByMbr.get("snsKey"),
                voteItemHstByMbr.get("voteNum"), voteItemHstByMbr.get("hstSeq"),
                voteItemHstByMbr.get("voteDate"), voteItemHstByMbr.get("created"),
                voteItemHstByMbr.get("voteNum"), voteItemHstByMbr.get("hstSeq"), voteItemHstByMbr.get("voteDate"), voteItemHstByMbr.get("created"));
        logger.info("vote item merge sql - {} - {} - {}", voteItemHstByMbr.get("voteSeq"), voteItemHstByMbr.get("voteNum"), com);


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
