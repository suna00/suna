package net.ion.ice.service;

import net.ion.ice.core.context.ExecuteContext;
import net.ion.ice.core.data.bind.NodeBindingService;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeService;
import net.ion.ice.core.node.NodeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service("couponService")
public class CouponService {
    public static final String CREATE = "create";
    public static final String UPDATE = "update";
    public static final String DELETE = "delete";
    public static final String ALL_EVENT = "allEvent";

    @Autowired
    private NodeService nodeService ;
    @Autowired
    private NodeBindingService nodeBindingService ;
    private CommonService common;


    public ExecuteContext download(ExecuteContext context) {
        Map<String, Object> data = new LinkedHashMap<>(context.getData());

        String[] params = { "memberNo","couponTypeId" };
        if (common.requiredParams(context, data, params)) return context;

        Node node = nodeService.getNode("couponType", data.get("couponTypeId").toString());
        if(node == null){
            common.setErrorMessage(context, "V0001");
            return context;
        }
        if("limit".equals(node.getValue("limitedQuantityType"))){
            if("0".equals(node.getValue("remainingQuantity"))){
                common.setErrorMessage(context, "V0002");
                return context;
            }
        }
        if("limit".equals(node.getValue("samePersonQuantityType"))){
            List<Node> list = nodeService.getNodeList("coupon", "memberNo_matching="+data.get("memberNo")+"&couponTypeId_matching="+data.get("couponTypeId"));
            if(list.size() > 0){
                Object count = node.getValue("samePersonQuantity");
            }
        }



        return context;
    }

}
