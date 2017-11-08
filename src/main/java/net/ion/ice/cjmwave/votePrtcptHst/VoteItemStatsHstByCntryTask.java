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

import java.util.*;

@Service("voteItemStatsHstByCntryTask")
public class VoteItemStatsHstByCntryTask {

    private static Logger logger = LoggerFactory.getLogger(VoteItemStatsHstByCntryTask.class);

    public static final String VOTE_BAS_INFO = "voteBasInfo";
    public static final Integer SELECT_LIST_COUNT = 500000;

    @Autowired
    NodeService nodeService;

    @Autowired
    private DBService dbService ;

    private JdbcTemplate jdbcTemplate;
    private JdbcTemplate jdbcTemplate_replica;

    public void execVoteItemStatsHstByCntry() {

        logger.info("start schedule task - execVoteItemStatsHstByCntry");
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

            logger.info("vote item stat schedule task - {} - {} ", voteBasInfo.getId(), voteBasInfo.getStringValue("voteNm"));

            Integer lastHstSeq = 0;
            String selectLastHstSeqQuery = "SELECT max(hstSeq) AS lastSeq FROM voteItemStatsHstByCntry WHERE voteSeq=?";
            Map<String, Object> startHstSeqMap = jdbcTemplate.queryForMap(selectLastHstSeqQuery, voteBasInfo.getId());
            lastHstSeq = startHstSeqMap.get("lastSeq")==null ? 0 : Integer.parseInt(startHstSeqMap.get("lastSeq").toString());

            String searchStartSeqQuery = "SELECT min(seq) AS startSeq FROM " + voteBasInfo.getId() + "_voteItemHstByMbr WHERE seq>?";
            Map<String, Object> startSeqMap = jdbcTemplate_replica.queryForMap(searchStartSeqQuery, lastHstSeq);
            Integer startSeq = 0 ;
            if (startSeqMap!=null && startSeqMap.get("startSeq")!=null) {
                startSeq = Integer.parseInt(startSeqMap.get("startSeq").toString());
            }
            Integer lastSeq = startSeq + SELECT_LIST_COUNT ;
            logger.info("vote item hstseq - {} - {} to {} ", voteBasInfo.getId(), startSeq, lastSeq);
            List<Map<String,Object>> voteItemHstInfoList = selectVoteItemHstInfoList(voteBasInfo.getId(), startSeq, lastSeq);

            logger.info("vote item infolist - {} - {} ", voteBasInfo.getId(), voteItemHstInfoList==null ? 0 : voteItemHstInfoList.size());

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
                voteItemStatsHstByMbr.put("created", new Date());

                mergeVoteItemStatsHstByCntry(voteItemStatsHstByMbr);
            }

        }
        logger.info("complete schedule task - execVoteItemStatsHstByCntry");
    }

    private List<Map<String, Object>> selectVoteItemHstInfoList(String voteSeq, Integer startHstSeq, Integer maxSeq) {
        String selectListQuery =
                "SELECT voteItemSeq, if(length(cntryCd)!=3, 'OTHERS', ifnull(if(cntryCd='THR','OTHERS',cntryCd),'OTHERS') ) AS cntryCd, voteDate, count(seq) AS voteNum, max(seq) AS hstSeq " +
                "FROM ( SELECT v.seq, v.voteDate, v.voteItemSeq, v.mbrId " +
                "        , (SELECT cntryCd FROM mbrInfo WHERE snsTypeCd = v.snsTypeCd AND snsKey = v.snsKey) AS cntryCd " +
                "     FROM ( SELECT seq, voteDate, voteItemSeq, mbrId " +
                "               , SUBSTRING_INDEX(mbrId, '>', 1) AS snsTypeCd " +
                "               , SUBSTRING_INDEX(mbrId, '>', -1) AS snsKey " +
                "               , created " +
                "           FROM " + voteSeq + "_voteItemHstByMbr " +
                "           WHERE seq>=? AND seq<? " +
                "       ) v " +
                "   ) rt " +
                "GROUP BY rt.voteItemSeq, if(length(cntryCd)!=3, 'OTHERS', ifnull(if(cntryCd='THR','OTHERS',cntryCd),'OTHERS') ), rt.voteDate";
        List retList = null;
        try {
            retList = jdbcTemplate_replica.queryForList(selectListQuery, startHstSeq, maxSeq);
        } catch (Exception e) {
            return null;
        }
        return retList;
    }

    private void mergeVoteItemStatsHstByCntry(Map<String, Object> voteItemHstByCntry) {
        String insertQuery =
                "INSERT INTO voteItemStatsHstByCntry (voteSeq, voteItemSeq, cntryCd, voteNum, hstSeq, voteDate, created) " +
                "VALUES (?,?,?,?,?,?,?) on DUPLICATE KEY " +
                "UPDATE voteNum = (voteNum + ?), hstSeq=?, created=?";
        int com = jdbcTemplate.update(insertQuery,
                voteItemHstByCntry.get("voteSeq"), voteItemHstByCntry.get("voteItemSeq"),
                voteItemHstByCntry.get("cntryCd"), voteItemHstByCntry.get("voteNum"), voteItemHstByCntry.get("hstSeq"),
                voteItemHstByCntry.get("voteDate"), voteItemHstByCntry.get("created"),
                voteItemHstByCntry.get("voteNum"), voteItemHstByCntry.get("hstSeq"), voteItemHstByCntry.get("created"));

        logger.info("vote item merge sql - voteSeq : {} - voteItemSeq : {} - voteDate : {} - voteNum : {} - hstSeq : {} - result : {}",
                voteItemHstByCntry.get("voteSeq"), voteItemHstByCntry.get("voteItemSeq"),
                voteItemHstByCntry.get("voteDate"), voteItemHstByCntry.get("voteNum"), voteItemHstByCntry.get("hstSeq"), com);
    }
}
