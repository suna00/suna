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

import net.ion.ice.cjmwave.votePrtcptHst.vo.ArtistCntryVO;

import java.math.BigDecimal;
import java.util.*;

@Service("artistVoteStatsByCntryWlyTask")
public class ArtistVoteStatsByCntryWlyTask {

    private static Logger logger = LoggerFactory.getLogger(ArtistVoteStatsByCntryWlyTask.class);

    public static final String VOTE_BAS_INFO = "voteBasInfo";

    @Autowired
    NodeService nodeService;

    @Autowired
    private DBService dbService ;

    private JdbcTemplate jdbcTemplate;
    private JdbcTemplate jdbcTemplate_replica;

    /**
     * 국가별.
     */
    public void execArtistVoteStatsByCntry() {

        logger.info("start schedule task - execArtistVoteStatsByCntry");

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

        // 셩별로 나눠어진 artist sex voteNum 맵 을 관리
        Map<String, Map<ArtistCntryVO,ArtistCntryVO>> allArtistMapBySex = new HashMap<String, Map<ArtistCntryVO,ArtistCntryVO>>();

        // 성별 카운트 저장
        Map<String, BigDecimal> hmTolCountPerCntrycd = new HashMap<String, BigDecimal>();

        // 전체 카운트 ... 참고
        BigDecimal totalVoteNum = new BigDecimal(0);

        logger.info("schedule task - {} ~ {} query", "[" + sVoteStart + "]", "[" +sVoteEnd + "]");
        List<Map<String,Object>> voteItemVoteCntByItemSeqCntryList = selectItemStatsByCntryList(sVoteStart, sVoteEnd);
        logger.info("schedule task - {}", voteItemVoteCntByItemSeqCntryList.size());

        // 날짜별 voteSeq별 voteItem별 artist별 sex 별 count ====> artist별 sex 별 count 로 정리...
        for (Map voteItemVoteCntByItemSeqSex : voteItemVoteCntByItemSeqCntryList) {
            BigDecimal voteNum = new BigDecimal(voteItemVoteCntByItemSeqSex.get("voteNum").toString());

            String cntryCd = (String)voteItemVoteCntByItemSeqSex.get("cntryCd");
            if( cntryCd.toLowerCase().equals("thailand")) cntryCd = "Thailand";
            logger.info("[cntryCd] " + cntryCd + " [artistId] " + voteItemVoteCntByItemSeqSex.get("artistId") + " [voteNum] " + voteNum);

            BigDecimal bcCountPerSex = hmTolCountPerCntrycd.get(cntryCd);
            if( bcCountPerSex == null ) bcCountPerSex = new BigDecimal(0);
            bcCountPerSex = bcCountPerSex.add(voteNum);
            hmTolCountPerCntrycd.put(cntryCd, bcCountPerSex);

            // 해당성별의 HashMap이 있는지 파악
            // 해당성별의 artist 별 voteNum
            Map<ArtistCntryVO,ArtistCntryVO> artistCntryVOMap = allArtistMapBySex.get(voteItemVoteCntByItemSeqSex.get("cntryCd"));
            if( artistCntryVOMap == null ) artistCntryVOMap = new HashMap<ArtistCntryVO,ArtistCntryVO>();

            ArtistCntryVO artistCntryVO = new ArtistCntryVO();
            artistCntryVO.setArtistId((String)voteItemVoteCntByItemSeqSex.get("artistId"));
            artistCntryVO.setCntryCd((String)voteItemVoteCntByItemSeqSex.get("cntryCd"));
            artistCntryVO.setVoteStart(sVoteStart);
            artistCntryVO.setVoteEnd(sVoteEnd);
            artistCntryVO.setVoteNum(voteNum);
            artistCntryVOMap.put(artistCntryVO, artistCntryVO);
            allArtistMapBySex.put(cntryCd, artistCntryVOMap);
        }

        Iterator iter1 = hmTolCountPerCntrycd.keySet().iterator();
        while( iter1.hasNext() ) {
            String cntryCd = (String)iter1.next();
            logger.info("[cntryCd " + cntryCd + "] " + hmTolCountPerCntrycd.get(cntryCd).intValue());
        }

        logger.info("deleteArtistVoteStatsByCntry ");
        deleteArtistVoteStatsByCntry(sVoteStart);

        Iterator iter = allArtistMapBySex.keySet().iterator();       // 국가별...............
        while(iter.hasNext()) {
            String countryCd = (String)iter.next();
            BigDecimal currentVoteNumCntBySex = hmTolCountPerCntrycd.get(countryCd);

            Map<ArtistCntryVO,ArtistCntryVO> ArtistCntryVOMap = allArtistMapBySex.get(countryCd);
            List<ArtistCntryVO> ArtistCntryVOList = new ArrayList<ArtistCntryVO>(ArtistCntryVOMap.values());

            // 정렬
            Collections.sort(ArtistCntryVOList, new Comparator<ArtistCntryVO>() {
                @Override
                public int compare(ArtistCntryVO lhs, ArtistCntryVO rhs) {
                    return rhs.getVoteNum().compareTo(lhs.getVoteNum());
                }
            });

            // VoteRate처리 및 insert
            BigDecimal prevVoteNum = null;
            BigDecimal prevRankNum = null;
            for(int i = 0 ; i < ArtistCntryVOList.size(); i++ ) {
                ArtistCntryVO artistCntryVO = ArtistCntryVOList.get(i);
                if(prevVoteNum != null && artistCntryVO.getVoteNum().compareTo(prevVoteNum) == 0) artistCntryVO.setRankNum(prevRankNum);
                else artistCntryVO.setRankNum(new BigDecimal(i+1));
                artistCntryVO.setVoteRate(Math.round((artistCntryVO.getVoteNum().doubleValue()/currentVoteNumCntBySex.doubleValue()) * 1000)/10.0);
                logger.info("... ArtistCntryVO " + artistCntryVO);
                insertArtistVoteStatsByCntry(artistCntryVO);
                prevVoteNum = artistCntryVO.getVoteNum();
                prevRankNum = artistCntryVO.getRankNum();
            }
        }

        logger.info("complete schedule task - execArtistVoteStatsByCntry");
    }

