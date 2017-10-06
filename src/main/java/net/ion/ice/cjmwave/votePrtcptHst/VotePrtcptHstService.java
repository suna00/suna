package net.ion.ice.cjmwave.votePrtcptHst;

import net.ion.ice.IceRuntimeException;
import net.ion.ice.core.api.ApiException;
import net.ion.ice.core.context.ExecuteContext;
import net.ion.ice.core.data.bind.NodeBindingUtils;
import net.ion.ice.core.event.EventService;
import net.ion.ice.core.json.JsonUtils;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeService;
import net.ion.ice.core.node.NodeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.lang.management.LockInfo;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Collections.synchronizedList;

/**
 * Created by leehh on 2017. 9. 20.
 */
@Service("votePrtcptHstService")
public class VotePrtcptHstService {
    public static final String VOTE_BAS_INFO = "voteBasInfo";
    public static final String VOTE_SEQ = "voteSeq";

    private static Logger logger = LoggerFactory.getLogger(VotePrtcptHstService.class);
    private static String[] pidArray = {"sersVoteSeq", "voteSeq", "voteItemSeq", "prtcpMbrId", "connIpAdr"};

    @Autowired
    private NodeService nodeService;

    private JdbcTemplate jdbcTemplate ;

    private Map<String, String> voteHstSqlMap = new ConcurrentHashMap<>() ;
    private Map<String, String> voteItemHstSqlMap = new ConcurrentHashMap<>() ;
    private Map<String, String> votePvHstSqlMap = new ConcurrentHashMap<>() ;   // TODO - PV Counting
    private Map<String, String> voteHstSelectSqlMap = new ConcurrentHashMap<>() ;
    private Map<String, String> voteHstByIpSelectSqlMap = new ConcurrentHashMap<>() ;



    private Map<String, Map<String, Integer>> mbrVoteCount = new ConcurrentHashMap<>() ;

    @PostConstruct
    public void init() {
        // TODO - 캐시 데이터가 없을 경우 오류 발생
        jdbcTemplate = NodeUtils.getNodeBindingService().getNodeBindingInfo(VOTE_BAS_INFO).getJdbcTemplate();
    }

