package net.ion.ice.cjmwave.votePrtcptHst;


import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import net.ion.ice.core.api.ApiException;
import net.ion.ice.core.context.ContextUtils;
import net.ion.ice.core.context.ExecuteContext;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeService;
import net.ion.ice.core.node.NodeUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service("voteResponseService")
public class VoteResponseService {
    public static final String VOTE_BAS_INFO = "voteBasInfo";
    public static final String VOTE_ITEM_INFO = "voteItemInfo";
    public static final String SERS_VOTE_ITEM_INFO = "sersVoteItemInfo";

    @Autowired
    private NodeService nodeService;

    private JdbcTemplate jdbcTemplate;

    public void voteStatus(ExecuteContext context) {
        Map<String, Object> data = context.getData();

        String term = "voteSeq_matching={{:voteSeq}}&pstngStDt_above={{:concatStr(voteYear,0101)}}&pstngStDt_below={{:concatStr(voteYear,1231)}}"
                + "&showLoCd_matching={{:showLoCd}}&voteFormlCd_matching={{:voteFormlCd}}&showYn_matching=true&evVoteYn_matching=true"
                + "&sorting=voteSeq desc&limit=1";
        // 투표 basinfo 리스트 추출 다른 경우는 조건 없이 전체 추출 가능
        List<Node> voteInfoList = nodeService.getNodeList(VOTE_BAS_INFO, (String) ContextUtils.getValue(term, data));


        for (Node voteBasInfo : voteInfoList) {
            String voteItemTerms = "voteSeq_matching=" + voteBasInfo.getId() +
                    "&langCd_matching" + data.get("langCd");
            List<Node> voteItemList = nodeService.getNodeList(VOTE_ITEM_INFO, voteItemTerms);
            for (Node voteItem : voteItemList) {
                context.makeReferenceView("contsMetaId"); // referenceView 설정
                voteItem.toDisplay(context);
            }

            List<Node> sersVoteItemList = nodeService.getNodeList(SERS_VOTE_ITEM_INFO, voteItemTerms);
            for (Node voteItem : sersVoteItemList) {
                context.makeReferenceView("sersItemVoteSeq"); // referenceView 설정
                context.setIncludeReferenced(true);
                voteItem.toDisplay(context);
            }

            voteBasInfo.toDisplay(context);

            voteBasInfo.put("refdItemList", voteItemList);
            voteBasInfo.put("refdSeriesItemList", sersVoteItemList);

            // voteNum 계산
            Integer voteNum = 0;
            voteBasInfo.put("voteNum", voteNum);
        }

    }


    public void resIfMwv001(ExecuteContext context) {
        if (jdbcTemplate == null) {
            jdbcTemplate = NodeUtils.getNodeBindingService().getNodeBindingInfo(VOTE_BAS_INFO).getJdbcTemplate();
        }

        String term = "voteFormlCd_matching={{:voteFormlCd}}&showLoCd_matching={{:showLoCd}}&pstngStDt_below={{:conditionSysdate(progYn,true)}}"
                + "&pstngFnsDt_above={{:conditionSysdate(progYn,true)}}&pstngFnsDt_under={{:conditionSysdate(progYn,false)}}&showYn_matching=true&evVoteYn_matching=false"
                + "&voteNm_{{:langCd}}_wildcardShould={{:searchKeyword}}*&voteDesc_{{:langCd}}_wildcardShould={{:searchKeyword}}*"
                + "&findKywrd_matchingShould={{:searchKeyword}}&sorting={{:sorting}}&page={{:page}}&pageSize={{:pageSize}}&langCd_matching={{:langCd}}";

        Map<String, Object> data = context.getData();
        /*
        if (data.get("pageSize")==null || Integer.parseInt(data.get("pageSize").toString())<=0) {
            data.put("pageSize", 10);
        }
        */
        context.setIncludeReferenced(true);

        List<Node> voteInfoList = nodeService.getNodeList(VOTE_BAS_INFO, (String) ContextUtils.getValue(term, data));

        for (Node voteBasInfo : voteInfoList) {
            // TODO - voteSeq_referenceJoin 처리관련 문의 요청 필요
            String voteItemTerms = "voteSeq_matching=" + voteBasInfo.getId() +
                    "&langCd_matching=" + data.get("langCd");

            List<Node> refdItemList = nodeService.getNodeList(VOTE_ITEM_INFO, voteItemTerms);
            for (Node refdItem : refdItemList) {
                refdItem.toDisplay(context);
            }

            List<Node> refdSeriesItemList = nodeService.getNodeList(SERS_VOTE_ITEM_INFO, voteItemTerms);
            for (Node refdSeriesItem : refdSeriesItemList) {
                refdSeriesItem.toDisplay(context);
            }

            voteBasInfo.toDisplay(context);

            voteBasInfo.put("refdItemList", refdItemList);
            voteBasInfo.put("refdSeriesItemList", refdSeriesItemList);

            // voteNum 계산
            Integer voteNum = getVoteNum(voteBasInfo.getId());
            voteBasInfo.put("voteNum", (voteNum == null) ? 0 : voteNum);
        }

        data.put("items", voteInfoList);

        context.setResult(data);
    }


