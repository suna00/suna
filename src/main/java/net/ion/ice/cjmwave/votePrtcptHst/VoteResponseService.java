package net.ion.ice.cjmwave.votePrtcptHst;


import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import net.ion.ice.ApplicationContextManager;
import net.ion.ice.core.api.ApiException;
import net.ion.ice.core.context.ContextUtils;
import net.ion.ice.core.context.ExecuteContext;
import net.ion.ice.core.context.QueryContext;
import net.ion.ice.core.infinispan.InfinispanRepositoryService;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeService;
import net.ion.ice.core.node.NodeType;
import net.ion.ice.core.node.NodeUtils;
import net.ion.ice.core.query.QueryResult;
import net.ion.ice.core.query.QueryTerm;
import net.ion.ice.core.query.QueryUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;
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
    static InfinispanRepositoryService infinispanRepositoryService;

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
        if (infinispanRepositoryService == null) {
            infinispanRepositoryService = ApplicationContextManager.getBean(InfinispanRepositoryService.class);
        }

        String term = "voteFormlCd_matching={{:voteFormlCd}}&showLoCd_matching={{:showLoCd}}&pstngStDt_below={{:conditionSysdate(progYn,true)}}"
                + "&pstngFnsDt_above={{:conditionSysdate(progYn,true)}}&pstngFnsDt_under={{:conditionSysdate(progYn,false)}}&showYn_matching=true&evVoteYn_matching=false"
                + "&voteNm_{{:langCd}}_wildcardShould={{:searchKeyword}}*&voteDesc_{{:langCd}}_wildcardShould={{:searchKeyword}}*"
                + "&findKywrd_matchingShould={{:searchKeyword}}&sorting={{:sorting}}&page={{:page}}&pageSize={{:pageSize}}&langCd_matching={{:langCd}}";
        try {
            Map<String, Object> data = context.getData();
            if (data.get("pageSize") == null || Integer.parseInt(data.get("pageSize").toString()) <= 0) {
                data.put("pageSize", 10);
            }
            if (data.get("page") == null || Integer.parseInt(data.get("page").toString()) <= 0) {
                data.put("page", 1);
            }

            context.setIncludeReferenced(true);

            NodeType voteBasInfoNodeType = NodeUtils.getNodeType(VOTE_BAS_INFO);
            //List<Node> voteInfoList = nodeService.getNodeList(VOTE_BAS_INFO, (String) ContextUtils.getValue(term, data));

            Object values = ContextUtils.getValue(term, data);
            //System.out.println("#####values :" + values);
            QueryContext queryContext = QueryContext.createQueryContextFromText((String) values, voteBasInfoNodeType, null);
            List<Node> voteInfoList = infinispanRepositoryService.getSubQueryNodes(voteBasInfoNodeType.getTypeId(), queryContext);

            QueryResult queryResult = new QueryResult();
            queryResult.put("totalCount", queryContext.getResultSize());
            queryResult.put("resultCount", voteInfoList.size());
            queryResult.put("pageSize", queryContext.getPageSize());
            queryResult.put("pageCount", queryContext.getResultSize() / queryContext.getPageSize() + (queryContext.getResultSize() % queryContext.getPageSize() > 0 ? 1 : 0));
            queryResult.put("currentPage", queryContext.getCurrentPage());

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

            //data.put("items", voteInfoList);
            queryResult.put("items", voteInfoList);
            context.setResult(queryResult);
        } catch (Exception e) {
            e.printStackTrace();
        }

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

        String mbrId = "";
        if (data.get("snsTypeCd") != null || data.get("snsKey") != null) {
            mbrId = data.get("snsTypeCd").toString() + ">" + data.get("snsKey").toString();
        }

        Date now = new Date();
        String connIpAdr = data.get("connIpAdr").toString();
        String voteDate = DateFormatUtils.format(now, "yyyyMMdd");

        // Checking Available IP with mbrId and voteSeq
        String searchText = "setupTypeCd_matching=2&sorting=dclaSetupSeq desc&limit=1";
        List<Node> dclaNodeList = nodeService.getNodeList("dclaSetupMng", searchText);
        Node dclaNode = dclaNodeList.get(0);
        Integer ipDclaCnt = dclaNode.getIntValue("setupBaseCnt");
        Integer ipCnt = getIpCnt(connIpAdr, voteDate);

        Integer ipAdrVoteCnt = ipDclaCnt - ipCnt;

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

            // userVoteCnt
            Integer userVoteCnt = 0;
            if (mbrId != null && mbrId.length() > 0) {
                userVoteCnt = getUserVoteCnt(voteBasInfo.getId(), mbrId);
            }
            voteBasInfo.put("userVoteCnt", (userVoteCnt == null) ? 0 : userVoteCnt);
            voteBasInfo.put("userPvCnt", 0);
            voteBasInfo.put("ipAdrVoteCnt", ipAdrVoteCnt);
        }

        data.put("items", voteInfoList);

        context.setResult(data);
    }

    private Integer getUserVoteCnt(String voteSeq, String mbrId) {
        if (jdbcTemplate == null) {
            jdbcTemplate = NodeUtils.getNodeBindingService().getNodeBindingInfo(VOTE_BAS_INFO).getJdbcTemplate();
        }
        String voteNumSQL = "SELECT count(*) AS userVoteCnt FROM " + voteSeq + "_voteHstByMbr WHERE mbrId=?";

        Map voteNumMap = jdbcTemplate.queryForMap(voteNumSQL, mbrId);

        Integer retValue = voteNumMap.get("userVoteCnt") == null ? 0 : Integer.parseInt(voteNumMap.get("userVoteCnt").toString());
        return retValue;
    }


    // 접근 IP Count 조회
    private Integer getIpCnt(String connIpAdr, String voteDate) {

        String selectIpDclaCnt = "SELECT count(*) ipCnt FROM voteHstByIp WHERE ipAdr=? AND voteDate=?";
        Map<String, Object> ipCntMap = jdbcTemplate.queryForMap(selectIpDclaCnt, connIpAdr, voteDate);
        Integer mbrIpDclaCnt = Integer.parseInt(ipCntMap.get("ipCnt").toString());

        return mbrIpDclaCnt;
    }

    public void resIfMwv108(ExecuteContext context) {
        if (jdbcTemplate == null) {
            jdbcTemplate = NodeUtils.getNodeBindingService().getNodeBindingInfo(VOTE_BAS_INFO).getJdbcTemplate();
        }
        if (infinispanRepositoryService == null) {
            infinispanRepositoryService = ApplicationContextManager.getBean(InfinispanRepositoryService.class);
        }

        String term = "voteFormlCd_matching=2&showLoCd_matching=4&showYn_matching=true&evVoteYn_matching=false"
                + "&bradTelcsDt_above={{:concatStr(bradTelcsDt,000000)}}&bradTelcsDt_below={{:concatStr(bradTelcsDt,235959)}}&"
                + "&sorting=voteSeq desc&limit=1";
        try {
            Map<String, Object> data = context.getData();
            if (data.get("pageSize") == null || Integer.parseInt(data.get("pageSize").toString()) <= 0) {
                data.put("pageSize", 10);
            }
            if (data.get("page") == null || Integer.parseInt(data.get("page").toString()) <= 0) {
                data.put("page", 1);
            }

            context.setIncludeReferenced(true);

            NodeType voteBasInfoNodeType = NodeUtils.getNodeType(VOTE_BAS_INFO);
            //List<Node> voteInfoList = nodeService.getNodeList(VOTE_BAS_INFO, (String) ContextUtils.getValue(term, data));

            Object values = ContextUtils.getValue(term, data);
            //System.out.println("#####values :" + values);
            QueryContext queryContext = QueryContext.createQueryContextFromText((String) values, voteBasInfoNodeType, null);
            List<Node> voteInfoList = infinispanRepositoryService.getSubQueryNodes(voteBasInfoNodeType.getTypeId(), queryContext);

            QueryResult queryResult = new QueryResult();
            queryResult.put("totalCount", queryContext.getResultSize());
            queryResult.put("resultCount", voteInfoList.size());

            String mbrId = "";
            if (data.get("snsTypeCd") != null || data.get("snsKey") != null) {
                mbrId = data.get("snsTypeCd").toString() + ">" + data.get("snsKey").toString();
            }

            Date now = new Date();
            String connIpAdr = data.get("connIpAdr").toString();
            String voteDate = DateFormatUtils.format(now, "yyyyMMdd");

            // Checking Available IP with mbrId and voteSeq
            String searchText = "setupTypeCd_matching=2&sorting=dclaSetupSeq desc&limit=1";
            List<Node> dclaNodeList = nodeService.getNodeList("dclaSetupMng", searchText);
            Node dclaNode = dclaNodeList.get(0);
            Integer ipDclaCnt = dclaNode.getIntValue("setupBaseCnt");
            Integer ipCnt = getIpCnt(connIpAdr, voteDate);

            Integer ipAdrVoteCnt = ipDclaCnt - ipCnt;

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

                // userVoteCnt
                Integer userVoteCnt = 0;
                if (mbrId != null && mbrId.length() > 0) {
                    userVoteCnt = getUserVoteCnt(voteBasInfo.getId(), mbrId);
                }
                voteBasInfo.put("userVoteCnt", (userVoteCnt == null) ? 0 : userVoteCnt);
                voteBasInfo.put("userPvCnt", 0);
                voteBasInfo.put("ipAdrVoteCnt", ipAdrVoteCnt);
            }

            queryResult.put("items", voteInfoList);
            context.setResult(queryResult);
        } catch (Exception e) {
            e.printStackTrace();
        }
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
        Map<String, Object> result = new LinkedHashMap<>();
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
        List<Map<String, Object>> myVoteList = new ArrayList<>();
        for (Node voteInfo : voteInfoList) {
            //투표별 투표내역 조회해서 result에 합쳐야함
            String voteSeq = voteInfo.getId();
            String votedListSql = "select mbrId, count(*) as voteNum, max(created) as created from " + voteSeq + "_voteHstByMbr where mbrId ='" + snsTypeCd + ">" + snsKey + "' group by mbrId ";

            List<Map<String, Object>> votedList = jdbcTemplate.queryForList(votedListSql);
            if (votedList != null && votedList.size() > 0) {
                Map<String, Object> item = new LinkedHashMap<>();
                //voteSeq response 형식 만들기.
                voteInfo.toDisplay(context);
                Map<String, Object> voteBasInfo = new LinkedHashMap<>();
                voteBasInfo.put("value", voteInfo.getStringValue("voteSeq"));
                voteBasInfo.put("label", voteInfo.getStringValue("voteNm"));
                voteBasInfo.put("refId", voteInfo.getStringValue("voteSeq"));
                voteBasInfo.put("item", voteInfo);

                Map<String, Object> votedresult = votedList.get(0);

                item.put("voteSeq", voteBasInfo);
                //item.put("createdSort", votedresult.get("created"));
                Object createdObj = votedresult.get("created");
                if (createdObj instanceof Date) {
                    Date createdDate = (Date) votedresult.get("created");
                    item.put("created", DateFormatUtils.format(createdDate, "yyyy-MM-dd HH:mm:ss"));
                } else {
                    item.put("created", votedresult.get("created"));
                }

                myVoteList.add(item);
            }

        }
        //response는 paging,sorting=default로 created desc로 적용되어야 함
        Collections.sort(myVoteList, new CompareCreatedDesc());

        //paging
        int totalCount = myVoteList.size();
        int pageSize = 10;
        int page = 1;
        if (data.get("pageSize") != null) {
            int paramPageSize = Integer.parseInt(data.get("pageSize").toString());
            pageSize = paramPageSize;
        }
        if (data.get("page") != null) {
            int paramPage = Integer.parseInt(data.get("page").toString());
            page = paramPage;
        }
        int pageCount = totalCount / pageSize + (totalCount % pageSize > 0 ? 1 : 0);
        List<Map<String, Object>> myVotePagingList = new ArrayList<>();
        for (int i = (pageSize * (page - 1)); i < (page * pageSize); i++) {
            if (i <= (myVoteList.size() - 1)) {
                myVotePagingList.add(myVoteList.get(i));
            }
        }

        //voteSeq에 referenceView 설정
        //created
        result.put("totalCount", totalCount);
        result.put("resultCount", myVotePagingList.size());
        result.put("pageSize", pageSize);
        result.put("pageCount", pageCount);
        result.put("currentPage", page);
        result.put("items", myVotePagingList);

        context.setResult(result);
    }

    /**
     * pid로 내림차순(Desc) 정
     */
    static class CompareCreatedDesc implements Comparator<Map<String, Object>> {
        @Override
        public int compare(Map<String, Object> o1, Map<String, Object> o2) {
            Date o1Obj = null;
            Date o2Obj = null;
            try {
                o1Obj = DateUtils.parseDate(o1.get("created").toString(), "yyyy-MM-dd HH:mm:ss");
                o2Obj = DateUtils.parseDate(o2.get("created").toString(), "yyyy-MM-dd HH:mm:ss");
            } catch (Exception e) {
                e.printStackTrace();
            }
            return o2Obj.compareTo(o1Obj);
        }
    }

    /**
     * pid로 오름차순(Asc) 정렬
     */
    static class CompareCreatedAsc implements Comparator<Map<String, Object>> {
        @Override
        public int compare(Map<String, Object> o1, Map<String, Object> o2) {
            Date o1Obj = null;
            Date o2Obj = null;
            try {
                o1Obj = DateUtils.parseDate(o1.get("created").toString(), "yyyy-MM-dd HH:mm:ss");
                o2Obj = DateUtils.parseDate(o2.get("created").toString(), "yyyy-MM-dd HH:mm:ss");
            } catch (Exception e) {
                e.printStackTrace();
            }
            return o1Obj.compareTo(o2Obj);
        }
    }
}
