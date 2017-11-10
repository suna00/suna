package net.ion.ice.cjmwave.votePrtcptHst;

import net.ion.ice.cjmwave.votePrtcptHst.vo.ArtistTypeVO;
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

@Service("artistVoteStatsByTypeWlyTask")
public class ArtistVoteStatsByTypeWlyTask {

    private static Logger logger = LoggerFactory.getLogger(ArtistVoteStatsByTypeWlyTask.class);

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
        String sVoteStart = DateFormatUtils.format(cVoteStart.getTime(), "yyyy-MM-dd");
        String sVoteEnd = DateFormatUtils.format(cVoteEnd.getTime(), "yyyy-MM-dd");
//        String sVoteStart   = "20171023";
//        String sVoteEnd     = "20171024";

        // 셩별로 나눠어진 artist sex voteNum 맵 을 관리
        Map<String, Map<ArtistTypeVO,ArtistTypeVO>> allArtistMapBySex = new HashMap<String, Map<ArtistTypeVO,ArtistTypeVO>>();

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

            String typeCd = (String)voteItemVoteCntByItemSeqSex.get("typeCd");
            BigDecimal bcCountPerSex = hmTolCountPerCntrycd.get(typeCd);
            if( bcCountPerSex == null ) bcCountPerSex = new BigDecimal(0);
            bcCountPerSex = bcCountPerSex.add(voteNum);
            hmTolCountPerCntrycd.put(typeCd, bcCountPerSex);

            // 해당성별의 HashMap이 있는지 파악
            // 해당성별의 artist 별 voteNum
            Map<ArtistTypeVO,ArtistTypeVO> artistCntryVOMap = allArtistMapBySex.get(voteItemVoteCntByItemSeqSex.get("typeCd"));
            if( artistCntryVOMap == null ) artistCntryVOMap = new HashMap<ArtistTypeVO,ArtistTypeVO>();

            ArtistTypeVO artistCntryVO = new ArtistTypeVO();
            artistCntryVO.setArtistId((String)voteItemVoteCntByItemSeqSex.get("artistId"));
            artistCntryVO.setTypeCd((String)voteItemVoteCntByItemSeqSex.get("typeCd"));
            artistCntryVO.setVoteStart(sVoteStart);
            artistCntryVO.setVoteEnd(sVoteEnd);
            artistCntryVO.setVoteNum(voteNum);
            artistCntryVOMap.put(artistCntryVO, artistCntryVO);
            allArtistMapBySex.put(typeCd, artistCntryVOMap);
        }

        Iterator iter1 = hmTolCountPerCntrycd.keySet().iterator();
        while( iter1.hasNext() ) {
            String typeCd = (String)iter1.next();
            logger.info("[typeCd " + typeCd + "] " + hmTolCountPerCntrycd.get(typeCd).intValue());
        }

        logger.info("deleteArtistVoteStatsByType ");
        deleteArtistVoteStatsByType(sVoteStart);

        Iterator iter = allArtistMapBySex.keySet().iterator();       // 국가별...............
        while(iter.hasNext()) {
            String countryCd = (String)iter.next();
            BigDecimal currentVoteNumCntBySex = hmTolCountPerCntrycd.get(countryCd);

            Map<ArtistTypeVO,ArtistTypeVO> ArtistTypeVOMap = allArtistMapBySex.get(countryCd);
            List<ArtistTypeVO> ArtistTypeVOList = new ArrayList<ArtistTypeVO>(ArtistTypeVOMap.values());

            // 정렬
            Collections.sort(ArtistTypeVOList, new Comparator<ArtistTypeVO>() {
                @Override
                public int compare(ArtistTypeVO lhs, ArtistTypeVO rhs) {
                    return rhs.getVoteNum().compareTo(lhs.getVoteNum());
                }
            });

            // VoteRate처리 및 insert
            BigDecimal prevVoteNum = null;
            BigDecimal prevRankNum = null;
            for(int i = 0 ; i < ArtistTypeVOList.size(); i++ ) {
                ArtistTypeVO artistCntryVO = ArtistTypeVOList.get(i);
                if(prevVoteNum != null && artistCntryVO.getVoteNum().compareTo(prevVoteNum) == 0) artistCntryVO.setRankNum(prevRankNum);
                else artistCntryVO.setRankNum(new BigDecimal(i+1));
                artistCntryVO.setVoteRate(Math.round((artistCntryVO.getVoteNum().doubleValue()/currentVoteNumCntBySex.doubleValue()) * 1000)/10.0);
                logger.info("... ArtistTypeVO " + artistCntryVO);
                insertArtistVoteStatsByType(artistCntryVO);
                prevVoteNum = artistCntryVO.getVoteNum();
                prevRankNum = artistCntryVO.getRankNum();
            }
        }

        logger.info("complete schedule task - execArtistVoteStatsByCntry");
    }

    public void deleteArtistVoteStatsByType(String sVoteStart) {
        String deleteQuery = "DELETE FROM artistVoteStatsByTypeWly WHERE perdStDate = ? " ;
        int com = jdbcTemplate.update(deleteQuery, sVoteStart);
        logger.info("deleteArtistVoteStatsByType - {} - {}", sVoteStart, com);
    }

    public void insertArtistVoteStatsByType(ArtistTypeVO artistCntryVO) {
        String insertQuery = "INSERT INTO artistVoteStatsByTypeWly "
                +  " (perdStDate, perdFnsDate, artistId, typeCd, rankNum, voteRate, voteNum, owner, created) "
                +  " VALUES(?, ?, ?, ?, ?, ?, ?, ?, NOW())";

        int com = jdbcTemplate.update(insertQuery,
                artistCntryVO.getVoteStart()
                , artistCntryVO.getVoteEnd()
                , artistCntryVO.getArtistId()
                , artistCntryVO.getTypeCd()
                , artistCntryVO.getRankNum()
                , artistCntryVO.getVoteRate()
                , artistCntryVO.getVoteNum()
                , "system");
        logger.info("insertArtistVoteStatsByTypeCd - {} - {} - {}", artistCntryVO.getArtistId(), artistCntryVO.getVoteNum(), com);
    }


    private List<Map<String,Object>> selectItemStatsByCntryList(String sVoteStart, String sVoteEnd) {
        String selectQuery = ""
                + " SELECT                                                "
                + " 	perdStDate,                                       "
                + " 	perdFnsDate,                                      "
                + " 	typeCd,                                            "
                + " 	artistId,                                         "
//                + "   rankNum,                                          "
//                + "  	voteRate,                                         "
                + "  	sum(voteNum) voteNum,                             "
                + " 	owner                                             "
//                + "  	created                                           "
                + " FROM                                                  "
                + " 	artistVoteStatsByType                              "
                + " WHERE 1 = 1                                           "
                + " 	and perdStDate BETWEEN ? and ?  "
                + " group by typeCd, artistId, voteNum	                   "
                ;
        return jdbcTemplate_replica.queryForList(selectQuery, sVoteStart, sVoteEnd);
    }

}
