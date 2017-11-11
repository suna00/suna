package net.ion.ice.cjmwave.mbrInfo;

import java.io.File;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.*;

import net.ion.ice.cjmwave.aws.CaptchaService;
import net.ion.ice.cjmwave.errMsgInfo.ErrMsgUtil;
import net.ion.ice.core.api.ApiException;
import net.ion.ice.core.context.ExecuteContext;
import net.ion.ice.core.data.DBService;
import net.ion.ice.core.data.bind.NodeBindingUtils;
import net.ion.ice.core.event.EventService;
import net.ion.ice.core.file.FileValue;
import net.ion.ice.core.infinispan.NotFoundNodeException;
import net.ion.ice.core.node.*;
import net.ion.ice.core.query.QueryResult;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by leehh on 2017. 9. 25.
 */

@Service("mbrInfoService")
public class MbrInfoService {
    private ErrMsgUtil errMsgUtil = new ErrMsgUtil();
    private JdbcTemplate jdbcTemplate;

    private Logger logger = Logger.getLogger(MbrInfoService.class);
    private List<String> cntryMngMap = Arrays.asList("CHN","KOR","THA","PHL","IDN","VNM","USA","TWN","MAL","JPN","BRA","MEX","SGP","HKG","GBR","RUS","PER","SAU","CDN","AUS","RCH","ARG","TUR","DEU","FRA","MMR","IND","COL","ESP","KAZ","ITA","KHM","POL","ECU","ARE","NLD","BOL","MAR","NZL","DZA","IRQ","UKR","EGY","SWE","BRU","HUN","VEN","PRT","FIN","MAC","OTHERS");
    private List<String> sexCdMap = Arrays.asList("1","2","3");
    private List<String> snsTypeCdMap = Arrays.asList("1","2","3","4","5","6","7","8","9","10");


    @Autowired
    private DBService dbService;

    @Autowired
    private CaptchaService captchaService;

    public Node getMbrNode(String snsType, String snsKey) {
        if (jdbcTemplate == null) {
            jdbcTemplate = dbService.getJdbcTemplate("authDb");
        }
        try {
            Map<String, Object> data = jdbcTemplate.queryForMap("select  * from mbrInfo where snsTypeCd = ? and snsKey = ?", snsType, snsKey);
            if (data != null) {
                return new Node(data, "mbrInfo");
            }
        } catch (Exception e) {
        }
        return null;
    }

    public void chkMbr(ExecuteContext context) {

        Map<String, Object> data = context.getData();
        chkSnsParams(data);
        String snsKey = data.get("snsKey").toString();
        String snsTypeCd = data.get("snsTypeCd").toString();

        Node anode = null;
        try {
            anode = getMbrNode(snsTypeCd, snsKey);
        } catch (NotFoundNodeException e) {

        }

        if (anode == null || anode.isEmpty()) {
            Map<String, Object> resultDate = new LinkedHashMap<>();
            resultDate.put("chkResult", true);
            context.setResult(resultDate);
        } else {
            logger.info("######chkMbr :" + anode);
            throw new ApiException("405", "Information that meets the conditions already exists.");
        }
    }

    public void semiMbrJoin(ExecuteContext context) {
        if (jdbcTemplate == null) {
            jdbcTemplate = NodeUtils.getNodeBindingService().getNodeBindingInfo("mbrInfo").getJdbcTemplate();
        }

        Map<String, Object> data = context.getData();
        chkSnsParams(data);
        //snsTypeCd 확인
        //chkSnsTypeCd(data);
        String snsKey = data.get("snsKey").toString();
        String snsTypeCd = data.get("snsTypeCd").toString();

        String chkMbrSql = "select  if(count(*)>0,'true','false') chkResult from mbrInfo where snsTypeCd = ? and snsKey = ?";
        Boolean chkResult = Boolean.parseBoolean(jdbcTemplate.queryForMap(chkMbrSql, snsTypeCd, snsKey).get("chkResult").toString());
        if (chkResult) {
            throw new ApiException("405", "Information that meets the conditions already exists.");
        }

        data.put("sttusChgSbst", "준회원 가입");
        int result = jdbcTemplate.update("insert into mbrInfo(snsTypeCd, snsKey, mbrDivCd, sbscDt, aliasNm, pushMsgRcvYn, sbscShapCd, infoOttpAgreeYn, mbrSttusCd) values (?, ?, '1', sysdate(),?,?,?,?,?)", snsTypeCd, snsKey, data.get("aliasNm"), "true".equals(data.get("pushMsgRcvYn")) ? 1 : 0, data.get("sbscShapCd"), "true".equals(data.get("infoOttpAgreeYn")) ? 1 : 0, "1");

        Map<String, Object> exe = new LinkedHashMap<>();
        exe.put("sbscDt", new Date());
        context.setResult(exe);
    }

