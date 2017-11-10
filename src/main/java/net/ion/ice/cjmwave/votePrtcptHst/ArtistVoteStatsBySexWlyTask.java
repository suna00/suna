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

import net.ion.ice.cjmwave.votePrtcptHst.vo.ArtistSexVO;

import java.math.BigDecimal;
import java.util.*;

@Service("artistVoteStatsBySexWlyTask")
public class ArtistVoteStatsBySexWlyTask {

    private static Logger logger = LoggerFactory.getLogger(ArtistVoteStatsBySexWlyTask.class);

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
    public void execArtistVoteStatsBySex(String sDate, String eDate) {

        logger.info("start schedule task - execArtistVoteStatsBySex");

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
        if( sDate != null ) {sVoteStart = sDate;sVoteEnd = eDate;}

        // 셩별로 나눠어진 artist sex voteNum 맵 을 관리
        Map<String, Map<ArtistSexVO,ArtistSexVO>> allArtistMapBySex = new HashMap<String, Map<ArtistSexVO,ArtistSexVO>>();
        // 성별 카운트 저장
        Map<String, BigDecimal> hmTolCountPerSexcd = new HashMap<String, BigDecimal>();
        // 전체 카운트 ... 참고
        BigDecimal totalVoteNum = new BigDecimal(0);

        logger.info("schedule task - {} ~ {} query", "[" + sVoteStart + "]", "[" +sVoteEnd + "]");
        List<Map<String,Object>> artistSexResultList = selectItemStatsBySexList(sVoteStart, sVoteEnd);
        logger.info("schedule task - {}", artistSexResultList.size());


        for (Map artistSexCntResultMap : artistSexResultList) {
            BigDecimal voteNum = new BigDecimal(artistSexCntResultMap.get("voteNum").toString());
            Object oSexCd = artistSexCntResultMap.get("sexCd");
            String sexCd = (String)oSexCd;

            logger.info("[sexCd] " + sexCd + " [artistId] " + artistSexCntResultMap.get("artistId") + " [voteNum] " + voteNum);

            BigDecimal bcCountPerSex = hmTolCountPerSexcd.get(sexCd);
            if( bcCountPerSex == null ) bcCountPerSex = new BigDecimal(0);
            bcCountPerSex = bcCountPerSex.add(voteNum);
            hmTolCountPerSexcd.put(sexCd, bcCountPerSex);

            // 해당성별의 HashMap이 있는지 파악
            // 해당성별의 artist 별 voteNum
            Map<ArtistSexVO,ArtistSexVO> artistSexVOMap = allArtistMapBySex.get(artistSexCntResultMap.get("sexCd"));
            if( artistSexVOMap == null ) artistSexVOMap = new HashMap<ArtistSexVO,ArtistSexVO>();

            ArtistSexVO artistSexVO = new ArtistSexVO();
            artistSexVO.setArtistId((String)artistSexCntResultMap.get("artistId"));
            artistSexVO.setSexCd((String)artistSexCntResultMap.get("sexCd"));
            artistSexVO.setVoteStart(sVoteStart);
            artistSexVO.setVoteEnd(sVoteEnd);
            artistSexVO.setVoteNum(voteNum);
            artistSexVOMap.put(artistSexVO, artistSexVO);
            allArtistMapBySex.put(sexCd, artistSexVOMap);
        }

        Iterator iter1 = hmTolCountPerSexcd.keySet().iterator();
        while( iter1.hasNext() ) {
            String sexCd = (String)iter1.next();
            logger.info("[SexCD " + sexCd + "] " + hmTolCountPerSexcd.get(sexCd).intValue());
        }

        logger.info("deleteArtistVoteStatsBySex ");
        deleteArtistVoteStatsBySex(sVoteStart);

        Iterator iter = allArtistMapBySex.keySet().iterator();       // 성별 1, 2, 3
        while(iter.hasNext()) {
            String sexCd = (String)iter.next();
            BigDecimal currentVoteNumCntBySex = hmTolCountPerSexcd.get(sexCd);

            Map<ArtistSexVO,ArtistSexVO> artistSexVOMap = allArtistMapBySex.get(sexCd);
            List<ArtistSexVO> artistSexVOList = new ArrayList<ArtistSexVO>(artistSexVOMap.values());

            // 정렬
            Collections.sort(artistSexVOList, new Comparator<ArtistSexVO>() {
                @Override
                public int compare(ArtistSexVO lhs, ArtistSexVO rhs) {
                    return rhs.getVoteNum().compareTo(lhs.getVoteNum());
                }
            });

            // VoteRate처리 및 insert
            BigDecimal prevVoteNum = null;
            BigDecimal prevRankNum = null;
            for(int i = 0 ; i < artistSexVOList.size(); i++ ) {
                ArtistSexVO artistSexVO = artistSexVOList.get(i);
                if(prevVoteNum != null && artistSexVO.getVoteNum().compareTo(prevVoteNum) == 0) artistSexVO.setRankNum(prevRankNum);
                else artistSexVO.setRankNum(new BigDecimal(i+1));
                artistSexVO.setVoteRate(Math.round((artistSexVO.getVoteNum().doubleValue()/currentVoteNumCntBySex.doubleValue()) * 1000)/10.0);
                logger.info("... ArtistSexVO " + artistSexVO);
                insertArtistVoteStatsBySex(artistSexVO);
                prevVoteNum = artistSexVO.getVoteNum();
                prevRankNum = artistSexVO.getRankNum();
            }
        }

        logger.info("complete schedule task - execArtistVoteStatsBySex");
    }

