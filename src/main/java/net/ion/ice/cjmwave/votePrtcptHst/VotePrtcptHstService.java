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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by leehh on 2017. 9. 20.
 */
@Service("votePrtcptHstService")
public class VotePrtcptHstService {
    private static Logger logger = LoggerFactory.getLogger(VotePrtcptHstService.class);
    private static String[] pidArray = {"sersVoteSeq", "voteSeq", "voteItemSeq", "prtcpMbrId", "connIpAdr"};

    @Autowired
    private NodeService nodeService;

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

        for (Map<String, Object> voteData : reqJson) {
            voteData.put("connIpAdr",connIpAdr);//ip
            //node create
            Node result = (Node) nodeService.executeNode(voteData, "sersVotePrtcptHst", EventService.CREATE);
            if (result != null) {
                context.setResult(result);
            } else {
                logger.info("###sersVotePrtcptHst result null ");
            }

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
                        "voteItemSeq bigInt NOT NULL COMMENT '투표일자', " +
                        "mbrId varchar(220) NOT NULL COMMENT '회원아이디', " +
                        "created datetime NOT NULL COMMENT '등록일시', " +
                        "PRIMARY KEY (seq)" +
                        ")"
                , itemTableName);

        try {
            JdbcTemplate jdbcTemplate2 = NodeUtils.getNodeBindingService().getNodeBindingInfo("voteBasInfo").getJdbcTemplate();
            jdbcTemplate2.execute(createMbrTableSql);
            jdbcTemplate2.execute(createItemTableSql);
            String voteFormlCd = voteBasNode.getStringValue("voteFormlCd");
            if("voteFormlCd>1".equals(voteFormlCd)){//토너먼트인 경우에만 pv 이력테이블 생성
                jdbcTemplate2.execute(createMbrPvTableSql);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