    public void deleteArtistVoteStatsByCntry(String sVoteStart) {
        String deleteQuery = "DELETE FROM artistVoteStatsByCntryWly WHERE perdStDate = ? " ;
        int com = jdbcTemplate.update(deleteQuery, sVoteStart);
        logger.info("deleteArtistVoteStatsByCntry - {} - {}", sVoteStart, com);
    }

    public void insertArtistVoteStatsByCntry(ArtistCntryVO artistCntryVO) {
        String insertQuery = "INSERT INTO artistVoteStatsByCntryWly "
                +  " (perdStDate, perdFnsDate, artistId, cntryCd, rankNum, voteRate, voteNum, owner, created) "
                +  " VALUES(?, ?, ?, ?, ?, ?, ?, ?, NOW())";

//        try {
            int com = jdbcTemplate.update(insertQuery,
                    artistCntryVO.getVoteStart()
                    , artistCntryVO.getVoteEnd()
                    , artistCntryVO.getArtistId()
                    , artistCntryVO.getCntryCd()
                    , artistCntryVO.getRankNum()
                    , artistCntryVO.getVoteRate()
                    , artistCntryVO.getVoteNum()
                    , "system");
            logger.info("insertArtistVoteStatsByCntry - {} - {} - {}", artistCntryVO.getArtistId(), artistCntryVO.getVoteNum(), com);
//        } catch ( Exception ex ) {
//            logger.error(ex.getMessage(), ex);
//        }
    }


    private List<Map<String,Object>> selectItemStatsByCntryList(String sVoteStart, String sVoteEnd) {
        String selectQuery = ""
                + " SELECT                                                "
                + " 	perdStDate,                                       "
                + " 	perdFnsDate,                                      "
                + " 	cntryCd,                                            "
                + " 	artistId,                                         "
//                + "   rankNum,                                          "
//                + "  	voteRate,                                         "
                + "  	sum(voteNum) voteNum,                             "
                + " 	owner                                             "
//                + "  	created                                           "
                + " FROM                                                  "
                + " 	artistVoteStatsByCntry                              "
                + " WHERE 1 = 1                                           "
                + " 	and perdStDate BETWEEN ? and ?  "
                + " group by cntryCd, artistId, voteNum	                   "
                ;
        return jdbcTemplate_replica.queryForList(selectQuery, sVoteStart, sVoteEnd);
    }

}
