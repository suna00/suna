package net.ion.ice.service;

import net.ion.ice.core.context.ExecuteContext;
import net.ion.ice.core.data.bind.NodeBindingInfo;
import net.ion.ice.core.node.NodeService;
import net.ion.ice.core.node.NodeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;



@Service("mypageService")
public class MypageService {
    public static final String DELETE = "delete";
    @Autowired
    private NodeService nodeService;

    public ExecuteContext removeOutOfStockProduct(ExecuteContext context){
        Map<String, Object> data = new LinkedHashMap<>(context.getData());

        if(data.get("memberNo") == null){
            context.setResult("No Param : memberNo");
            return context;
        }

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


}
