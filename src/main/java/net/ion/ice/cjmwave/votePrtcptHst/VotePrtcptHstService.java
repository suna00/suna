package net.ion.ice.cjmwave.votePrtcptHst;

import com.hazelcast.core.IMap;
import com.hazelcast.core.IQueue;
import net.ion.ice.cjmwave.errMsgInfo.ErrMsgUtil;
import net.ion.ice.core.api.ApiException;
import net.ion.ice.core.cluster.ClusterService;
import net.ion.ice.core.cluster.JdbcSqlData;
import net.ion.ice.core.context.ExecuteContext;
import net.ion.ice.core.data.bind.NodeBindingUtils;
import net.ion.ice.core.event.EventService;
import net.ion.ice.core.json.JsonUtils;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeService;
import net.ion.ice.core.node.NodeUtils;
import net.ion.ice.core.node.PropertyType;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.infinispan.commons.util.ByRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Collections.synchronizedList;

/**
 * Created by leehh on 2017. 9. 20.
 */
@Service("votePrtcptHstService")
public class VotePrtcptHstService {

    private static Logger logger = LoggerFactory.getLogger(VotePrtcptHstService.class);

    private ErrMsgUtil errMsgUtil = new ErrMsgUtil();

    public static final String VOTE_BAS_INFO = "voteBasInfo";
    public static final String VOTE_SEQ = "voteSeq";

    public static final String VOTE_ITEM_INFO = "voteItemInfo";
    public static final String VOTE_ITEM_SEQ = "voteItemSeq";

    public static final String PRTCP_MBR_ID = "prtcpMbrId";
    public static final String CONN_IP_ADR = "connIpAdr";

    @Autowired
    private NodeService nodeService;

    @Autowired
    private ClusterService clusterService ;


    private JdbcTemplate jdbcTemplate ;

    private Map<String, String> voteHstSqlMap = new ConcurrentHashMap<>() ;
    private Map<String, String> voteItemHstSqlMap = new ConcurrentHashMap<>() ;
    private Map<String, String> votePvHstSqlMap = new ConcurrentHashMap<>() ;   // TODO - PV Counting
    private Map<String, String> voteHstSelectSqlMap = new ConcurrentHashMap<>() ;
    private Map<String, String> voteHstByIpSelectSqlMap = new ConcurrentHashMap<>() ;

    // Ip 접근 관리
    private IMap<String, Integer> voteIPCntMap ;

    private IMap<String, Map<String, Integer>> mbrVoteCount  ;

    private IQueue<JdbcSqlData> jdbcQueue ;


    @PostConstruct
    public void init() {
        voteIPCntMap = clusterService.getMap("ipVoteCnt") ;
        mbrVoteCount = clusterService.getMap("mbrVoteMap") ;
        jdbcQueue = clusterService.getDataQueue();
    }