    /**
     * 2017.09.27 latinus.hong
     * 단일 투표하기 api에서 사용함
     */
    public void voting(ExecuteContext context) {
        Map<String, Object> data = context.getData() ;

        Date now = new Date();
        String mbrId = data.get("snsTypeCd") + ">" + data.get("snsKey");
        String voteDate = DateFormatUtils.format(now, "yyyyMMdd");

        Node voteBasInfo = NodeUtils.getNode(VOTE_BAS_INFO, data.get(VOTE_SEQ).toString());
        if (voteBasInfo!=null) {

            String rstrtnPredDivCd = (String) voteBasInfo.getStoreValue("rstrtnPredDivCd");
            switch (rstrtnPredDivCd) {
                case "rstrtnPredDivCd>day" : {
                    Integer rstrtnDayCnt = voteBasInfo.getIntValue("rstrtnDayCnt") ;
                    Integer rstrtnVoteCnt = voteBasInfo.getIntValue("rstrtnVoteCnt") ;

                    if(!mbrVoteCount.containsKey(mbrId)) {
                        Map<String, Integer> voteHstMap = new ConcurrentHashMap<>() ;
                        voteHstMap.put(voteBasInfo.getId(), selectVoteHstByDate(mbrId, voteDate, voteBasInfo));
                        mbrVoteCount.put(mbrId, voteHstMap);
                    }

                    Map<String, Integer> voteHstMap = mbrVoteCount.get(mbrId);
                    if(!voteHstMap.containsKey(voteBasInfo.getId())) {
                        voteHstMap.put(voteBasInfo.getId(), selectVoteHstByDate(mbrId, voteDate, voteBasInfo));
                    }

                    if(mbrVoteCount.get(mbrId).get(voteBasInfo.getId()) >= rstrtnVoteCnt){
                        throw new ApiException("403", "over day vote limit ") ;
                    }
                }
            }

            // Vote by Mbr
            mbrVoteCount.get(mbrId).put(voteBasInfo.getId(), mbrVoteCount.get(mbrId).get(voteBasInfo.getId()) + 1);
            if (!voteHstSqlMap.containsKey(voteBasInfo.getId())) {
                String voteHstInsert = "INSERT INTO " + voteBasInfo.getId().toString() + "_voteHstByMbr " +
                                        "(voteDate, mbrId, created) VALUES(?,?,?)";
                voteHstSqlMap.put(voteBasInfo.getId(), voteHstInsert);
            }
            jdbcTemplate.update(voteHstSqlMap.get(voteBasInfo.getId()), voteDate, mbrId, now);

            // Vote by Item
            String voteItemSeqs = (String) data.get("voteQueiSeq");
            if (!voteItemHstSqlMap.containsKey(voteBasInfo.getId())) {
                String voteItemHstInsert = "INSERT INTO " + voteBasInfo.getId().toString() + "_voteItemHstByMbr " +
                                            "(voteDate, voteItemSeq, mbrId, created) VALUES(?,?,?,?)";
                voteItemHstSqlMap.put(voteBasInfo.getId(), voteItemHstInsert);
            }
            for(String voteItemSeq : StringUtils.split(voteItemSeqs,",")) {
                jdbcTemplate.update(voteItemHstSqlMap.get(voteBasInfo.getId()), voteDate, voteItemSeq, mbrId, now);
            }

            // TODO - PV Number Count
            /*
            if (!votePvHstSqlMap.containsKey(voteBasInfo.getId())) {
                String votePvHstInsert = "INSERT INTO " + voteBasInfo.getId().toString() + "_votePvHstByMbr" +
                                            "(voteDate, mbrId, created) VALUES(?,?,?)";
                votePvHstSqlMap.put(voteBasInfo.getId(), votePvHstInsert);
            }
            */
            // TODO - IP Number Count
        }
        else {
            throw new ApiException("404", "could not found item on VoteBasInfo");
        }


        Map<String, Integer> resMap = new ConcurrentHashMap<>();

        resMap.put("userVoteCnt", mbrVoteCount.get(mbrId).get(voteBasInfo.getId()));
        resMap.put("userPvCnt", 10);    // TODO - PV Count
        resMap.put("ipAdrVoteCnt", getNumberOfDclaSetMngCnt() - selectVoteHstByIp((String) data.get("connIpAdr"), voteDate, voteBasInfo));
        data.put("response", resMap);

        context.setResult(data);
    }

