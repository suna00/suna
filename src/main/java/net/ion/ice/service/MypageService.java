package net.ion.ice.service;

import net.ion.ice.core.context.ExecuteContext;
import net.ion.ice.core.data.bind.NodeBindingInfo;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeService;
import net.ion.ice.core.node.NodeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;


@Service("mypageService")
public class MypageService {
    public static final String DELETE = "delete";
    @Autowired
    private NodeService nodeService;

//    관심상품 리스트 -> 품절상품 삭제
    public ExecuteContext removeOutOfStockProduct(ExecuteContext context){
        Map<String, Object> data = new LinkedHashMap<>(context.getData());

        String[] params = { "memberNo" };
        if (requiredParams(context, data, params)) return context;

        NodeBindingInfo nodeBindingInfo = NodeUtils.getNodeBindingInfo(context.getNodeType().getTypeId()) ;
        String query = " select a.*\n" +
                        "from interestproduct a, product b\n" +
                        "where b.productId = a.productId\n" +
                        "  and a.memberNo = ? \n" +
                        "  and b.stockQuantity = 0";
        List<Map<String, Object>> maps = nodeBindingInfo.getJdbcTemplate().queryForList(query, data.get("memberNo").toString());
        List<Map<String, Object>> list = new ArrayList<>();
        for(Map<String, Object> map : maps){
            nodeService.deleteNode("interestProduct", map.get("interestProductId").toString());
            list.add(map);
        }

        context.setResult(list);

        return context;

    }


//  POST 처리 필요 :  {{protocol}}://{{hostname}}:{{port}}/node/member/event/pwAuth.json?memberNo=77777&password=1234
//    마이페이지 회원정보 수정 비밀번호 인증
    public ExecuteContext passwordAuthentication(ExecuteContext context){
        Map<String, Object> data = new LinkedHashMap<>(context.getData());
        String[] params = { "memberNo", "password" };
        if (requiredParams(context, data, params)) return context;

        Node node = nodeService.read("member", data.get("memberNo").toString());
        String nodePw = node.getValue("password").toString();
        if(!nodePw.equals(data.get("password").toString())){
            Integer failedCount = (node.getValue("failedCount") == null ? 0 : Integer.parseInt(node.getValue("failedCount").toString())) + 1 ;
            node.put("failedCount", failedCount );
            node.put("lastFailedDate", new Date());
            nodeService.updateNode(node, "member");
            context.setResult("비밀번호 인증 실패 " + failedCount + "회");
        }else{
            context.setResult("비밀번호 인증 성공");
        }

        return context;
    }

    private boolean requiredParams(ExecuteContext context, Map<String, Object> data, String[] params) {
        for(String str : params){
            if(data.get(str) == null){
                context.setResult("No Param : " + str);
                return true;
            }
        }
        return false;
    }


}