    public void rglrMbrJoin(ExecuteContext context) {

        captchaService.validate(context.getHttpRequest());

        if (jdbcTemplate == null) {
            jdbcTemplate = NodeUtils.getNodeBindingService().getNodeBindingInfo("mbrInfo").getJdbcTemplate();
        }

        Map<String, Object> data = context.getData();
        chkSnsParams(data);
        String snsKey = data.get("snsKey").toString();
        String snsTypeCd = data.get("snsTypeCd").toString();

        String chkMbrSql = "select  if(count(*)>0,'false','true') chkResult from mbrInfo where snsTypeCd = ? and snsKey = ?";
        Boolean chkResult = Boolean.parseBoolean(jdbcTemplate.queryForMap(chkMbrSql, snsTypeCd, snsKey).get("chkResult").toString());
        if (chkResult) {
            throw new ApiException("404", "Not Found");
        }

        Node anode = null;
        try {
            anode = getMbrNode(snsTypeCd, snsKey);
            logger.info("##anode:" + anode);
            String mbrSttusCd = anode.getStringValue("mbrSttusCd");
            if ("2".equals(mbrSttusCd) || "3".equals(mbrSttusCd)) {
                throw new ApiException("412", errMsgUtil.getErrMsg(context, "412"));
            }
            String mbrDivCd = anode.getStringValue("mbrDivCd");
            if ("2".equals(mbrDivCd)) {
                throw new ApiException("411", errMsgUtil.getErrMsg(context, "411"));
            }
        } catch (NotFoundNodeException e) {
            throw new ApiException("404", "Not Found Member");
        }
        //국가코드 확인
        //chkCntryCd(data);
        //성별코드 확인
        //chkSexCd(data);

        //update
        data.put("snsTypeCd", snsTypeCd);
        data.put("snsKey", snsKey);
        data.put("mbrDivCd", "2");
        data.put("sttusChgSbst", "정회원 전환");
        int result = jdbcTemplate.update("update mbrInfo set mbrDivCd='2', aliasNm=?,cntryCd=?,sexCd=?,imgUrl=?,bthYear=?,intrstCdList=?,email=?,emailRcvYn=?, pushMsgRcvYn=?, infoOttpAgreeYn=?  where snsTypeCd=? and snsKey=? ", data.get("aliasNm"), data.get("cntryCd"), data.get("sexCd"), data.get("imgUrl"), data.get("bthYear"), data.get("intrstCdList"), data.get("email"), "true".equals(data.get("emailRcvYn")) ? 1 : 0, "true".equals(data.get("pushMsgRcvYn")) ? 1 : 0, "true".equals(data.get("infoOttpAgreeYn")) ? 1 : 0, snsTypeCd, snsKey);
        //Node result = (Node) NodeUtils.getNodeService().executeNode(data, "mbrInfo", EventService.UPDATE);
        context.setResult(result);
    }

