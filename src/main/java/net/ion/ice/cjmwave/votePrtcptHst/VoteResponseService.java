package net.ion.ice.cjmwave.votePrtcptHst;


import java.util.List;
import java.util.Map;

import net.ion.ice.core.context.ContextUtils;
import net.ion.ice.core.context.ExecuteContext;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("voteResponseService")
public class VoteResponseService {
    public static final String VOTE_BAS_INFO = "voteBasInfo";
    public static final String VOTE_ITEM_INFO = "voteItemInfo";
    public static final String SERS_VOTE_ITEM_INFO = "sersVoteItemInfo" ;

    @Autowired
    private NodeService nodeService ;

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
}
