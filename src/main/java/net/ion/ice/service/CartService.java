package net.ion.ice.service;

import net.ion.ice.core.context.ExecuteContext;
import net.ion.ice.core.data.bind.NodeBindingInfo;
import net.ion.ice.core.data.bind.NodeBindingService;
import net.ion.ice.core.json.JsonUtils;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeService;
import net.ion.ice.core.node.NodeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service("cartService")
public class CartService {

    public static final String CREATE = "create";
    public static final String UPDATE = "update";
    public static final String DELETE = "delete";
    public static final String ALL_EVENT = "allEvent";

    @Autowired
    private NodeService nodeService ;

    public void addCart(ExecuteContext context) throws IOException {
        Map<String, Object> data = context.getData();
        Object cartId = data.get("cartId");
        Node node = null;

        if(cartId != null){
            node = NodeUtils.getNode("cart", cartId.toString());
        }

        if(node == null){
            node = nodeService.executeNode(data, "cart", CREATE) ;
            data.put("cartId", node.getId());
            List<Node> baseOptionlist = createList(data,"cartProduct");
            data.put("cartProductId", baseOptionlist.get(0).getId());
            List<Node> addOptionList = createList(data,"cartProductItem");



//            nodeBindingService.save(data, context.getNodeType().getTypeId());

//            NodeBindingInfo nodeBindingInfo = NodeUtils.getNodeBindingInfo(context.getNodeType().getTypeId()) ;
//            nodeBindingInfo.getJdbcTemplate().update();



//            node = nodeService.executeNode(data, "cart", CREATE) ;
//            data.put("cartId", node.getId());
//            createList(data,"cartProduct");
        }else{
            node = nodeService.executeNode(data, "cart", UPDATE) ;

        }

        try {

        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    private List<Node> createList(Map<String, Object> data, String tid) throws IOException {
        List<Node> list = new ArrayList<>();
        for(Map<String, Object> map : JsonUtils.parsingJsonToList(data.get(tid).toString())){
            map.putAll(data);
            Node node = nodeService.executeNode(map, tid, CREATE) ;
            list.add(node);
        }
        return list;
    }

    public void update(ExecuteContext context){
        Node node = context.getNode() ;

    }

    public void delete(ExecuteContext context){
        Node node = context.getNode() ;

    }

}