    public void resIfMwv002(ExecuteContext context) {
        if (jdbcTemplate == null) {
            jdbcTemplate = NodeUtils.getNodeBindingService().getNodeBindingInfo(VOTE_BAS_INFO).getJdbcTemplate();
        }

        String term = "voteSeq_matching={{:voteSeq}}&pstngStDt_above={{:concatStr(voteYear,0101)}}&pstngStDt_below={{:concatStr(voteYear,1231)}}"
                + "&showLoCd_matching={{:showLoCd}}&voteFormlCd_matching={{:voteFormlCd}}&showYn_matching=true&evVoteYn_matching=false"
                + "&sorting=voteSeq desc&limit=1";


        Map<String, Object> data = context.getData();

        List<Node> voteInfoList = nodeService.getNodeList(VOTE_BAS_INFO, (String) ContextUtils.getValue(term, data));

        for (Node voteBasInfo : voteInfoList) {

            String voteItemTerms = "voteSeq_matching=" + voteBasInfo.getId() +
                    "&langCd_matching=" + data.get("langCd");

            List<Node> voteItemList = nodeService.getNodeList(VOTE_ITEM_INFO, voteItemTerms);
            for (Node voteItem : voteItemList) {
                context.makeReferenceView("contsMetaId"); // referenceView 설정
                voteItem.toDisplay(context);
            }

            List<Node> sersVoteItemList = nodeService.getNodeList(SERS_VOTE_ITEM_INFO, voteItemTerms);
            for (Node voteItem : sersVoteItemList) {
                context.makeReferenceView("sersItemVoteSeq"); // referenceView 설정
                context.setIncludeReferenced(true);
                voteItem.toDisplay(context);
            }

            voteBasInfo.toDisplay(context);

            voteBasInfo.put("refdItemList", voteItemList);
            voteBasInfo.put("refdSeriesItemList", sersVoteItemList);

            // voteNum 계산
            Integer voteNum = getVoteNum(voteBasInfo.getId());
            voteBasInfo.put("voteNum", (voteNum == null) ? 0 : voteNum);
        }

        data.put("items", voteInfoList);

        context.setResult(data);
    }

