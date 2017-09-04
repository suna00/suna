package net.ion.ice.service;

import net.ion.ice.core.context.ExecuteContext;
import net.ion.ice.core.context.QueryContext;
import net.ion.ice.core.data.bind.NodeBindingInfo;
import net.ion.ice.core.data.bind.NodeBindingService;
import net.ion.ice.core.json.JsonUtils;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeService;
import net.ion.ice.core.node.NodeType;
import net.ion.ice.core.node.NodeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static net.ion.ice.core.node.NodeUtils.getNodeBindingService;

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
            newCart(data);
        }else{
            nodeService.executeNode(data, "cart", UPDATE) ;
            mergeList(data, cartId, "cartProduct");
        }

    }

    private void mergeList(Map<String, Object> data, Object cartId, String tid) throws IOException {
        List<Map<String, Object>> referenced = getList(tid, "cartId_matching="+cartId);
        List<Map<String, Object>> maps = JsonUtils.parsingJsonToList(data.get(tid).toString());

        boolean exist = false;
        for(Map<String, Object> map : maps){
            for(Map<String, Object> obj : referenced){
                if((map.get("baseOptionItemId")).equals(obj.get("baseOptionItemId"))){
                    exist = true;
                }
            }
            if(!exist){
                map.putAll(data);
                nodeService.executeNode(map, tid, CREATE) ;
            }else{
                // 장바구니에 이미 존재하는 상품입니다
            }
        }

    }

//    "nodeType=data" getList
    private List<Map<String, Object>> getList(String tid, String searchText) {
        NodeType nodeType = NodeUtils.getNodeType(tid);
        QueryContext queryContext = QueryContext.createQueryContextFromText(searchText, nodeType);
        return getNodeBindingService().getNodeBindingInfo(nodeType.getTypeId()).list(queryContext);
    }

    private void newCart(Map<String, Object> data) throws IOException {
        Node node = nodeService.executeNode(data, "cart", CREATE) ;
        data.put("cartId", node.getId());

        createList(data);

    }


    private List<Node> createList(Map<String, Object> data) throws IOException {
        List<Node> list = new ArrayList<>();
        for(Map<String, Object> map : JsonUtils.parsingJsonToList(data.get("cartProduct").toString())){
            map.putAll(data);
            Node node = nodeService.executeNode(map, "cartProduct", CREATE) ;
            map.put("cartProductId", node.getId());

            if(map.get("cartProductItem") != null){
                for(Map<String, Object> item : (List<Map<String, Object>>) map.get("cartProductItem")){
                    item.putAll(map);
                    nodeService.executeNode(item, "cartProductItem", CREATE) ;
                }
            }
            list.add(node);
        }
        return list;
    }

    public void setDeliveryPrice(){

    }

    public void update(ExecuteContext context){
        Node node = context.getNode() ;

    }

    public void delete(ExecuteContext context){
        Node node = context.getNode() ;

    }

}