    public void updLoginInfo(ExecuteContext context) {
        if (jdbcTemplate == null) {
            jdbcTemplate = dbService.getJdbcTemplate("authDb");
        }

        Map<String, Object> data = context.getData();
        chkSnsParams(data);
        String snsKey = data.get("snsKey").toString();
        String snsTypeCd = data.get("snsTypeCd").toString();

        String chkMbrSql = "select  if(count(*)>0,'false','true') chkResult from mbrInfo where snsTypeCd = ? and snsKey = ?";
        Boolean chkResult = Boolean.parseBoolean(jdbcTemplate.queryForMap(chkMbrSql, snsTypeCd, snsKey).get("chkResult").toString());
        if (chkResult) {
            throw new ApiException("404", "Not Found");
        }

        Node anode = null;
        try {
            if (StringUtils.contains(snsTypeCd, "snsTypeCd>")) {
                snsTypeCd = StringUtils.replace(snsTypeCd, "snsTypeCd>", "");
            }
            anode = getMbrNode(snsTypeCd, snsKey);
            if (anode != null && !anode.isEmpty()) {
                //node에 최종수정일 디바이스 정보 등 수정
                Map<String, Object> updateData = new LinkedHashMap<>();
//                updateData.put("id", snsTypeCd + ">" + snsKey);
//                updateData.put("lastLoginDt", new Date());
//                Node result = (Node) NodeUtils.getNodeService().executeNode(updateData, "mbrInfo", EventService.UPDATE);
//                Object imgUrlObj = anode.get("imgUrl");
//                String imgName = "";
//                if(imgUrlObj instanceof FileValue) {
//                    FileValue fileValue = (FileValue) imgUrlObj;
//                    if (fileValue != null) {
//                        imgName = fileValue.getFileName();
//                    }
//                }//else if(){
                //
                // }

                Map<String, Object> resultData = new LinkedHashMap<>();
                NodeType nodeType = NodeUtils.getNodeType(anode.getTypeId());
                context.setDateFormat("yyyy-MM-dd HH:mm:ss");
                for (PropertyType pt : nodeType.getPropertyTypes()) {
                    resultData.put(pt.getPid(), NodeUtils.getResultValue(context, pt, anode));
                }
                resultData.put("imgFileName", "");
//                System.out.println("========" + resultData);
                QueryResult queryResult = new QueryResult();
                queryResult.put("item", resultData);
                context.setResult(queryResult);
            }
        } catch (NotFoundNodeException e) {
            throw new ApiException("404", "Not Found Member");
        }
    }