    /**
     * 2017.09.27 latinus.hong
     * 단일 투표하기 api에서 사용함
     */
    public void voting(ExecuteContext context) {
        if (jdbcTemplate == null) {
            jdbcTemplate = NodeUtils.getNodeBindingService().getNodeBindingInfo(VOTE_BAS_INFO).getJdbcTemplate();
        }
        Map<String, Object> data = context.getData() ;

        Date now = new Date();
        String mbrId = data.get("snsTypeCd") + ">" + data.get("snsKey");
        String voteDate = DateFormatUtils.format(now, "yyyyMMdd");

        String connIpAdr = data.get(CONN_IP_ADR).toString();
        if(connIpAdr==null || connIpAdr.length()<=0) {
            // Incorrect your IP Address.
            throw new ApiException("421", errMsgUtil.getErrMsg(context,"421"));
        }

        Node voteBasInfo = NodeUtils.getNode(VOTE_BAS_INFO, data.get(VOTE_SEQ).toString());
        for (String voteItemSeq : data.get("voteQueiSeq").toString().split(",")) {
            Node voteItemInfo = NodeUtils.getNode("voteItemInfo", voteItemSeq.trim());
            if(voteItemInfo == null || !voteItemInfo.get("voteSeq").toString().equals(voteBasInfo.getId())){
                logger.warn("VOTE INFO WARNING : {} - {}.{}", voteBasInfo.getId(), voteItemInfo.get("voteSeq"), voteItemSeq);
                throw new ApiException("424", errMsgUtil.getErrMsg(context,"424"));
            }
        }

        // Checking Available IP with mbrId and voteSeq
        String searchText = "setupTypeCd_matching=2&sorting=dclaSetupSeq desc&limit=1";
        List<Node> dclaNodeList = nodeService.getNodeList("dclaSetupMng", searchText);
        Node dclaNode = dclaNodeList.get(0);
        Integer ipDclaCnt = dclaNode.getIntValue("setupBaseCnt");

        Integer mbrIpDclaCnt = getIpCnt(connIpAdr, voteDate);
        if (mbrIpDclaCnt >= ipDclaCnt) {
            // This IP connection has exceeded the maximum number.
            throw new ApiException("421", errMsgUtil.getErrMsg(context,"421"));
        }

        if (voteBasInfo!=null) {
            // Checking Available Date
            String pstngStDt = voteBasInfo.getStringValue("pstngStDt");
            if (pstngStDt.length()>0) {
                pstngStDt = pstngStDt.substring(0, 8);   // 투표 시작일
            }
            String pstngFnsDt = voteBasInfo.getStringValue("pstngFnsDt");
            if (pstngFnsDt.length()>0) {
                pstngFnsDt = pstngFnsDt.substring(0, 8); // 투표 종료일
            }
            if (voteDate.compareTo(pstngStDt) < 0 || voteDate.compareTo(pstngFnsDt) > 0) {
                // It is not voting period.
                throw new ApiException("422", errMsgUtil.getErrMsg(context,"422"));
            }

            String rstrtnPredDivCd = (String) voteBasInfo.getStoreValue("rstrtnPredDivCd");
            logger.info("vote rstrtnPredDivCd = " + rstrtnPredDivCd);
//            switch (rstrtnPredDivCd) {
//                case "rstrtnPredDivCd>day" : {
                    Integer rstrtnDayCnt = voteBasInfo.getIntValue("rstrtnDayCnt") ;
                    Integer rstrtnVoteCnt = voteBasInfo.getIntValue("rstrtnVoteCnt") ;

                    if (rstrtnDayCnt==null || rstrtnDayCnt==0) {
                        FastDateFormat dateFormat = FastDateFormat.getInstance("yyyyMMdd");
                    }

//                    if(!mbrVoteCount.containsKey(mbrId)) {
//                        Map<String, Integer> voteHstMap = new ConcurrentHashMap<>() ;
//                        voteHstMap.put(voteBasInfo.getId(), selectVoteHstByDate(mbrId, voteDate, voteBasInfo));
//                        mbrVoteCount.put(mbrId, voteHstMap);
//                    }

//                    Map<String, Integer> voteHstMap = mbrVoteCount.get(mbrId);
//                    if(!voteHstMap.containsKey(voteBasInfo.getId())) {
//                        voteHstMap.put(voteBasInfo.getId(), selectVoteHstByDate(mbrId, voteDate, voteBasInfo));
//                    }

                    if(selectVoteHstByDate(mbrId, voteDate, voteBasInfo) >= rstrtnVoteCnt){
//                        if(mbrVoteCount.get(mbrId).get(voteBasInfo.getId()) >= rstrtnVoteCnt){

                        // 투표수 초과
                        throw new ApiException("423", errMsgUtil.getErrMsg(context,"423"));
                    }
//                }
//            }

            // 접근 IP 관리 테이블에 등록
            executeQuery(insertIpDclaCnt, voteDate, connIpAdr, now);
//            jdbcTemplate.update(insertIpDclaCnt, voteDate, connIpAdr, now);
            // 접근 IP Count 관리 Map에 등록
            voteIPCntMap.put(connIpAdr+">"+voteDate, mbrIpDclaCnt+1);

            // Vote by Mbr
//            Integer chkVoteCnt = mbrVoteCount.get(mbrId).get(voteBasInfo.getId());
//            Map<String, Integer> mbrMap = mbrVoteCount.get(mbrId);
//            mbrMap.put(voteBasInfo.getId(), chkVoteCnt + 1) ;
//
//            mbrVoteCount.put(mbrId, mbrMap) ;

//            Map<String, Integer> checkMap = mbrVoteCount.get(mbrId);
//            logger.info("mbrVoteCount data - {} - {} ", checkMap, checkMap.get(voteBasInfo.getId()).toString());


            if (!voteHstSqlMap.containsKey(voteBasInfo.getId())) {
                String voteHstInsert = "INSERT INTO " + voteBasInfo.getId().toString() + "_voteHstByMbr " +
                                        "(voteDate, mbrId, created) VALUES(?,?,?)";
                voteHstSqlMap.put(voteBasInfo.getId(), voteHstInsert);
            }

            executeQuery(voteHstSqlMap.get(voteBasInfo.getId()), voteDate, mbrId, now);
//            jdbcTemplate.update(voteHstSqlMap.get(voteBasInfo.getId()), voteDate, mbrId, now);

            // Vote by Item
            if (!voteItemHstSqlMap.containsKey(voteBasInfo.getId())) {
                String voteItemHstInsert = "INSERT INTO " + voteBasInfo.getId().toString() + "_voteItemHstByMbr " +
                                            "(voteDate, voteItemSeq, mbrId, created) VALUES(?,?,?,?)";
                voteItemHstSqlMap.put(voteBasInfo.getId(), voteItemHstInsert);
            }
            String voteItemSeqs = (String) data.get("voteQueiSeq");
            for(String voteItemSeq : StringUtils.split(voteItemSeqs,",")) {
                executeQuery(voteItemHstSqlMap.get(voteBasInfo.getId()), voteDate, voteItemSeq, mbrId, now);
//                jdbcTemplate.update(voteItemHstSqlMap.get(voteBasInfo.getId()), voteDate, voteItemSeq, mbrId, now);
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
            throw new ApiException("424", errMsgUtil.getErrMsg(context,"424"));
        }

        Map<String, Object> createItem = new ConcurrentHashMap<>();
        Map<String, Integer> resMap = new ConcurrentHashMap<>();

        resMap.put("userVoteCnt", mbrVoteCount.get(mbrId).get(voteBasInfo.getId()));
        resMap.put("userPvCnt", 10);    // TODO - PV Count
        resMap.put("ipAdrVoteCnt", ipDclaCnt - mbrIpDclaCnt - 1);
        //data.put("createItem", resMap);

        createItem.put("createItem", resMap);
        context.setResult(createItem);
    }

    private void executeQuery(String sql, Object... params) {
        try {
            jdbcQueue.put(new JdbcSqlData(sql, params));
        } catch (InterruptedException e) {
            e.printStackTrace();
            jdbcTemplate.update(sql, params);
        }
    }

    /**
     * 2017.09.20 leehh
     * 시리즈(MAMA) 투표하기 api에서 사용함
     */
    public void seriesVoting(ExecuteContext context) {

        if (jdbcTemplate == null) {
            jdbcTemplate = NodeUtils.getNodeBindingService().getNodeBindingInfo(VOTE_BAS_INFO).getJdbcTemplate();
        }

        Map<String, Object> data = context.getData();
        if (data.isEmpty()) {   // Parameter is null
            throw new ApiException("420", errMsgUtil.getErrMsg(context,"420"));
        }
        if (data.get("voteResult") == null || StringUtils.isEmpty(data.get("voteResult").toString())) {
            // Required Parameter : voteResult
            throw new ApiException("420", errMsgUtil.getErrMsg(context,"420"));
        }
        if (data.get("connIpAdr") == null || StringUtils.isEmpty(data.get(CONN_IP_ADR).toString())) {
            // Required Parameter : connIpAdr
            throw new ApiException("420", errMsgUtil.getErrMsg(context,"420"));
        }

        String voteResult = data.get("voteResult").toString();
        String connIpAdr = data.get(CONN_IP_ADR).toString();

        //Json parsing
        List<Map<String, Object>> reqJson = null;
        try {
            reqJson = JsonUtils.parsingJsonToList(voteResult);
            if (reqJson.isEmpty()) {
                // voteResult is not array
                throw new ApiException("420", errMsgUtil.getErrMsg(context,"420"));
            }
        } catch (IOException e){
            // voteResult format is incorrect
            throw new ApiException("420", errMsgUtil.getErrMsg(context,"420"));
        }

        Date now = new Date();
        FastDateFormat dateFormat = FastDateFormat.getInstance("yyyyMMdd");

        Node seriesVoteBasInfo = null;
        String mbrId = null;

        List<Map<String, Object>> insertList = synchronizedList(new ArrayList());
        for (Map<String, Object> voteData : reqJson) {
            if (seriesVoteBasInfo == null) {
                seriesVoteBasInfo = NodeUtils.getNode(VOTE_BAS_INFO, voteData.get("sersVoteSeq").toString());
            }
            // 사용자 ID
            if (mbrId==null || mbrId.length()<=0) {
                mbrId = voteData.get(PRTCP_MBR_ID).toString();
            }
            insertList.add(voteData);
        }

        String voteDate = DateFormatUtils.format(now, "yyyyMMdd");          // 투표 진행일 (현재날짜)
        String pstngStDt = seriesVoteBasInfo.getStringValue("pstngStDt");
        if (pstngStDt.length()>0) {
            pstngStDt = pstngStDt.substring(0, 8);   // 투표 시작일
        }
        String pstngFnsDt = seriesVoteBasInfo.getStringValue("pstngFnsDt");
        if (pstngFnsDt.length()>0) {
            pstngFnsDt = pstngFnsDt.substring(0, 8); // 투표 종료일
        }

        // Checking Available Date
        if (voteDate.compareTo(pstngStDt) < 0 || voteDate.compareTo(pstngFnsDt) > 0) {
            // It is not voting period.
            throw new ApiException("422", errMsgUtil.getErrMsg(context,"422"));
        }

        // Checking Available IP with mbrId and voteSeq
        List<Node> dclaNodeList = NodeUtils.getNodeService()
                .getNodeList("dclaSetupMng","setupTypeCd_matching=2&sorting=dclaSetupSeq desc&limit=1");
        Node dclaNode = dclaNodeList.get(0);

        Integer ipDclaCnt = dclaNode.getIntValue("setupBaseCnt");
        // 접근 IP 관리
        Integer mbrIpDclaCnt = getIpCnt(connIpAdr, voteDate);
        if (mbrIpDclaCnt >= ipDclaCnt) {
            // This IP connection has exceeded the maximum number.
            throw new ApiException("421", errMsgUtil.getErrMsg(context,"421"));
        }

        Long pstngStDtTime = 0L;    // 투표 시작일 (DateTime)
        Long pstngFnsDtTime = 0L;   // 투표 종료일 (DateTime)
        Long voteDtTime = 0L;       // 투표 진행일 - 현재날짜 (DateTime)
        try {
            pstngStDtTime = dateFormat.parse(pstngStDt).getTime();
            pstngFnsDtTime = dateFormat.parse(pstngFnsDt).getTime();
            voteDtTime = dateFormat.parse(voteDate).getTime();
        } catch (ParseException e) {
            // Server Error
            throw new ApiException("425", errMsgUtil.getErrMsg(context,"425"));
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
            voteCntByMbr = selectSeriesVoteHstByAccum(seriesVoteBasInfo.get("voteSeq").toString(), mbrId, pstngStDt.toString(), voteDate);

        } else {
            // 날짜 간격별 조회를 위한 시작일 계산   ---- 투표 진행일수
            Long rstrtnStDate = voteDtTime - ((dayCntByPeriod % rstrtnDayCnt) * (24*60*60*1000));
            Date chkDt = new Date(rstrtnStDate);
            String voteCntStDt = DateFormatUtils.format(chkDt, "yyyyMMdd");

            if (voteCntStDt.compareTo(pstngStDt) < 0) {
                voteCntStDt = pstngStDt;
            }
            voteCntByMbr = selectSeriesVoteHstByAccum(seriesVoteBasInfo.get("voteSeq").toString(), mbrId, voteCntStDt.toString(), voteDate);
        }

        // 가능한 투표수 확인
        if(voteCntByMbr>=maxVoteCntByMbr) {
            throw new ApiException("423", errMsgUtil.getErrMsg(context,"423"));
        }

        // 접근 IP 관리 테이블에 등록
        executeQuery(insertIpDclaCnt, voteDate, connIpAdr, now);
//        jdbcTemplate.update(insertIpDclaCnt, voteDate, connIpAdr, now);
        // 접근 IP Count 관리 Map에 등록
        voteIPCntMap.put(connIpAdr+">"+voteDate, mbrIpDclaCnt+1);

        // 투표 등록
        String insertVoteHst = "INSERT INTO " + seriesVoteBasInfo.get(VOTE_SEQ).toString() + "_voteHstByMbr" +
                "(voteDate, mbrId, created) VALUES(?,?,?)";
        executeQuery(insertVoteHst, voteDate, mbrId, now);
//        jdbcTemplate.update(insertVoteHst, voteDate, mbrId, now);

        // TODO 관리를 위한 Map 생성 및 업데이트
        //seriesVoteInfoMap.put(seriesVoteBasInfo.get(VOTE_SEQ), )

        // 투표 하위 아이템 등록
        for (Map<String, Object> voteData : insertList) {
            String voteItemSeq = voteData.get(VOTE_ITEM_SEQ).toString();
            // Vote Item 투표 등록 // TODO - series 요청일 경우 "voteQueiSeq"와 같이 입력값이 여러개 인지 확인 필요.
            String insertSeriesVoteItemHst = "INSERT INTO " + voteData.get(VOTE_SEQ) + "_voteItemHstByMbr " +
                    "(voteDate, voteItemSeq, mbrId, created) VALUES(?,?,?,?)";
            executeQuery(insertSeriesVoteItemHst, voteDate, voteItemSeq, mbrId, now);
//            jdbcTemplate.update(insertSeriesVoteItemHst, voteDate, voteItemSeq, mbrId, now);
        }

        // 접근 IP 관리 테이블에 등록
        executeQuery(insertIpDclaCnt, voteDate, connIpAdr, now);
        // jdbcTemplate.update(insertIpDclaCnt, voteDate, connIpAdr, now);
        // 접근 IP Count 관리 Map에 등록
        voteIPCntMap.put(connIpAdr+">"+voteDate, mbrIpDclaCnt+1);


        // 이벤트 투표 생성
        Integer dayEventVoteRstrtnCnt = seriesVoteBasInfo.getIntValue("dayEventVoteRstrtnCnt");
        Integer contnuEventVoteRstrtnCnt = seriesVoteBasInfo.getIntValue("contnuEventVoteRstrtnCnt");
        if (dayEventVoteRstrtnCnt>0) {
            addEvtVoteNum(now, mbrId, dayEventVoteRstrtnCnt, contnuEventVoteRstrtnCnt);
        }

        //node create
        Map<String, Object> responseMap = new ConcurrentHashMap<>() ;
        //responseMap.put("sersVoteSeq", seriesVoteBasInfo.get(VOTE_SEQ).toString());
        //responseMap.put("maxVoteCnt", maxVoteCntByMbr);
        responseMap.put("userVoteCnt", voteCntByMbr + 1);
        responseMap.put("ipAdrVoteCnt", ipDclaCnt - mbrIpDclaCnt - 1);

        Map<String, Object> response = new ConcurrentHashMap<>();
        response.put("response", responseMap);
        if (response.size() > 0) {
            context.setResult(response);
        } else {
            logger.info("###sersVotePrtcptHst result null ");
        }
    }

    // 접근 IP Count 조회
    private Integer getIpCnt(String connIpAdr, String voteDate) {
        Integer mbrIpDclaCnt;
        String ipCntKey = connIpAdr + ">" + voteDate;
        if (voteIPCntMap.get(ipCntKey) != null) {
            mbrIpDclaCnt = (Integer) voteIPCntMap.get(ipCntKey);
        } else {
            String selectIpDclaCnt = "SELECT count(*) ipCnt FROM voteHstByIp WHERE ipAdr=? AND voteDate=?";
            Map<String, Object> ipCntMap = jdbcTemplate.queryForMap(selectIpDclaCnt, connIpAdr, voteDate);
            mbrIpDclaCnt = Integer.parseInt(ipCntMap.get("ipCnt").toString());
            voteIPCntMap.put(ipCntKey, mbrIpDclaCnt);
        }
        return mbrIpDclaCnt;
    }

    private void putIpCntInfo(String connIpAdr, String voteDate) {

    }

    public void hstTableCreate(ExecuteContext context) {
        if(jdbcTemplate == null){
            jdbcTemplate = NodeUtils.getNodeBindingService().getNodeBindingInfo(VOTE_BAS_INFO).getJdbcTemplate();
        }
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
                        //"voteSeq bigInt NOT NULL COMMENT '투표 일련번호', " +
                        "voteItemSeq bigInt NOT NULL COMMENT '투표 항목', " +
                        "mbrId varchar(220) NOT NULL COMMENT '회원아이디', " +
                        "created datetime NOT NULL COMMENT '등록일시', " +
                        "PRIMARY KEY (seq)" +
                        ")"
                , itemTableName);

        try {
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

    // [IF-MEV-002] 이벤트 투표
    public void evtVoting(ExecuteContext context) {
        if (jdbcTemplate == null) {
           jdbcTemplate = NodeUtils.getNodeBindingService().getNodeBindingInfo(VOTE_BAS_INFO).getJdbcTemplate();
        }

        Map<String, Object> data = context.getData();
        if (data.isEmpty()) {   // Parameter is null
            throw new ApiException("420", errMsgUtil.getErrMsg(context,"420"));
        }
        if (data.get("connIpAdr") == null || StringUtils.isEmpty(data.get(CONN_IP_ADR).toString())) {
            // Required Parameter : connIpAdr
            throw new ApiException("420", errMsgUtil.getErrMsg(context,"420"));
        }

        // 사용자 ID
        String mbrId = data.get("snsTypeCd") + ">" + data.get("snsKey");
        String connIpAdr = data.get(CONN_IP_ADR).toString();

        Date now = new Date();
        FastDateFormat dateFormat = FastDateFormat.getInstance("yyyyMMdd");
        String voteDate = DateFormatUtils.format(now, "yyyyMMdd");

        // 이벤트 투표 정보 조회
        Node eventVoteBasInfo = NodeUtils.getNode(VOTE_BAS_INFO, data.get("voteSeq").toString());
        String pstngStDt = eventVoteBasInfo.getStringValue("pstngStDt");
        if (pstngStDt.length()>0) {
            pstngStDt = pstngStDt.substring(0, 8);   // 투표 시작일
        }
        String pstngFnsDt = eventVoteBasInfo.getStringValue("pstngFnsDt");
        if (pstngFnsDt.length()>0) {
            pstngFnsDt = pstngFnsDt.substring(0, 8); // 투표 종료일
        }

        // Checking Available Date
        if (voteDate.compareTo(pstngStDt) < 0 || voteDate.compareTo(pstngFnsDt) > 0) {
            // It is not voting period.
            throw new ApiException("422", errMsgUtil.getErrMsg(context,"422"));
        }

        // Checking Available IP with mbrId and voteSeq
        List<Node> dclaNodeList = NodeUtils.getNodeService()
                .getNodeList("dclaSetupMng","setupTypeCd_matching=2&sorting=dclaSetupSeq desc&limit=1");
        Node dclaNode = dclaNodeList.get(0);

        Integer ipDclaCnt = dclaNode.getIntValue("setupBaseCnt");
        Integer mbrIpDclaCnt = getIpCnt(connIpAdr, voteDate);
        if (mbrIpDclaCnt >= ipDclaCnt) {
            // This IP connection has exceeded the maximum number.
            throw new ApiException("421", errMsgUtil.getErrMsg(context,"421"));
        }

        // 투표수 확인
        Map<String, Object> voteEvtCntByMbr = selectVoteEvtByMbr(mbrId);
        if (voteEvtCntByMbr == null) {
            throw new ApiException("426", "You do not have event votes.");
        }
        //Integer voteCnt = selectVoteCntByMbrId(data.get(VOTE_SEQ).toString(), mbrId);
        Integer usedVoteNum = Integer.parseInt(voteEvtCntByMbr.get("usedVoteNum").toString());
        Integer voteNum= Integer.parseInt(voteEvtCntByMbr.get("voteNum").toString());
        if (usedVoteNum >= voteNum) {
            throw new ApiException("423", errMsgUtil.getErrMsg(context, "423"));
        }

        // 투표 진행
        String insertEventVoteHst = "INSERT INTO " + eventVoteBasInfo.getId() + "_voteHstByMbr " +
                "(voteDate, mbrId, created) VALUES(?,?,?)";
        //voteHstSqlMap.put(eventVoteBasInfo.getId(), voteHstInsert);
        executeQuery(insertEventVoteHst, voteDate, mbrId, now);
//        jdbcTemplate.update(insertEventVoteHst, voteDate, mbrId, now);

        String voteItemSeqs = (String) data.get("voteQueiSeq");
        String insertEventVoteItemHst = "INSERT INTO " + data.get(VOTE_SEQ) + "_voteItemHstByMbr " +
                "(voteDate, voteItemSeq, mbrId, created) VALUES(?,?,?,?)";

        for (String voteItemSeq : StringUtils.split(voteItemSeqs, ",")) {
            executeQuery(insertEventVoteItemHst, voteDate, voteItemSeq, mbrId, now);
//            jdbcTemplate.update(insertEventVoteItemHst, voteDate, voteItemSeq, mbrId, now);
        }

        // 사용한 Event 투표수 증가
        usedVoteNum += 1;
        String increaseUsedVoteNum = "UPDATE voteEvtByMbr SET usedVoteNum=? WHERE mbrId=?";
        executeQuery(increaseUsedVoteNum, usedVoteNum,mbrId);
//        jdbcTemplate.update(increaseUsedVoteNum, usedVoteNum,mbrId);

        // 접근 IP 관리 테이블에 등록
        executeQuery(insertIpDclaCnt, voteDate, connIpAdr, now);
//        jdbcTemplate.update(insertIpDclaCnt, voteDate, connIpAdr, now);
        voteIPCntMap.put(connIpAdr+">"+voteDate, mbrIpDclaCnt+1);

        //node create
        Map<String, Object> resDataMap = new ConcurrentHashMap<>();

        resDataMap.put("ipAdrVoteCnt", ipDclaCnt - mbrIpDclaCnt - 1);
        resDataMap.put("userEvtVoteCnt", voteNum - usedVoteNum);

        Map<String, Object> response = new ConcurrentHashMap<>();
        response.put("response", resDataMap);
        context.setResult(response);
    }
    String selectVoteEvt = "SELECT mbrId, continuedDayCnt, usedVoteNum, voteNum, created FROM voteEvtByMbr WHERE mbrId=?";

    private Map<String,Object> selectVoteEvtByMbr(String mbrId) {
        if (jdbcTemplate == null) {
            jdbcTemplate = NodeUtils.getNodeBindingService().getNodeBindingInfo(VOTE_BAS_INFO).getJdbcTemplate();
        }

        try {
            return jdbcTemplate.queryForMap(selectVoteEvt, mbrId);
        } catch (EmptyResultDataAccessException e) {
            logger.info("이벤트 투표권이 없습니다.");
            return null;
        }
    }
    String insertIpDclaCnt = "INSERT INTO voteHstByIp (voteDate, ipAdr, created) VALUES(?,?,?)";

    // [IF-MEV-003] sponsor 투표
    public void sponsorEvtVoting(ExecuteContext context) {

        if (jdbcTemplate == null) {
            jdbcTemplate = NodeUtils.getNodeBindingService().getNodeBindingInfo(VOTE_BAS_INFO).getJdbcTemplate();
        }

        Map<String, Object> data = context.getData();
        if (data.isEmpty()) {   // Parameter is null
            throw new ApiException("420", errMsgUtil.getErrMsg(context,"420"));
        }
        if (data.get("voteResult") == null || StringUtils.isEmpty(data.get("voteResult").toString())) {
            // Required Parameter : voteResult
            throw new ApiException("420", errMsgUtil.getErrMsg(context,"420"));
        }
        if (data.get("connIpAdr") == null || StringUtils.isEmpty(data.get(CONN_IP_ADR).toString())) {
            // Required Parameter : connIpAdr
            throw new ApiException("420", errMsgUtil.getErrMsg(context,"420"));
        }

        String voteResult = data.get("voteResult").toString();
        String connIpAdr = data.get(CONN_IP_ADR).toString();

        //Json parsing
        List<Map<String, Object>> reqJson = null;
        try{
            reqJson = JsonUtils.parsingJsonToList(voteResult);
            if (reqJson.isEmpty()) {
                // voteResult is not array
                throw new ApiException("420", errMsgUtil.getErrMsg(context,"420"));
            }
        }catch (IOException e){
            // voteResult format is incorrect
            throw new ApiException("420", errMsgUtil.getErrMsg(context,"420"));
        }

        Date now = new Date();
        FastDateFormat dateFormat = FastDateFormat.getInstance("yyyyMMdd");

        Node seriesVoteBasInfo = null;
        String mbrId = null;
        boolean infoOttpAgreeYn = Boolean.parseBoolean(data.get("infoOttpAgreeYn").toString());

        List<Map<String, Object>> insertList = synchronizedList(new ArrayList());
        for (Map<String, Object> voteData : reqJson) {
            if (seriesVoteBasInfo == null) {
                seriesVoteBasInfo = NodeUtils.getNode(VOTE_BAS_INFO, voteData.get("sersVoteSeq").toString());
            }
            // 사용자 ID
            if (mbrId==null || mbrId.length()<=0) {
                mbrId = voteData.get(PRTCP_MBR_ID).toString();
            }
            insertList.add(voteData);
        }

        // 스폰서 snsType => 10
        if (mbrId!=null && mbrId.startsWith("10>")) {

            String[] mbrIdArray  = mbrId.split(">");
            String snsTypeCd = mbrIdArray[0];;
            String snsKey = mbrIdArray[1];

            //user screate
            data.put("snsTypeCd", mbrIdArray[0]);
            data.put("snsKey", snsKey);
            data.put("mbrDivCd", "1");
            data.put("mbrSttusCd", "1");
            data.put("infoOttpAgreeYn", infoOttpAgreeYn);
            data.put("pushMsgRcvYn", false);
            data.put("sbscShapCd", "1");
            data.put("sttusChgSbst", "스폰서 이벤트 가입");
            data.put("sbscDt", new Date());

            Node result = (Node) NodeUtils.getNodeService().executeNode(data, "mbrInfo", EventService.SAVE);
            context.setResult(result);
        }

        String voteDate = DateFormatUtils.format(now, "yyyyMMdd");          // 투표 진행일 (현재날짜)

        String pstngStDt = seriesVoteBasInfo.getStringValue("pstngStDt");
        if (pstngStDt.length()>0) {
            pstngStDt = pstngStDt.substring(0, 8);   // 투표 시작일
        }
        String pstngFnsDt = seriesVoteBasInfo.getStringValue("pstngFnsDt");
        if (pstngFnsDt.length()>0) {
            pstngFnsDt = pstngFnsDt.substring(0, 8); // 투표 종료일
        }
        // Checking Available Date
        if (voteDate.compareTo(pstngStDt) < 0 || voteDate.compareTo(pstngFnsDt) > 0) {
            // It is not voting period.
            throw new ApiException("422", errMsgUtil.getErrMsg(context,"422"));
        }

        // Checking Available IP with mbrId and voteSeq
        List<Node> dclaNodeList = NodeUtils.getNodeService()
                .getNodeList("dclaSetupMng","setupTypeCd_matching=2&sorting=dclaSetupSeq desc&limit=1");
        Node dclaNode = dclaNodeList.get(0);

        Integer ipDclaCnt = dclaNode.getIntValue("setupBaseCnt");


        Integer mbrIpDclaCnt = getIpCnt(connIpAdr, voteDate);
        if (mbrIpDclaCnt >= ipDclaCnt) {
            // This IP connection has exceeded the maximum number.
            throw new ApiException("421", errMsgUtil.getErrMsg(context,"421"));
        }

        Long pstngStDtTime = 0L;    // 투표 시작일 (DateTime)
        Long pstngFnsDtTime = 0L;   // 투표 종료일 (DateTime)
        Long voteDtTime = 0L;       // 투표 진행일 - 현재날짜 (DateTime)
        try {
            pstngStDtTime = dateFormat.parse(pstngStDt).getTime();
            pstngFnsDtTime = dateFormat.parse(pstngFnsDt).getTime();
            voteDtTime = dateFormat.parse(voteDate).getTime();
        } catch (ParseException e) {
            // Server Error
            //throw new ApiException("501", e.getMessage());
            throw new ApiException("425", errMsgUtil.getErrMsg(context,"425"));
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
            voteCntByMbr = selectSeriesVoteHstByAccum(seriesVoteBasInfo.get("voteSeq").toString(), mbrId, pstngStDt.toString(), voteDate);

        } else {
            // 날짜 간격별 조회를 위한 시작일 계산   ---- 투표 진행일수
            Long rstrtnStDate = voteDtTime - ((dayCntByPeriod % rstrtnDayCnt) * (24*60*60*1000));
            Date chkDt = new Date(rstrtnStDate);
            String voteCntStDt = DateFormatUtils.format(chkDt, "yyyyMMdd");

            if (voteCntStDt.compareTo(pstngStDt) < 0) {
                voteCntStDt = pstngStDt;
            }
            voteCntByMbr = selectSeriesVoteHstByAccum(seriesVoteBasInfo.get("voteSeq").toString(), mbrId, voteCntStDt.toString(), voteDate);
        }

        // 가능한 투표수 확인
        if(voteCntByMbr>=maxVoteCntByMbr) {
            throw new ApiException("423", errMsgUtil.getErrMsg(context,"423"));
        }

        // 접근 IP 관리 테이블에 등록
        executeQuery(insertIpDclaCnt, voteDate, connIpAdr, now);
//        jdbcTemplate.update(insertIpDclaCnt, voteDate, connIpAdr, now);
        // 접근 IP Count 관리 Map에 등록
        voteIPCntMap.put(connIpAdr+">"+voteDate, mbrIpDclaCnt+1);

        // 투표 등록
        String insertVoteHst = "INSERT INTO " + seriesVoteBasInfo.get(VOTE_SEQ).toString() + "_voteHstByMbr" +
                "(voteDate, mbrId, created) VALUES(?,?,?)";
        executeQuery(insertVoteHst, voteDate, mbrId, now);
//        jdbcTemplate.update(insertVoteHst, voteDate, mbrId, now);

        // TODO 관리를 위한 Map 생성 및 업데이트
        //seriesVoteInfoMap.put(seriesVoteBasInfo.get(VOTE_SEQ), )

        // 투표 하위 아이템 등록
        for (Map<String, Object> voteData : insertList) {
            String voteItemSeq = voteData.get(VOTE_ITEM_SEQ).toString();
            // Vote Item 투표 등록 // TODO - series 요청일 경우 "voteQueiSeq"와 같이 입력값이 여러개 인지 확인 필요.
            String insertSeriesVoteItemHst = "INSERT INTO " + voteData.get(VOTE_SEQ) + "_voteItemHstByMbr " +
                    "(voteDate, voteItemSeq, mbrId, created) VALUES(?,?,?,?)";
            executeQuery(insertSeriesVoteItemHst, voteDate, voteItemSeq, mbrId, now);
//            jdbcTemplate.update(insertSeriesVoteItemHst, voteDate, voteItemSeq, mbrId, now);
        }

        // 이벤트 투표 생성
        Integer dayEventVoteRstrtnCnt = seriesVoteBasInfo.getIntValue("dayEventVoteRstrtnCnt");
        Integer contnuEventVoteRstrtnCnt = seriesVoteBasInfo.getIntValue("contnuEventVoteRstrtnCnt");
        if (dayEventVoteRstrtnCnt>0) {
            addEvtVoteNum(now, mbrId, dayEventVoteRstrtnCnt, contnuEventVoteRstrtnCnt);
        }

        //node create
        Map<String, Object> responseMap = new ConcurrentHashMap<>() ;
        //responseMap.put("sersVoteSeq", seriesVoteBasInfo.get(VOTE_SEQ).toString());
        //responseMap.put("maxVoteCnt", maxVoteCntByMbr);
        responseMap.put("userVoteCnt", voteCntByMbr + 1);
        responseMap.put("ipAdrVoteCnt", ipDclaCnt - mbrIpDclaCnt - 1);

        Map<String, Object> response = new ConcurrentHashMap<>();
        response.put("response", responseMap);
        if (response.size() > 0) {
            context.setResult(response);
        } else {
            logger.info("###sersVotePrtcptHst result null ");
        }
    }

    String insertVoteEvtQuery = "INSERT INTO voteEvtByMbr (mbrId, continuedDayCnt, usedVoteNum, voteNum, created) VALUES(?,?,?,?,?)";
    String updateVoteEvtQuery = "UPDATE voteEvtByMbr SET continuedDayCnt=?, voteNum=?, created=? WHERE mbrId=?";

    private void addEvtVoteNum(Date now, String mbrId, Integer dayEventVoteRstrtnCnt, Integer contnuEventVoteRstrtnCnt) {
        Map<String, Object> voteEvtMap = selectVoteEvtByMbr(mbrId);
        Integer incVoteNum = dayEventVoteRstrtnCnt;

        // yesterday
        Calendar calendar = Calendar.getInstance();
        calendar.add(calendar.DATE, -1);
        String yesterday = DateFormatUtils.format(calendar, "yyyyMMdd");

        if (voteEvtMap==null) {
            executeQuery(insertVoteEvtQuery, mbrId, 1, 0, incVoteNum, now);
//            jdbcTemplate.update(insertVoteEvtQuery, mbrId, 1, 0, incVoteNum, now);
        }
        else {
            Date created = (Date) voteEvtMap.get("created");
            String chkDt = DateFormatUtils.format(created, "yyyyMMdd");
            Integer continued = Integer.parseInt(voteEvtMap.get("continuedDayCnt").toString()) + 1;
            if (continued==2 && chkDt.equals(yesterday)) {
                incVoteNum = incVoteNum * contnuEventVoteRstrtnCnt;
            } else {
                continued = 1;
            }

            Integer voteNum = Integer.parseInt(voteEvtMap.get("voteNum").toString()) + incVoteNum;
            executeQuery(updateVoteEvtQuery, continued, voteNum, now, mbrId);
//            jdbcTemplate.update(updateVoteEvtQuery, continued, voteNum, now, mbrId);
        }
    }
}
