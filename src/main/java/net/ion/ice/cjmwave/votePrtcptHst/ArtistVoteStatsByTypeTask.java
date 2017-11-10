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

@Service("artistVoteStatsByTypeTask")
public class ArtistVoteStatsByTypeTask {

    private static Logger logger = LoggerFactory.getLogger(ArtistVoteStatsByTypeTask.class);

    public static final String VOTE_BAS_INFO = "voteBasInfo";

    @Autowired
    NodeService nodeService;

    @Autowired
    private DBService dbService ;

    private JdbcTemplate jdbcTemplate;
    private JdbcTemplate jdbcTemplate_replica;

    /**
     * 날짜별 처리를... 어떻게 할지는 고민해봐야겠군.
     * 성별.
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
        cVoteStart.add(Calendar.DATE, -1);
        String sVoteStart = DateFormatUtils.format(cVoteStart.getTime(), "yyyy-MM-dd");
        String sVoteEnd = sVoteStart;     // 현재날짜
//        String sVoteStart   = "20171023";
//        String sVoteEnd     = "20171023";

        // 타입 맵 을 관리
        Map<String, Map<ArtistTypeVO,ArtistTypeVO>> allArtistMapByType = new HashMap<String, Map<ArtistTypeVO,ArtistTypeVO>>();

        // 타입 카운트 저장
        Map<String, BigDecimal> hmTolCountPerType = new HashMap<String, BigDecimal>();

        // 전체 카운트 ... 참고
        BigDecimal totalVoteNum = new BigDecimal(0);

        for (Node voteBasInfo : voteBasInfoList) {

            logger.info("schedule task - {} - {} ~ {} query", voteBasInfo.getId(), "[" + sVoteStart + "]", "[" +sVoteEnd + "]");
            List<Map<String,Object>> artistCntryCntResultList = selectItemStatsByCntryList(sVoteStart, sVoteEnd, voteBasInfo.getId());
            logger.info("schedule task - {} - {} - {}", voteBasInfo.getId(), voteBasInfo.getStringValue("voteNm"), artistCntryCntResultList.size());

            // 날짜별 voteSeq별 voteItem별 artist별 sex 별 count ====> artist별 sex 별 count 로 정리...
            for (Map artistCntryCntResultMap : artistCntryCntResultList) {
                BigDecimal voteNum = new BigDecimal(artistCntryCntResultMap.get("voteNum").toString());
                totalVoteNum = totalVoteNum.add(voteNum);
                String typeCd = ""; // node 에서 가져와야하는 값...

                BigDecimal bcCountPerType = hmTolCountPerType.get(typeCd);
                if( bcCountPerType == null ) bcCountPerType = new BigDecimal(0);
                bcCountPerType = bcCountPerType.add(voteNum);
                hmTolCountPerType.put(typeCd, bcCountPerType);

                // 해당성별의 HashMap이 있는지 파악
                // 해당성별의 artist 별 voteNum
                Map<ArtistTypeVO,ArtistTypeVO> artistCntryVOMap = allArtistMapByType.get(artistCntryCntResultMap.get("typeCd"));
                if( artistCntryVOMap == null ) artistCntryVOMap = new HashMap<ArtistTypeVO,ArtistTypeVO>();

                ArtistTypeVO artistCntryVO = new ArtistTypeVO();
                artistCntryVO.setArtistId((String)artistCntryCntResultMap.get("artistId"));
                artistCntryVO.setTypeCd(typeCd);
                artistCntryVO.setVoteStart(sVoteStart);
                artistCntryVO.setVoteEnd(sVoteEnd);
                artistCntryVO.setVoteNum(voteNum);

                if( artistCntryVOMap.get(artistCntryVO) == null ) artistCntryVOMap.put(artistCntryVO, artistCntryVO);
                else {
                    ArtistTypeVO ArtistTypeVO2 = artistCntryVOMap.get(artistCntryVO);
                    ArtistTypeVO2.setVoteNum(ArtistTypeVO2.getVoteNum().add(voteNum));
                    artistCntryVOMap.put(ArtistTypeVO2, ArtistTypeVO2);
                }
                allArtistMapByType.put(typeCd, artistCntryVOMap);
            }
        }
        Iterator iter1 = hmTolCountPerType.keySet().iterator();
        while( iter1.hasNext() ) {
            String typeCd = (String)iter1.next();
            logger.info("[typeCd " + typeCd + "] " + hmTolCountPerType.get(typeCd).intValue());
        }

        logger.info("deleteArtistVoteStatsByCntry ");
        deleteArtistVoteStatsByType(sVoteStart);

        Iterator iter = allArtistMapByType.keySet().iterator();       // 국가별...............
        while(iter.hasNext()) {
            String countryCd = (String)iter.next();
            BigDecimal currentVoteNumCntBySex = hmTolCountPerType.get(countryCd);

            Map<ArtistTypeVO,ArtistTypeVO> ArtistTypeVOMap = allArtistMapByType.get(countryCd);
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
        String deleteQuery = "DELETE FROM artistVoteStatsByType WHERE perdStDate = ? " ;
        int com = jdbcTemplate.update(deleteQuery, sVoteStart);
        logger.info("deleteArtistVoteStatsByCntry - {} - {}", sVoteStart, com);
    }

    public void insertArtistVoteStatsByType(ArtistTypeVO artistTypeVO) {
        String insertQuery = "INSERT INTO artistVoteStatsByType "
                +  " (perdStDate, perdFnsDate, artistId, typeCd, rankNum, voteRate, voteNum, owner, created) "
                +  " VALUES(?, ?, ?, ?, ?, ?, ?, ?, NOW())";

        int com = jdbcTemplate.update(insertQuery,
                artistTypeVO.getVoteStart()
                , artistTypeVO.getVoteEnd()
                , artistTypeVO.getArtistId()
                , artistTypeVO.getTypeCd()
                , artistTypeVO.getRankNum()
                , artistTypeVO.getVoteRate()
                , artistTypeVO.getVoteNum()
                , "system");
        logger.info("insertArtistVoteStatsByType - {} - {} - {}", artistTypeVO.getArtistId(), artistTypeVO.getVoteNum(), com);
    }


    private List<Map<String,Object>> selectItemStatsByCntryList(String sVoteStart, String sVoteEnd, String voteSeq) {
        String selectQuery = ""
                + " SELECT                                          "
                + " 	T.voteDate,                                 "
                + " 	substring( T.contsMetaId, 9 ) artistId,     "
                + " 	count(*) AS voteNum                         "
                + " FROM                                            "
                + " 	(                                           "
                + " 		SELECT                                  "
                + " 			v.voteDate,                         "
                + " 			VII.contsMetaId                     "
                + " 		FROM                                    "
                + " 		(                                       "
                + " 			 SELECT                             "
                + " 			 	seq                             "
                + " 			 	, voteDate                      "
                + " 			 	, voteItemSeq                   "
                + " 			 FROM " + voteSeq+"_voteItemHstByMbr       "
                + " 			 WHERE 1 = 1                        "
                + " 				AND voteDate = ?       "
                + " 		) v, voteItemInfo VII                   "
                + " 		WHERE                                   "
                + " 			1 = 1                               "
                + " 			and v.voteItemSeq = VII.voteItemSeq "
                + " 			and VII.voteSeq = ?            "
                + " 	) T                                         "
                + " GROUP BY                                        "
                + " 	voteDate,                                   "
                + " 	contsMetaId                                 "

                ;
        return jdbcTemplate_replica.queryForList(selectQuery, sVoteEnd, voteSeq);
    }

}
