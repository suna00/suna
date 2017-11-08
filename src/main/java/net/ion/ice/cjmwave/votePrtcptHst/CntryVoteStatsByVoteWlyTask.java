package net.ion.ice.cjmwave.votePrtcptHst;

import net.ion.ice.cjmwave.votePrtcptHst.vo.CntryVoteByVoteVO;
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

@Service("cntryVoteStatsByVoteWlyTask")
public class CntryVoteStatsByVoteWlyTask {

    private static Logger logger = LoggerFactory.getLogger(CntryVoteStatsByVoteWlyTask.class);

    public static final String VOTE_BAS_INFO = "voteBasInfo";

    @Autowired
    NodeService nodeService;

    @Autowired
    private DBService dbService ;

    private JdbcTemplate jdbcTemplate;
    private JdbcTemplate jdbcTemplate_replica;

    /**
     */
    public void execCntryVoteStatsByVoteWly() {

        logger.info("start schedule task - execCntryVoteStatsByVoteWly");

        if (jdbcTemplate == null) {
            jdbcTemplate = NodeUtils.getNodeBindingService().getNodeBindingInfo(VOTE_BAS_INFO).getJdbcTemplate();
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

        Calendar cVoteStart = Calendar.getInstance() ;
        cVoteStart.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        Calendar cVoteEnd = Calendar.getInstance() ;
        String sVoteStart = DateFormatUtils.format(cVoteStart.getTime(), "yyyyMMdd");
        String sVoteEnd = DateFormatUtils.format(cVoteEnd.getTime(), "yyyyMMdd");
//        String sVoteStart   = "20171023";
//        String sVoteEnd     = "20171024";

        // 국가별 카운트 저장
        Map<CntryVoteByVoteVO, CntryVoteByVoteVO> hmTolCountPerCntrycd = new HashMap<CntryVoteByVoteVO, CntryVoteByVoteVO>();

        // 전체 카운트 ... 참고
        BigDecimal totalVoteNum = new BigDecimal(0);

        for (Node voteBasInfo : voteBasInfoList) {

            logger.info("schedule task - {} - {} ~ {} query", voteBasInfo.getId(), "[" + sVoteStart + "]", "[" +sVoteEnd + "]");
            List<Map<String,Object>> cntryVoteResultList = selectItemStatsByCntryList(sVoteStart, sVoteEnd, voteBasInfo.getId());
            logger.info("schedule task - {} - {} - {}", voteBasInfo.getId(), voteBasInfo.getStringValue("voteNm"), cntryVoteResultList.size());

            // 날짜별 voteSeq별 voteItem별 artist별 sex 별 count ====> artist별 sex 별 count 로 정리...
            for (Map voteItemVoteCntByItemSeqSex : cntryVoteResultList) {
                BigDecimal voteNum = new BigDecimal(voteItemVoteCntByItemSeqSex.get("voteNum").toString());
                totalVoteNum = totalVoteNum.add(voteNum);
                Object oCntryCd = voteItemVoteCntByItemSeqSex.get("cntryCd");
                if(oCntryCd == null) ;
                else {
                    String cntryCd = (String)oCntryCd;
                    if( cntryCd.toLowerCase().equals("thailand")) cntryCd = cntryCd.toUpperCase();
                    Long voteSeq = (Long)voteItemVoteCntByItemSeqSex.get("voteSeq");
                    logger.info("[voteSeq] " + voteSeq + " [cntryCd] " + cntryCd + " [voteNum] " + voteNum);
                    CntryVoteByVoteVO cntryVoteByVoteVO = new CntryVoteByVoteVO();
                    cntryVoteByVoteVO.setCntryCd(cntryCd);
                    cntryVoteByVoteVO.setVoteSeq(voteSeq);
                    cntryVoteByVoteVO.setVoteNum(voteNum);

                    if( hmTolCountPerCntrycd.get(cntryVoteByVoteVO) == null ) hmTolCountPerCntrycd.put(cntryVoteByVoteVO, cntryVoteByVoteVO);
                    else {
                        CntryVoteByVoteVO cntryVoteByVoteVO2 = hmTolCountPerCntrycd.get(cntryVoteByVoteVO);
                        cntryVoteByVoteVO2.setVoteNum(cntryVoteByVoteVO2.getVoteNum().add(voteNum));
                        hmTolCountPerCntrycd.put(cntryVoteByVoteVO2, cntryVoteByVoteVO2);
                    }
                }
            }
        }

        logger.info("deleteCntryVoteStatsByVote ");
        deleteCntryVoteStatsByVote(sVoteStart);

        List<CntryVoteByVoteVO> CntryVoteByVoteVOList = new ArrayList<CntryVoteByVoteVO>(hmTolCountPerCntrycd.values());

        // 정렬
        Collections.sort(CntryVoteByVoteVOList, new Comparator<CntryVoteByVoteVO>() {
            @Override
            public int compare(CntryVoteByVoteVO lhs, CntryVoteByVoteVO rhs) {
                return rhs.getVoteNum().compareTo(lhs.getVoteNum());
            }
        });

        logger.info("Total Cnt: " + totalVoteNum.intValue());

        // VoteRate처리 및 insert
        BigDecimal prevVoteNum = null;
        BigDecimal prevRankNum = null;
        for(int i = 0; i < CntryVoteByVoteVOList.size(); i++ ) {
            CntryVoteByVoteVO cntryVoteByVoteVO = CntryVoteByVoteVOList.get(i);
            if(prevVoteNum != null && cntryVoteByVoteVO.getVoteNum().compareTo(prevVoteNum) == 0) cntryVoteByVoteVO.setRankNum(prevRankNum);
            else cntryVoteByVoteVO.setRankNum(new BigDecimal(i+1));
            cntryVoteByVoteVO.setVoteRate(Math.round((cntryVoteByVoteVO.getVoteNum().doubleValue()/totalVoteNum.doubleValue()) * 1000)/10.0);
            logger.info("... CntryVoteByVoteVO " + cntryVoteByVoteVO);
            insertCntryVoteStatsByVote(cntryVoteByVoteVO);
            prevVoteNum = cntryVoteByVoteVO.getVoteNum();
            prevRankNum = cntryVoteByVoteVO.getRankNum();
        }

        logger.info("complete schedule task - execCntryVoteStatsByVoteWly");
    }

    public void deleteCntryVoteStatsByVote(String sVoteStart) {
        String deleteQuery = "DELETE FROM cntryVoteStatsByVoteWly" ;
        int com = jdbcTemplate.update(deleteQuery);
        logger.info("deleteCntryVoteStatsByVote - {} ", com);
    }

    public void insertCntryVoteStatsByVote(CntryVoteByVoteVO cntryVoteByVoteVO) {
        String insertQuery = "INSERT INTO cntryVoteStatsByVoteWly " +
                " (voteSeq, cntryCd, rankNum, voteRate, voteNum, owner, created) " +
                " VALUES(?, ?, ?, ?, ?, ?, NOW())";

//        try {
            int com = jdbcTemplate.update(insertQuery
                    , cntryVoteByVoteVO.getVoteSeq()
                    , cntryVoteByVoteVO.getCntryCd()
                    , cntryVoteByVoteVO.getRankNum()
                    , cntryVoteByVoteVO.getVoteRate()
                    , cntryVoteByVoteVO.getVoteNum()
                    , "system");
            logger.info("insertCntryVoteStatsByVote - {} - {}", cntryVoteByVoteVO.getVoteNum(), com);
//        } catch ( Exception ex ) {
//            logger.error(ex.getMessage(), ex);
//        }
    }


    private List<Map<String,Object>> selectItemStatsByCntryList(String sVoteStart, String sVoteEnd, String voteSeq) {
        String selectQuery = ""
                + " SELECT                                                                                                         "
                + " 	T.voteSeq,                                                                                                  "
                + " 	T.cntryCd,                                                                                                  "
                + " 	count(*) AS voteNum                                                                                        "
                + " FROM                                                                                                           "
                + " 	(                                                                                                           "
                + " 		SELECT                                                                                                  "
                + " 			VII.voteSeq,                                                                                        "
                + " 			(select cntryCd from mbrInfo m where m.snsTypeCd = v.snsTypeCd and m.snsKey = v.snsKey) as cntryCd "
                + " 		FROM                                                                                                   "
                + " 		(                                                                                                      "
                + " 			 SELECT                                                                                            "
                + " 			 	seq                                                                                            "
                + " 			 	, voteItemSeq                                                                                  "
                + " 			 	, SUBSTRING_INDEX(mbrId, '>', 1) as snsTypeCd                                                  "
                + " 			 	, SUBSTRING_INDEX(mbrId, '>', -1) as snsKey                                                    "
                + " 			 FROM " + voteSeq + "_voteItemHstByMbr                                                                      "
                + " 			 WHERE 1 = 1                                                                                       "
                + " 				AND voteDate BETWEEN ? AND ?                                                 "
                + " 		) v, voteItemInfo VII                                                                                  "
                + " 		WHERE                                                                                                  "
                + " 			1 = 1                                                                                              "
                + " 			and v.voteItemSeq = VII.voteItemSeq                                                                "
                + " 			and VII.voteSeq = ?                                                                           "
                + " 	) T                                                                                                        "
                + " GROUP BY                                                                                                       "
                + " 	voteSeq,                                                                                                   "
                + " 	cntryCd                                                                                                    "
                ;

        return jdbcTemplate_replica.queryForList(selectQuery, sVoteStart, sVoteEnd, voteSeq);
    }

}