    public void rejoin(ExecuteContext context) {
        if (jdbcTemplate == null) {
            jdbcTemplate = dbService.getJdbcTemplate("authDb");
        }

        Map<String, Object> data = context.getData();
        chkSnsParams(data);
        String snsKey = data.get("snsKey").toString();
        String snsTypeCd = data.get("snsTypeCd").toString();

        String chkMbrSql = "select if(count(*)>0,'false','true') chkResult from mbrInfo where snsTypeCd = ? and snsKey = ?";
        Boolean chkResult = Boolean.parseBoolean(jdbcTemplate.queryForMap(chkMbrSql, snsTypeCd, snsKey).get("chkResult").toString());
        if (chkResult) {
            throw new ApiException("404", "Not Found");
        }

        if (data.get("infoOttpAgreeYn") == null || StringUtils.isEmpty(data.get("infoOttpAgreeYn").toString())) {
            throw new ApiException("400", "Required Parameter : infoOttpAgreeYn");
        }
        String infoOttpAgreeYn = data.get("infoOttpAgreeYn").toString();
        if (!"true".equals(infoOttpAgreeYn)) {
            throw new ApiException("413", errMsgUtil.getErrMsg(context, "413"));
        }

        Node anode = null;
        try {
            anode = getMbrNode(snsTypeCd, snsKey);

            String mbrSttusCd = anode.getStringValue("mbrSttusCd");
            Date rtrmmbDate = null;
            if (anode.get("rtrmmbDate") != null) {
                try {
                    rtrmmbDate = DateUtils.parseDate(anode.getStringValue("rtrmmbDate"), "yyyyMMddHHmmss", "yyyy/MM/dd HH:mm:ss");
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
            if (!"3".equals(mbrSttusCd)) {
                throw new ApiException("414", errMsgUtil.getErrMsg(context, "414"));
            }
            if (rtrmmbDate == null) {
                throw new ApiException("415", errMsgUtil.getErrMsg(context, "415"));
            }

            List<Node> dclaNodeList = null;
            try {
                dclaNodeList = NodeUtils.getNodeService().getNodeList("dclaSetupMng", "setupTypeCd_matching=4&sorting=dclaSetupSeq desc&limit=1");
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (dclaNodeList == null || dclaNodeList.isEmpty()) {
                throw new ApiException("416", errMsgUtil.getErrMsg(context, "416"));
            }
            Node dclaNode = dclaNodeList.get(0);
            int setupBaseDayNum = dclaNode.getIntValue("setupBaseDayNum");

            Date rejoinAbleDate = DateUtils.addDays(rtrmmbDate, setupBaseDayNum);
            logger.info("rejoinAbleDate:" + rejoinAbleDate);
            Date current = new Date();
            logger.info("current:" + current);
            if (current.getTime() < rejoinAbleDate.getTime()) {
                throw new ApiException("417", String.format(errMsgUtil.getErrMsg(context, "417"), setupBaseDayNum));
            }

            //update
            data.put("snsTypeCd", snsTypeCd);
            data.put("snsKey", snsKey);
            data.put("mbrSttusCd", "1");
            data.put("rtrmmbDate", "");
            data.put("sttusChgSbst", "재가입");
            int result = jdbcTemplate.update("update mbrInfo set mbrSttusCd='1',rtrmmbDate = '' where snsTypeCd=? and snsKey=? ", snsTypeCd, snsKey);
            //Node result = (Node) NodeUtils.getNodeService().executeNode(data, "mbrInfo", EventService.UPDATE);
            context.setResult(result);
        } catch (NotFoundNodeException e) {
            throw new ApiException("404", "Not Found Member");
        }

    }

    public void secession(ExecuteContext context) {
        if (jdbcTemplate == null) {
            jdbcTemplate = dbService.getJdbcTemplate("authDb");
        }

        Map<String, Object> data = context.getData();
        chkSnsParams(data);
        String snsKey = data.get("snsKey").toString();
        String snsTypeCd = data.get("snsTypeCd").toString();

        String chkMbrSql = "select  if(count(*)>0,'false','true') chkResult from mbrInfo where snsTypeCd = ? and snsKey = ?";
        Boolean chkResult = Boolean.parseBoolean(jdbcTemplate.queryForMap(chkMbrSql, snsTypeCd, snsKey).get("chkResult").toString());
        if (chkResult) {
            throw new ApiException("404", "Not Found");
        }


        Node anode = null;
        try {
            anode = getMbrNode(snsTypeCd, snsKey);

            String mbrSttusCd = anode.getStringValue("mbrSttusCd");
            if ("3".equals(mbrSttusCd)) {
                throw new ApiException("418", errMsgUtil.getErrMsg(context, "418"));
            }

            //update
            data.put("snsTypeCd", snsTypeCd);
            data.put("snsKey", snsKey);
            data.put("mbrSttusCd", "3");
            data.put("rtrmmbDate", new Date());
            data.put("sttusChgSbst", "회원 탈퇴");
            int result = jdbcTemplate.update("update mbrInfo set mbrSttusCd='3',rtrmmbDate = sysdate() where snsTypeCd=? and snsKey=? ", snsTypeCd, snsKey);
            //Node result = (Node) NodeUtils.getNodeService().executeNode(data, "mbrInfo", EventService.UPDATE);
            context.setResult(result);
        } catch (NotFoundNodeException e) {
            throw new ApiException("404", "Not Found Member");
        }
    }

    public void dormReles(ExecuteContext context) {
        if (jdbcTemplate == null) {
            jdbcTemplate = dbService.getJdbcTemplate("authDb");
        }

        Map<String, Object> data = context.getData();
        chkSnsParams(data);
        String snsKey = data.get("snsKey").toString();
        String snsTypeCd = data.get("snsTypeCd").toString();

        String chkMbrSql = "select  if(count(*)>0,'false','true') chkResult from mbrInfo where snsTypeCd = ? and snsKey = ?";
        Boolean chkResult = Boolean.parseBoolean(jdbcTemplate.queryForMap(chkMbrSql, snsTypeCd, snsKey).get("chkResult").toString());
        if (chkResult) {
            throw new ApiException("404", "Not Found");
        }

        if (data.get("infoOttpAgreeYn") == null || StringUtils.isEmpty(data.get("infoOttpAgreeYn").toString())) {
            throw new ApiException("400", "Required Parameter : infoOttpAgreeYn");
        }
        String infoOttpAgreeYn = data.get("infoOttpAgreeYn").toString();
        /*if(!"true".equals(infoOttpAgreeYn)){
            throw new ApiException("413", errMsgUtil.getErrMsg(context,"413"));
        }*/

        Node anode = null;
        try {
            anode = getMbrNode(snsTypeCd, snsKey);

            String mbrSttusCd = anode.getStringValue("mbrSttusCd");
            if ("1".equals(mbrSttusCd)) {
                throw new ApiException("411", errMsgUtil.getErrMsg(context, "411"));
            } else if ("3".equals(mbrSttusCd)) {
                throw new ApiException("418", errMsgUtil.getErrMsg(context, "418"));
            }

            //update
            data.put("snsTypeCd", snsTypeCd);
            data.put("snsKey", snsKey);
            data.put("mbrSttusCd", "1");
            data.put("dormTrtDate", "");
            data.put("sttusChgSbst", "휴면 해제");
            int result = jdbcTemplate.update("update mbrInfo set mbrSttusCd='1',dormTrtDate='',infoOttpAgreeYn=? where snsTypeCd=? and snsKey=? ", "true".equals(infoOttpAgreeYn) ? 1 : 0, snsTypeCd, snsKey);
            //Node result = (Node) NodeUtils.getNodeService().executeNode(data, "mbrInfo", EventService.UPDATE);
            context.setResult(result);
        } catch (NotFoundNodeException e) {
            throw new ApiException("404", "Not Found Member");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void mbrUpd(ExecuteContext context) {
        if (jdbcTemplate == null) {
            jdbcTemplate = dbService.getJdbcTemplate("authDb");
        }

        Map<String, Object> data = context.getData();
        chkSnsParams(data);
        String snsKey = data.get("snsKey").toString();
        String snsTypeCd = data.get("snsTypeCd").toString();

        String chkMbrSql = "select  if(count(*)>0,'false','true') chkResult from mbrInfo where snsTypeCd = ? and snsKey = ?";
        Boolean chkResult = Boolean.parseBoolean(jdbcTemplate.queryForMap(chkMbrSql, snsTypeCd, snsKey).get("chkResult").toString());
        if (chkResult) {
            throw new ApiException("404", "Not Found");
        }

        Node anode = null;
        try {
            anode = getMbrNode(snsTypeCd, snsKey);

            String mbrSttusCd = anode.getStringValue("mbrSttusCd");
            if ("2".equals(mbrSttusCd) || "3".equals(mbrSttusCd)) {
                throw new ApiException("412", errMsgUtil.getErrMsg(context, "412"));
            }
            String mbrDivCd = anode.getStringValue("mbrDivCd");
            if ("2".equals(mbrDivCd)) {
                throw new ApiException("411", errMsgUtil.getErrMsg(context, "411"));
            }
        } catch (NotFoundNodeException e) {
            throw new ApiException("404", "Not Found Member");
        }

        //국가코드 확인
        //chkCntryCd(data);
        //성별코드 확인
        //chkSexCd(data);


        NodeType nodeType = NodeUtils.getNodeType("mbrInfo");
        PropertyType imgUrlPt = nodeType.getPropertyType("imgUrl");
        FileValue file = null;
        String imgurl = "";
        if (data.get("imgUrl") != null) {
            file = (FileValue) NodeUtils.getStoreValue(data.get("imgUrl"), imgUrlPt, snsTypeCd + ">" + snsKey);
        } else {
            imgurl = data.get("imgUrl").toString();
            if ("_null_".equals(imgurl)) {
                imgurl = "";
            }
        }
        //update

        int result = jdbcTemplate.update("update mbrInfo set aliasNm=?,cntryCd=?,sexCd=?,imgUrl=?,bthYear=?,intrstCdList=?,email=?,infoOttpAgreeYn=?  where snsTypeCd=? and snsKey=? ", data.get("aliasNm"), data.get("cntryCd"), data.get("sexCd"), (file != null && file.getStorePath() != null ? file.getStorePath() : imgurl), data.get("bthYear"), data.get("intrstCdList"), data.get("email"), "true".equals(data.get("infoOttpAgreeYn")) ? 1 : 0, snsTypeCd, snsKey);
        //Node result = (Node) NodeUtils.getNodeService().executeNode(data, "mbrInfo", EventService.UPDATE);
        context.setResult(result);
    }

    public void mbrImgUpd(ExecuteContext context) {
        if (jdbcTemplate == null) {
            jdbcTemplate = dbService.getJdbcTemplate("authDb");
        }

        Map<String, Object> data = context.getData();
        chkSnsParams(data);
        String snsKey = data.get("snsKey").toString();
        String snsTypeCd = data.get("snsTypeCd").toString();

        String chkMbrSql = "select  if(count(*)>0,'false','true') chkResult from mbrInfo where snsTypeCd = ? and snsKey = ?";
        Boolean chkResult = Boolean.parseBoolean(jdbcTemplate.queryForMap(chkMbrSql, snsTypeCd, snsKey).get("chkResult").toString());
        if (chkResult) {
            throw new ApiException("404", "Not Found");
        }

        Node anode = null;
        try {
            anode = getMbrNode(snsTypeCd, snsKey);
        } catch (NotFoundNodeException e) {
            throw new ApiException("404", "Not Found Member");
        }

        NodeType nodeType = NodeUtils.getNodeType("mbrInfo");
        PropertyType imgUrlPt = nodeType.getPropertyType("imgUrl");
        FileValue file = null;
        String imgurl = "";

        if (data.get("imgUrl") != null && data.get("imgUrl") instanceof MultipartFile) {
            try {
                file = (FileValue) NodeUtils.getStoreValue(data.get("imgUrl"), imgUrlPt, snsTypeCd + ">" + snsKey);
            } catch (Exception e) {

            }
        } else {
            imgurl = data.get("imgUrl").toString();
            if ("_null_".equals(imgurl)) {
                imgurl = "";
            }
        }
        //update
        int result = jdbcTemplate.update("update mbrInfo set imgUrl=?  where snsTypeCd=? and snsKey=? ", (file != null && file.getStorePath() != null ? file.getStorePath() : imgurl), snsTypeCd, snsKey);
        //Node result = (Node) NodeUtils.getNodeService().executeNode(data, "mbrInfo", EventService.UPDATE);
        context.setResult(result);
    }

    private void chkSnsParams(Map<String, Object> data) {

        if (data.get("snsTypeCd") == null || StringUtils.isEmpty(data.get("snsTypeCd").toString())) {
            throw new ApiException("400", "Required Parameter : snsTypeCd");
        } else if (data.get("snsKey") == null || StringUtils.isEmpty(data.get("snsKey").toString())) {
            throw new ApiException("400", "Required Parameter :snsKey");
        }
    }

    private void chkCntryCd(Map<String, Object> data) {
        //국가코드 확인
        String paramCntryId = (data.get("cntryCd") != null) ? data.get("cntryCd").toString() : "";
        boolean chkCntry = false;
        if(cntryMngMap.contains(paramCntryId)){
            chkCntry = true;
        }

        if (!chkCntry) {
            logger.info("##chkCntryCd : paramCntryId="+paramCntryId+", cntryMngMap size="+cntryMngMap.size()+", chkCntry="+chkCntry);
            throw new ApiException("404", "Not Found Cntry");
        }
    }

    private void chkSexCd(Map<String, Object> data) {
        String paramSexCd = (data.get("sexCd") != null) ? data.get("sexCd").toString() : "";

        boolean chkSex = false;
        if(sexCdMap.contains(paramSexCd)){
            chkSex = true;
        }

        if (!chkSex) {
            logger.info("##chkSexCd : paramSexCd="+paramSexCd+", sexCdMap size="+sexCdMap.size()+", chkSex="+chkSex);
            throw new ApiException("404", "Not Found SexCd");
        }
    }

    private void chkSnsTypeCd(Map<String, Object> data) {
        String paramSnsTypeCd = (data.get("snsTypeCd") != null) ? data.get("snsTypeCd").toString() : "";

        boolean chkSnsType = false;
        if(snsTypeCdMap.contains(paramSnsTypeCd)){
            chkSnsType = true;
        }

        if (!chkSnsType) {
            logger.info("##chkSnsTypeCd : paramSnsTypeCd="+paramSnsTypeCd+", snsTypeCdMap size="+snsTypeCdMap.size()+", chkSnsType="+chkSnsType);
            throw new ApiException("404", "Not Found SnsTypeCd");
        }
    }
}
