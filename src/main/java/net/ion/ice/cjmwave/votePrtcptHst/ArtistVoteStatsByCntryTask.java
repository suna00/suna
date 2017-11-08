package net.ion.ice.cjmwave.votePrtcptHst;

import net.ion.ice.cjmwave.votePrtcptHst.vo.ArtistCntryVO;
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

@Service("artistVoteStatsByCntryTask")
public class ArtistVoteStatsByCntryTask {

    private static Logger logger = LoggerFactory.getLogger(ArtistVoteStatsByCntryTask.class);

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
    public void execArtistVoteStatsByCntry(String statDate) {

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
        String sVoteStart = DateFormatUtils.format(cVoteStart.getTime(), "yyyyMMdd");
        String sVoteEnd = sVoteStart;     // 현재날짜
        if( statDate != null ) {
            sVoteStart = statDate;
            sVoteEnd = statDate;
        }

//        String sVoteStart   = "20171023";
//        String sVoteEnd     = "20171023";

        // 셩별로 나눠어진 artist sex voteNum 맵 을 관리
        Map<String, Map<ArtistCntryVO,ArtistCntryVO>> allArtistMapByCntry = new HashMap<String, Map<ArtistCntryVO,ArtistCntryVO>>();

        // 성별 카운트 저장
        Map<String, BigDecimal> hmTolCountPerCntry = new HashMap<String, BigDecimal>();

        // 전체 카운트 ... 참고
        BigDecimal totalVoteNum = new BigDecimal(0);

        for (Node voteBasInfo : voteBasInfoList) {

            logger.info("schedule task - {} - {} ~ {} query", voteBasInfo.getId(), "[" + sVoteStart + "]", "[" +sVoteEnd + "]");
            List<Map<String,Object>> artistCntryCntResultList = selectItemStatsByCntryList(sVoteStart, sVoteEnd, voteBasInfo.getId());
            logger.info("schedule task - {} - {} - {}", voteBasInfo.getId(), voteBasInfo.getStringValue("voteNm"), artistCntryCntResultList.size());

            // 날짜별 voteSeq별 voteItem별 artist별 sex 별 count ====> artist별 sex 별 count 로 정리...
            for (Map artistCntryCntResultMap : artistCntryCntResultList) {
                BigDecimal voteNum = new BigDecimal(artistCntryCntResultMap.get("voteNum").toString());
//                Integer voteNum = artistCntryCntResultMap.get("voteNum")==null ? 0 : Integer.parseInt(artistCntryCntResultMap.get("voteNum").toString());
                totalVoteNum = totalVoteNum.add(voteNum);
                Object oCntryCd = artistCntryCntResultMap.get("cntryCd");
                if(oCntryCd == null) ;
                else {
                    String cntryCd = (String)oCntryCd;
                    if( cntryCd.toLowerCase().equals("thailand")) cntryCd = "Thailand";
                    logger.info("[cntryCd] " + cntryCd + " [artistId] " + artistCntryCntResultMap.get("artistId") + " [voteNum] " + voteNum);

                    BigDecimal bcCountPerCntry = hmTolCountPerCntry.get(cntryCd);
                    if( bcCountPerCntry == null ) bcCountPerCntry = new BigDecimal(0);
                    bcCountPerCntry = bcCountPerCntry.add(voteNum);
                    hmTolCountPerCntry.put(cntryCd, bcCountPerCntry);

                    // 해당성별의 HashMap이 있는지 파악
                    // 해당성별의 artist 별 voteNum
                    Map<ArtistCntryVO,ArtistCntryVO> artistCntryVOMap = allArtistMapByCntry.get(artistCntryCntResultMap.get("cntryCd"));
                    if( artistCntryVOMap == null ) artistCntryVOMap = new HashMap<ArtistCntryVO,ArtistCntryVO>();

                    ArtistCntryVO artistCntryVO = new ArtistCntryVO();
                    artistCntryVO.setArtistId((String)artistCntryCntResultMap.get("artistId"));
                    artistCntryVO.setCntryCd(cntryCd);
                    artistCntryVO.setVoteStart(sVoteStart);
                    artistCntryVO.setVoteEnd(sVoteEnd);
                    artistCntryVO.setVoteNum(voteNum);

                    if( artistCntryVOMap.get(artistCntryVO) == null ) artistCntryVOMap.put(artistCntryVO, artistCntryVO);
                    else {
                        ArtistCntryVO ArtistCntryVO2 = artistCntryVOMap.get(artistCntryVO);
                        ArtistCntryVO2.setVoteNum(ArtistCntryVO2.getVoteNum().add(voteNum));
                        artistCntryVOMap.put(ArtistCntryVO2, ArtistCntryVO2);
                    }
                    allArtistMapByCntry.put(cntryCd, artistCntryVOMap);
                }
            }
        }
        Iterator iter1 = hmTolCountPerCntry.keySet().iterator();
        while( iter1.hasNext() ) {
            String cntryCd = (String)iter1.next();
            logger.info("[cntryCd " + cntryCd + "] " + hmTolCountPerCntry.get(cntryCd).intValue());
        }

        logger.info("deleteArtistVoteStatsByCntry ");
        deleteArtistVoteStatsByCntry(sVoteStart);

        Iterator iter = allArtistMapByCntry.keySet().iterator();       // 국가별...............
        while(iter.hasNext()) {
            String countryCd = (String)iter.next();
            BigDecimal currentVoteNumCntBySex = hmTolCountPerCntry.get(countryCd);

            Map<ArtistCntryVO,ArtistCntryVO> ArtistCntryVOMap = allArtistMapByCntry.get(countryCd);
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
        String deleteQuery = "DELETE FROM artistVoteStatsByCntry WHERE perdStDate = ? " ;
        int com = jdbcTemplate.update(deleteQuery, sVoteStart);
        logger.info("deleteArtistVoteStatsByCntry - {} - {}", sVoteStart, com);
    }

    public void insertArtistVoteStatsByCntry(ArtistCntryVO artistCntryVO) {
        String insertQuery = "INSERT INTO artistVoteStatsByCntry "
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


    private List<Map<String,Object>> selectItemStatsByCntryList(String sVoteStart, String sVoteEnd, String voteSeq) {
        String selectQuery = ""
                + " SELECT                                                                                                                          "
                + " 	T.voteDate,                                                                                                                  "
                + " 	T.cntryCd,                                                                                                                     "
                + " 	substring( T.contsMetaId, 9 ) artistId,                                                                                      "
                + " 	count(*) AS voteNum                                                                                                          "
                + " FROM                                                                                                                            "
                + " 	(                                                                                                                            "
                + " 		SELECT                                                                                                                   "
                + " 			v.voteDate,                                                                                                          "
                + " 			VII.contsMetaId,                                                                                                     "
                + " 			(select cntryCd from mbrInfo m where m.snsTypeCd = v.snsTypeCd and m.snsKey = v.snsKey) as cntryCd                      "
                + " 		FROM                                                                                                                     "
                + " 		(                                                                                                                        "
                + " 			 SELECT                                                                                                              "
                + " 			 	seq                                                                                                              "
                + " 			 	, voteDate                                                                                                       "
                + " 			 	, voteItemSeq                                                                                                    "
                + " 			 	, mbrId                                                                                                          "
                + " 			 	, SUBSTRING_INDEX(mbrId, '>', 1) as snsTypeCd                                                                    "
                + " 			 	, SUBSTRING_INDEX(mbrId, '>', -1) as snsKey                                                                      "
                + " 			 	, created                                                                                                        "
                + " 			 FROM " + voteSeq + "_voteItemHstByMbr                                                                                        "
                + " 			 WHERE 1 = 1                                                                                                         "
                + " 				AND voteDate = ?                                                                    "
                + " 		) v, voteItemInfo VII                                                                                                     "
                + " 		WHERE                                                                                                                     "
                + " 			1 = 1                                                                                                                 "
                + " 			and v.voteItemSeq = VII.voteItemSeq                                                                                   "
                + " 			and VII.voteSeq = ?                                                                                              "
                + " 	) T                                                                                                                           "
                + " GROUP BY                                                                                                                         "
                + " 	voteDate,                                                                                                                     "
                + " 	cntryCd,                                                                                                                         "
                + " 	contsMetaId                                                                                                                  "
                ;
        return jdbcTemplate_replica.queryForList(selectQuery, sVoteEnd, voteSeq);
    }

}
