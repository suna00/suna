package net.ion.ice.cjmwave.votePrtcptHst;


import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.ion.ice.core.context.ContextUtils;
import net.ion.ice.core.context.ExecuteContext;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeService;
import net.ion.ice.core.node.NodeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service("voteResponseService")
public class VoteResponseService {
    public static final String VOTE_BAS_INFO = "voteBasInfo";
    public static final String VOTE_ITEM_INFO = "voteItemInfo";
    public static final String SERS_VOTE_ITEM_INFO = "sersVoteItemInfo" ;

    @Autowired
    private NodeService nodeService ;

    private JdbcTemplate jdbcTemplate ;

    public void voteStatus(ExecuteContext context) {
        Map<String, Object> data = context.getData() ;

        String term = "voteSeq_matching={{:voteSeq}}&pstngStDt_above={{:concatStr(voteYear,0101)}}&pstngStDt_below={{:concatStr(voteYear,1231)}}"
                + "&showLoCd_matching={{:showLoCd}}&voteFormlCd_matching={{:voteFormlCd}}&showYn_matching=true&evVoteYn_matching=true"
                + "&sorting=voteSeq desc&limit=1";
        // 투표 basinfo 리스트 추출 다른 경우는 조건 없이 전체 추출 가능
        List<Node> voteInfoList = nodeService.getNodeList(VOTE_BAS_INFO, (String) ContextUtils.getValue(term, data)) ;


        for(Node voteBasInfo : voteInfoList){
            String voteItemTerms = "voteSeq_matching=" + voteBasInfo.getId() +
                    "&langCd_matching" + data.get("langCd") ;
            List<Node> voteItemList = nodeService.getNodeList(VOTE_ITEM_INFO, voteItemTerms) ;
            for(Node voteItem : voteItemList){
                context.makeReferenceView("contsMetaId"); // referenceView 설정
                voteItem.toDisplay(context) ;
            }

            List<Node> sersVoteItemList = nodeService.getNodeList(SERS_VOTE_ITEM_INFO, voteItemTerms) ;
            for(Node voteItem : sersVoteItemList){
                context.makeReferenceView("sersItemVoteSeq"); // referenceView 설정
                context.setIncludeReferenced(true);
                voteItem.toDisplay(context) ;
            }

            voteBasInfo.toDisplay(context) ;

            voteBasInfo.put("refdItemList", voteItemList) ;
            voteBasInfo.put("refdSeriesItemList", voteItemList) ;

            // voteNum 계산
            Integer voteNum = 0 ;
            voteBasInfo.put("voteNum", voteNum) ;
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

        Map<String, Object> data = context.getData() ;
        /*
        if (data.get("pageSize")==null || Integer.parseInt(data.get("pageSize").toString())<=0) {
            data.put("pageSize", 10);
        }
        */
        context.setIncludeReferenced(true);

        List<Node> voteInfoList = nodeService.getNodeList(VOTE_BAS_INFO, (String) ContextUtils.getValue(term, data)) ;

        for(Node voteBasInfo : voteInfoList){
            // TODO - voteSeq_referenceJoin 처리관련 문의 요청 필요
            String voteItemTerms = "voteSeq_matching=" + voteBasInfo.getId() +
                    "&langCd_matching=" + data.get("langCd") ;

            List<Node> refdItemList = nodeService.getNodeList(VOTE_ITEM_INFO, voteItemTerms) ;
            for(Node refdItem : refdItemList){
                refdItem.toDisplay(context) ;
            }

            List<Node> refdSeriesItemList = nodeService.getNodeList(SERS_VOTE_ITEM_INFO, voteItemTerms);
            for(Node refdSeriesItem : refdSeriesItemList){
                refdSeriesItem.toDisplay(context) ;
            }

            voteBasInfo.toDisplay(context) ;

            voteBasInfo.put("refdItemList", refdItemList);
            voteBasInfo.put("refdSeriesItemList", refdSeriesItemList);

            // voteNum 계산
            Integer voteNum = getVoteNum(voteBasInfo.getId());
            voteBasInfo.put("voteNum", (voteNum==null) ? 0 : voteNum);
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


        Map<String, Object> data = context.getData() ;

        List<Node> voteInfoList = nodeService.getNodeList(VOTE_BAS_INFO, (String) ContextUtils.getValue(term, data)) ;

        for(Node voteBasInfo : voteInfoList){

            String voteItemTerms = "voteSeq_matching=" + voteBasInfo.getId() +
                                    "&langCd_matching=" + data.get("langCd") ;

            List<Node> voteItemList = nodeService.getNodeList(VOTE_ITEM_INFO, voteItemTerms) ;
            for(Node voteItem : voteItemList){
                context.makeReferenceView("contsMetaId"); // referenceView 설정
                voteItem.toDisplay(context) ;
            }

            List<Node> sersVoteItemList = nodeService.getNodeList(SERS_VOTE_ITEM_INFO, voteItemTerms) ;
            for(Node voteItem : sersVoteItemList){
                context.makeReferenceView("sersItemVoteSeq"); // referenceView 설정
                context.setIncludeReferenced(true);
                voteItem.toDisplay(context) ;
            }

            voteBasInfo.toDisplay(context) ;

            voteBasInfo.put("refdItemList", voteItemList) ;
            voteBasInfo.put("refdSeriesItemList", voteItemList) ;

            // voteNum 계산
            Integer voteNum = getVoteNum(voteBasInfo.getId());
            voteBasInfo.put("voteNum", (voteNum==null) ? 0 : voteNum);
        }

        data.put("items", voteInfoList);

        context.setResult(data);
    }

    private Integer getVoteNum(String voteSeq) {
        if (jdbcTemplate == null) {
            jdbcTemplate = NodeUtils.getNodeBindingService().getNodeBindingInfo(VOTE_BAS_INFO).getJdbcTemplate();
        }
        String voteNumSQL = "SELECT sum(voteNum) AS voteNum FROM voteItemStats WHERE voteSeq=?";
        Map voteNumMap = jdbcTemplate.queryForMap(voteNumSQL, voteSeq);

        Integer retValue = voteNumMap.get("voteNum")==null ? 0 : Integer.parseInt(voteNumMap.get("voteNum").toString());
        return retValue;
    }

}