    /**
     * 2017.09.20 leehh
     * 시리즈(MAMA) 투표하기 api에서 사용함
     */
    public void seriesVoting(ExecuteContext context) {
        Map<String, String> returnResult = new HashMap<>();//response
        Map<String, Object> data = context.getData();
        if (data.isEmpty()) {
            throw new ApiException("400", "Parameter is null");
        }

        if (data.get("voteResult") == null || StringUtils.isEmpty(data.get("voteResult").toString())) {
            throw new ApiException("400", "Required Parameter : voteResult");
        }
        if (data.get("connIpAdr") == null || StringUtils.isEmpty(data.get("connIpAdr").toString())) {
            throw new ApiException("400", "Required Parameter : connIpAdr");
        }
        String voteResult = data.get("voteResult").toString();
        String connIpAdr = data.get("connIpAdr").toString();

        //Json parsing
        List<Map<String, Object>> reqJson = null;
        try{
            reqJson = JsonUtils.parsingJsonToList(voteResult);
            if (reqJson.isEmpty()) throw new ApiException("410", "voteResult is not array");
        }catch (IOException e){
            throw new ApiException("410", "voteResult format is incorrect");
        }

        Date now = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");

        Node seriesVoteBasInfo = null;
        String mbrId = null;

        //List<Map<String, Object>> resList = synchronizedList(new ArrayList());
        List<Map<String, Object>> insertList = synchronizedList(new ArrayList());
        for (Map<String, Object> voteData : reqJson) {
            voteData.put("connIpAdr",connIpAdr);//ip
            if (seriesVoteBasInfo == null) {
                seriesVoteBasInfo = NodeUtils.getNode(VOTE_BAS_INFO, voteData.get("sersVoteSeq").toString());
            }
            // 사용자 ID
            if (mbrId==null || mbrId.length()<=0) {
                mbrId = voteData.get("prtcpMbrId").toString();
            }
            insertList.add(voteData);
        }
        // TODO - Checking Available IP with mbrId and voteSeq

        String voteDate = DateFormatUtils.format(now, "yyyyMMdd");          // 투표 진행일 (현재날짜)
        String pstngStDt = seriesVoteBasInfo.get("pstngStDt").toString().substring(0, 8);   // 투표 시작일
        String pstngFnsDt = seriesVoteBasInfo.get("pstngFnsDt").toString().substring(0, 8); // 투표 종료일

        // Checking Available Date
        if (voteDate.compareTo(pstngStDt) < 0 || voteDate.compareTo(pstngFnsDt) > 0) {
            throw new ApiException("400", "Required Parameter : It is not voting period");
        }

        Long pstngStDtTime = 0L;    // 투표 시작일 (DateTime)
        Long pstngFnsDtTime = 0L;   // 투표 종료일 (DateTime)
        Long voteDtTime = 0L;       // 투표 진행일 - 현재날짜 (DateTime)
        // TODO 날짜 카운트 방식 변경 - 속도 비교 필요.
        try {
            pstngStDtTime = dateFormat.parse(pstngStDt).getTime();
            pstngFnsDtTime = dateFormat.parse(pstngFnsDt).getTime();
            voteDtTime = dateFormat.parse(voteDate).getTime();
        } catch (ParseException e) {
            throw new ApiException("500", e.getMessage());
        }
        // 총 투표 기간 - Day Count
        Long seriesDayCnt = (pstngFnsDtTime - pstngStDtTime) / (24*60*60*1000);
        seriesDayCnt += 1; // 시작일 포함
        // 현재까지의 투표 기간 - Day Count
        Long dayCntByPeriod = (voteDtTime - pstngStDtTime) / (24*60*60*1000);
        dayCntByPeriod += 1; // 시작일 포함

        // 투표 날짜 간격
        Long rstrtnDayCnt = Long.valueOf(seriesVoteBasInfo.getIntValue("rstrtnDayCnt"));
        // 투표 날짜 간격별 투표수
        Long rstrtnVoteCnt = Long.valueOf(seriesVoteBasInfo.getIntValue("rstrtnVoteCnt"));

        if (rstrtnDayCnt==0 || rstrtnDayCnt==null) {
            rstrtnDayCnt = seriesDayCnt;
        }
        /*
        // TODO - 무제한 투표수 확인 - 투표 날짜 간격은 무제한일 경우 기간으로 대체 가능한데... 투표수는 무제한이 가능한가......
        if (rstrtnVoteCnt==0 || rstrtnVoteCnt==null) {
            rstrtnVoteCnt = seriesDayCnt;
        }
        */

        // Series Vote Node
        //Node seriesVoteData = NodeUtils.getNode(
        // Response Parameter
        // 날짜 간격 투표수
        Long maxVoteCntByMbr = Long.valueOf(rstrtnVoteCnt);
        Long voteCntByMbr = 0L;
        // 누적 투표 여부 확인
        if (seriesVoteBasInfo.getBooleanValue("accumRstrtnAlwdYn")) {
            // 현재기준 최대 누적 투표수 (날짜 간격 투표수 계산)
            maxVoteCntByMbr = (dayCntByPeriod / rstrtnDayCnt) * rstrtnVoteCnt;
            if((dayCntByPeriod % rstrtnDayCnt) > 0) {
                maxVoteCntByMbr += rstrtnVoteCnt;
            }
            // 누적 투표수
            voteCntByMbr = selectSeriesVoteHstByAccum(seriesVoteBasInfo.get("voteSeq").toString(), mbrId, pstngStDt.toString(), voteDate);

        } else {
            // 날짜 간격별 조회를 위한 시작일 계산   ---- 투표 진행일수
            Long rstrtnStDate = voteDtTime - ((dayCntByPeriod % rstrtnDayCnt) * (24*60*60*1000));
            Date chkDt = new Date(rstrtnStDate);
            String voteCntStDt = DateFormatUtils.format(chkDt, "yyyyMMdd");

            if (voteCntStDt.compareTo(pstngStDt) < 0) {
                voteCntStDt = pstngStDt;
            }
            // 현재기준 투표수
            voteCntByMbr = selectSeriesVoteHstByAccum(seriesVoteBasInfo.get("voteSeq").toString(), mbrId, voteCntStDt.toString(), voteDate);
        }

        // 가능한 투표수 확인
        if(voteCntByMbr>=maxVoteCntByMbr) {
            throw new ApiException("420", "You have exceeded the number of votes");
        }


        // TODO - 접근 IP 관리 테이블에 등록 -

        // 투표 등록
        String insertVoteHst = "INSERT INTO " + seriesVoteBasInfo.get(VOTE_SEQ).toString() + "_voteHstByMbr" +
                "(voteDate, mbrId, created) VALUES(?,?,?)";
        jdbcTemplate.update(insertVoteHst, voteDate, mbrId, now);
        // 투표 하위 아이템 등록
        for (Map<String, Object> voteData : insertList) {
            String voteItemSeq = voteData.get("voteItemSeq").toString();
            // Vote Item 투표 등록 // TODO - series 요청일 경우 "voteQueiSeq"와 같이 입력값이 여러개 인지 확인 필요.
            String insertSeriesVoteItemHst = "INSERT INTO " + voteData.get(VOTE_SEQ) + "_voteItemHstByMbr " +
                    "(voteDate, voteItemSeq, mbrId, created) VALUES(?,?,?,?)";
            jdbcTemplate.update(insertSeriesVoteItemHst, voteDate, voteItemSeq, mbrId, now);
        }


        //node create
        //Node resNode = (Node) nodeService.executeNode(seriesVoteBasInfo, "sersVotePrtcptHst", EventService.CREATE);
        Map<String, Object> responseMap = new ConcurrentHashMap<>() ;
        //responseMap.put("sersVoteSeq", seriesVoteBasInfo.get(VOTE_SEQ).toString());
        //responseMap.put("maxVoteCnt", maxVoteCntByMbr);    // TODO - Delete
        responseMap.put("userVoteCnt", voteCntByMbr + 1);
        responseMap.put("userPvCnt", 10);
        responseMap.put("ipAdrVoteCnt", 10);

        //resList.add(resNode);
        Map<String, Object> response = new ConcurrentHashMap<>();
        response.put("response", responseMap);
        if (response.size() > 0) {
            context.setResult(response);
        } else {
            logger.info("###sersVotePrtcptHst result null ");
        }
    }