    public void resIfMwv108(ExecuteContext context) {
        if (jdbcTemplate == null) {
            jdbcTemplate = NodeUtils.getNodeBindingService().getNodeBindingInfo(VOTE_BAS_INFO).getJdbcTemplate();
        }

        String term = "voteFormlCd_matching=2&showLoCd_matching=4&showYn_matching=true&evVoteYn_matching=false"
                + "&bradTelcsDt_above={{:concatStr(bradTelcsDt,000000)}}&bradTelcsDt_below={{:concatStr(bradTelcsDt,235959)}}&"
                + "&sorting=voteSeq desc&limit=1";

        Map<String, Object> data = context.getData();

        List<Node> voteInfoList = nodeService.getNodeList(VOTE_BAS_INFO, (String) ContextUtils.getValue(term, data));

        for (Node voteBasInfo : voteInfoList) {

            String voteItemTerms = "voteSeq_matching=" + voteBasInfo.getId() +
                    "&langCd_matching=" + data.get("langCd");

            List<Node> voteItemList = nodeService.getNodeList(VOTE_ITEM_INFO, voteItemTerms);
            for (Node voteItem : voteItemList) {
                context.makeReferenceView("contsMetaId"); // referenceView 설정
                voteItem.toDisplay(context);
            }

            List<Node> sersVoteItemList = nodeService.getNodeList(SERS_VOTE_ITEM_INFO, voteItemTerms);
            for (Node voteItem : sersVoteItemList) {
                context.makeReferenceView("sersItemVoteSeq"); // referenceView 설정
                context.setIncludeReferenced(true);
                voteItem.toDisplay(context);
            }

            voteBasInfo.toDisplay(context);

            voteBasInfo.put("refdItemList", voteItemList);
            voteBasInfo.put("refdSeriesItemList", sersVoteItemList);

            // voteNum 계산
            Integer voteNum = getVoteNum(voteBasInfo.getId());
            voteBasInfo.put("voteNum", (voteNum == null) ? 0 : voteNum);
        }

        data.put("items", voteInfoList);

        context.setResult(data);
    }

    private Integer getVoteNum(String voteSeq) {
        if (jdbcTemplate == null) {
            jdbcTemplate = NodeUtils.getNodeBindingService().getNodeBindingInfo(VOTE_BAS_INFO).getJdbcTemplate();
        }
        String voteNumSQL = "SELECT count(*) AS voteNum FROM " + voteSeq + "_voteHstByMbr";

        Map voteNumMap = jdbcTemplate.queryForMap(voteNumSQL);

        Integer retValue = voteNumMap.get("voteNum") == null ? 0 : Integer.parseInt(voteNumMap.get("voteNum").toString());
        return retValue;
    }

    public void resIfUlc002(ExecuteContext context) {
        if (jdbcTemplate == null) {
            jdbcTemplate = NodeUtils.getNodeBindingService().getNodeBindingInfo(VOTE_BAS_INFO).getJdbcTemplate();
        }
        Map<String, Object> data = context.getData();
        if (data.get("snsTypeCd") == null || StringUtils.isEmpty(data.get("snsTypeCd").toString())) {
            throw new ApiException("400", "Required Parameter : snsTypeCd");
        } else if (data.get("snsKey") == null || StringUtils.isEmpty(data.get("snsKey").toString())) {
            throw new ApiException("400", "Required Parameter :snsKey");
        }
        String snsTypeCd = data.get("snsTypeCd").toString();
        String snsKey = data.get("snsKey").toString();

        //전체 투표일련번호 조회
        String term = "sorting=voteSeq";
        List<Node> voteInfoList = nodeService.getNodeList(VOTE_BAS_INFO, (String) ContextUtils.getValue(term, data));
        List<Map<String, Object>> myVoteList = new ArrayList<>() ;
        for (Node voteInfo : voteInfoList) {
            String voteSeq = voteInfo.getId();
            String votedListSql = "select '" + voteSeq + "' as voteSeq, voteItemSeq, mbrId as prtcpMbrId, created from " + voteSeq + "_voteItemHstByMbr where mbrId ='" + snsTypeCd + ">" + snsKey + "' ";
            List<Map<String, Object>> votedList = jdbcTemplate.queryForList(votedListSql);

            //sorting을 해야되는데....

        }


//        Collections.sort(resultList, new Comparator<Map<String, Object>>() {
//            @Override
//            public int compare(Map<String, Object> o1, Map<String, Object> o2) {
//                return o1.get();
//            }
//        });

        //response는 paging,sorting 적용되어야 함
        //voteSeq에 referenceView 설정
        //voteItemSeq,prtcpMbrId(mbrId)는 reference유형으로.
        //created


    }
}