    public void deleteArtistVoteStatsBySex(String sVoteStart) {
        String deleteQuery = "DELETE FROM artistVoteStatsBySexWly WHERE perdStDate = ? " ;
        int com = jdbcTemplate.update(deleteQuery, sVoteStart);
        logger.info("deleteArtistVoteStatsBySex - {} - {}", sVoteStart, com);
    }

    public void insertArtistVoteStatsBySex(ArtistSexVO artistSexVO) {
        String insertQuery = "INSERT INTO artistVoteStatsBySexWly "
                              +  " (perdStDate, perdFnsDate, artistId, sexCd, rankNum, voteRate, voteNum, owner, created) "
                              +  " VALUES(?, ?, ?, ?, ?, ?, ?, ?, NOW())";

        int com = jdbcTemplate.update(insertQuery,
                artistSexVO.getVoteStart()
                , artistSexVO.getVoteEnd()
                , artistSexVO.getArtistId()
                , artistSexVO.getSexCd()
                , artistSexVO.getRankNum()
                , artistSexVO.getVoteRate()
                , artistSexVO.getVoteNum()
                , "system");
        logger.info("insertArtistVoteStatsBySex - {} - {} - {}", artistSexVO.getArtistId(), artistSexVO.getVoteNum(), com);
    }


    private List<Map<String,Object>> selectItemStatsBySexList(String sVoteStart, String sVoteEnd) {
        String selectQuery = ""
                + " SELECT                                               "
                + " 	perdStDate,                                       "
                + " 	perdFnsDate,                                      "
                + " 	sexCd,                                            "
                + " 	artistId,                                         "
                + "  	sum(voteNum) voteNum,                             "
                + " 	owner                                             "
                + " FROM                                                 "
                + " 	artistVoteStatsBySex                              "
                + " WHERE 1 = 1                                          "
                + " 	and perdStDate BETWEEN ? and ?                  "
                + " group by sexCd, artistId, voteNum	                  "
                ;

        return jdbcTemplate_replica.queryForList(selectQuery, sVoteStart, sVoteEnd);
//        return jdbcTemplate.queryForList(selectQuery, sVoteStart, sVoteEnd);
    }
}
