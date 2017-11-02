package net.ion.ice.cjmwave.votePrtcptHst;

import net.ion.ice.cjmwave.votePrtcptHst.vo.CntryVoteVO;
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

@Service("cntryVoteStatsWlyTask")
public class CntryVoteStatsWlyTask {

    private static Logger logger = LoggerFactory.getLogger(CntryVoteStatsWlyTask.class);

    public static final String VOTE_BAS_INFO = "voteBasInfo";

    @Autowired
    NodeService nodeService;

    JdbcTemplate jdbcTemplate;

    /**
     * 날짜별 처리를... 어떻게 할지는 고민해봐야겠군.
     * 성별.
     */
    public void execCntryVoteStatsWly() {

        logger.info("start schedule task - execCntryVoteStatsWly");

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

        Calendar cVoteStart = Calendar.getInstance() ;
        cVoteStart.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        Calendar cVoteEnd = Calendar.getInstance() ;
        String sVoteStart = DateFormatUtils.format(cVoteStart.getTime(), "yyyy-MM-dd");
        String sVoteEnd = DateFormatUtils.format(cVoteEnd.getTime(), "yyyy-MM-dd");
//        String sVoteStart   = "20171023";
//        String sVoteEnd     = "20171024";

        // 국가별 카운트 저장
        Map<CntryVoteVO, CntryVoteVO> hmTolCountPerCntrycd = new HashMap<CntryVoteVO, CntryVoteVO>();

        // 전체 카운트 ... 참고
        BigDecimal totalVoteNum = new BigDecimal(0);

        logger.info("schedule task - {} ~ {} query", "[" + sVoteStart + "]", "[" +sVoteEnd + "]");
        List<Map<String,Object>> cntryCntList = selectItemStatsByCntryList(sVoteStart, sVoteEnd);
        logger.info("schedule task - {}", cntryCntList.size());

        // 날짜별 voteSeq별 voteItem별 artist별 sex 별 count ====> artist별 sex 별 count 로 정리...
        for (Map cntryCntMap : cntryCntList) {
            BigDecimal voteNum = new BigDecimal(cntryCntMap.get("voteNum").toString());
            totalVoteNum = totalVoteNum.add(voteNum);
            Object oCntryCd = cntryCntMap.get("cntryCd");
            if(oCntryCd == null) ;
            else {
                String cntryCd = (String)oCntryCd;
                if( cntryCd.toLowerCase().equals("thailand")) cntryCd = "Thailand";
                logger.info("[cntryCd] " + cntryCd + " [voteNum] " + voteNum);
                CntryVoteVO cntryVoteVO = new CntryVoteVO();
                cntryVoteVO.setCntryCd(cntryCd);
                cntryVoteVO.setVoteNum(voteNum);
                cntryVoteVO.setVoteStart(sVoteStart);
                cntryVoteVO.setVoteEnd(sVoteEnd);

                if( hmTolCountPerCntrycd.get(cntryVoteVO) == null ) hmTolCountPerCntrycd.put(cntryVoteVO, cntryVoteVO);
                else {
                    CntryVoteVO cntryVoteVO2 = hmTolCountPerCntrycd.get(cntryVoteVO);
                    cntryVoteVO2.setVoteNum(cntryVoteVO2.getVoteNum().add(voteNum));
                    hmTolCountPerCntrycd.put(cntryVoteVO2, cntryVoteVO2);
                }
            }
        }

        logger.info("deleteCntryVoteStats ");
        deleteCntryVoteStats(sVoteStart);

        List<CntryVoteVO> cntryVoteVOList = new ArrayList<CntryVoteVO>(hmTolCountPerCntrycd.values());

        // 정렬
        Collections.sort(cntryVoteVOList, new Comparator<CntryVoteVO>() {
            @Override
            public int compare(CntryVoteVO lhs, CntryVoteVO rhs) {
                return rhs.getVoteNum().compareTo(lhs.getVoteNum());
            }
        });

        logger.info("Total Cnt: " + totalVoteNum.intValue());

        // VoteRate처리 및 insert
        BigDecimal prevVoteNum = null;
        BigDecimal prevRankNum = null;
        for(int i = 0; i < cntryVoteVOList.size(); i++ ) {
            CntryVoteVO cntryVoteVO = cntryVoteVOList.get(i);
            if(prevVoteNum != null && cntryVoteVO.getVoteNum().compareTo(prevVoteNum) == 0) cntryVoteVO.setRankNum(prevRankNum);
            else cntryVoteVO.setRankNum(new BigDecimal(i+1));
            cntryVoteVO.setVoteRate(Math.round((cntryVoteVO.getVoteNum().doubleValue()/totalVoteNum.doubleValue()) * 1000)/10.0);
            logger.info("... cntryVoteVO " + cntryVoteVO);
            insertCntryVoteStats(cntryVoteVO);
            prevVoteNum = cntryVoteVO.getVoteNum();
            prevRankNum = cntryVoteVO.getRankNum();
        }
        logger.info("complete schedule task - execCntryVoteStatsWly");
    }

    public void deleteCntryVoteStats(String sVoteStart) {
        String deleteQuery = "DELETE FROM cntryVoteStatsWly WHERE perdStDate = ? " ;
        int com = jdbcTemplate.update(deleteQuery, sVoteStart);
        logger.info("deleteCntryVoteStats - {} - {}", sVoteStart, com);
    }

    public void insertCntryVoteStats(CntryVoteVO cntryVoteVO) {
        String insertQuery = "INSERT INTO cntryVoteStatsWly " +
                " (perdStDate, perdFnsDate, cntryCd, rankNum, voteRate, voteNum, owner, created) " +
                " VALUES(?, ?, ?, ?, ?, ?, ?, NOW())";
//        try {
            int com = jdbcTemplate.update(insertQuery,
                    cntryVoteVO.getVoteStart()
                    , cntryVoteVO.getVoteEnd()
                    , cntryVoteVO.getCntryCd()
                    , cntryVoteVO.getRankNum()
                    , cntryVoteVO.getVoteRate()
                    , cntryVoteVO.getVoteNum()
                    , "system");
            logger.info("insertCntryVoteStats - {} - {}", cntryVoteVO.getVoteNum(), com);
//        } catch ( Exception ex ) {
//            logger.error(ex.getMessage(), ex);
//        }
    }


    private List<Map<String,Object>> selectItemStatsByCntryList(String sVoteStart, String sVoteEnd) {
        String selectQuery = ""
                + " select                              "
                + " 	cntryCd, sum(voteNum) voteNum  "
                + " from cntryVoteStats                "
                + " where perdFnsDate BETWEEN ? and ?  "
                + " group by cntryCd                   "
                ;
        return jdbcTemplate.queryForList(selectQuery, sVoteStart, sVoteEnd);
    }

}