    public void hstTableCreate(ExecuteContext context) {
        Node voteBasNode = context.getNode();
        String mbrTableName = voteBasNode.getId().toString() + "_voteHstByMbr";
        String createMbrTableSql = String.format("CREATE TABLE %s (" +
                                                "seq bigInt NOT NULL AUTO_INCREMENT COMMENT '일련번호', " +
                                                "voteDate varchar(8) NOT NULL COMMENT '투표일자', " +
                                                "mbrId varchar(220) NOT NULL COMMENT '회원아이디', " +
                                                "created datetime NOT NULL COMMENT '등록일시', " +
                                                "PRIMARY KEY (seq)" +
                                                ")"
                                              , mbrTableName);
        String mbrPvTableName = voteBasNode.getId().toString() + "_votePvHstByMbr";
        String createMbrPvTableSql = String.format("CREATE TABLE %s (" +
                                                    "seq bigInt NOT NULL AUTO_INCREMENT COMMENT '일련번호', " +
                                                    "voteDate varchar(8) NOT NULL COMMENT '투표일자', " +
                                                    "mbrId varchar(220) NOT NULL COMMENT '회원아이디', " +
                                                    "created datetime NOT NULL COMMENT '등록일시', " +
                                                    "PRIMARY KEY (seq)" +
                                                    ")"
                                            , mbrPvTableName);
        String itemTableName = voteBasNode.getId().toString() + "_voteItemHstByMbr";
        String createItemTableSql = String.format("CREATE TABLE %s (" +
                        "seq bigInt NOT NULL AUTO_INCREMENT COMMENT '일련번호', " +
                        "voteDate varchar(8) NOT NULL COMMENT '투표일자', " +
                        "voteSeq bigInt NOT NULL COMMENT '투표 일련번호', " +
                        "voteItemSeq bigInt NOT NULL COMMENT '투표 항목', " +
                        "mbrId varchar(220) NOT NULL COMMENT '회원아이디', " +
                        "created datetime NOT NULL COMMENT '등록일시', " +
                        "PRIMARY KEY (seq)" +
                        ")"
                , itemTableName);

        try {
            //JdbcTemplate jdbcTemplate2 = NodeUtils.getNodeBindingService().getNodeBindingInfo("voteBasInfo").getJdbcTemplate();
            jdbcTemplate.execute(createMbrTableSql);
            jdbcTemplate.execute(createItemTableSql);
            String voteFormlCd = voteBasNode.getStringValue("voteFormlCd");
            if("voteFormlCd>1".equals(voteFormlCd)){//토너먼트인 경우에만 pv 이력테이블 생성
                jdbcTemplate.execute(createMbrPvTableSql);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    // TODO - id 값 확인 필요.
    private Integer getNumberOfDclaSetMngCnt() {
        Node dclaSetupMngNode = NodeUtils.getNode("dclaSetupMng", "2"); // TODO 변경 필요.
        if (dclaSetupMngNode == null) {
            return 0;
        }
        return dclaSetupMngNode.getIntValue("setupBaseCnt");
    }

    private Integer selectVoteHstByDate(String mbrId, String voteDate, Node voteBasInfo) {
        if(!voteHstSelectSqlMap.containsKey(voteBasInfo.getId())) {
            String voteHstSelectByDate = "SELECT count(*) cnt " +
                    "FROM " + voteBasInfo.getId() + "_voteHstByMbr " +
                    "WHERE mbrId = ? and voteDate = ? ";
            voteHstSelectSqlMap.put(voteBasInfo.getId(), voteHstSelectByDate) ;
        }
        Map<String, Object> result = jdbcTemplate.queryForMap(voteHstSelectSqlMap.get(voteBasInfo.getId()), mbrId, voteDate) ;
        Integer cnt = Integer.parseInt(result.get("cnt").toString()) ;

        return cnt;
    }

    // 현재까지의 누적 투표 수
    private Long selectSeriesVoteHstByAccum(String voteId, String mbrId, String startVoteDate, String voteDate) {
        String voteHstSelectByDate = "SELECT count(*) voteCnt " +
                "FROM " + voteId + "_voteHstByMbr " +
                "WHERE mbrId = ? AND voteDate >= ? AND voteDate <= ?";
        Map<String, Object> result = jdbcTemplate.queryForMap(voteHstSelectByDate, mbrId, startVoteDate, voteDate) ;

        return Long.parseLong(result.get("voteCnt").toString());
    }
    // 오늘날짜의 투표 수
    private Long selectSeriesVoteHstByDate(String voteId, String mbrId, String voteDate) {
        String voteHstSelectByDate = "SELECT count(*) voteCnt " +
                "FROM " + voteId + "_voteHstByMbr " +
                "WHERE mbrId = ? AND voteDate = ?";
        Map<String, Object> result = jdbcTemplate.queryForMap(voteHstSelectByDate, mbrId, voteDate) ;

        return Long.parseLong(result.get("voteCnt").toString());
    }

    // TODO - IP
    private Integer selectVoteHstByIp(String ip, String voteDate, Node voteBasInfo) {
        if(!voteHstByIpSelectSqlMap.containsKey(voteBasInfo.getId())) {
            String voteHstSelectByDate = "SELECT count(*) cnt " +
                    "FROM voteHstByIp " +
                    "WHERE ipAdr = ? and voteDate = ? "; // TODO - voteDate가 필요한지 확인 - 투표 전체가 대상인지 또는 일자별로 구분할지 확인 필요
            voteHstByIpSelectSqlMap.put(voteBasInfo.getId(), voteHstSelectByDate) ;
        }
        Map<String, Object> result = jdbcTemplate.queryForMap(voteHstSelectSqlMap.get(voteBasInfo.getId()), ip, voteDate) ;
        Integer cnt = Integer.parseInt(result.get("cnt").toString()) ;

        return cnt;
    }

}
