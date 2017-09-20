package net.ion.ice.service;

import net.ion.ice.core.context.ExecuteContext;
import net.ion.ice.core.data.bind.NodeBindingService;
import net.ion.ice.core.json.JsonUtils;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeService;
import net.ion.ice.core.node.NodeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service("cartService")
public class CartService {
    private static Logger logger = LoggerFactory.getLogger(CartService.class);

    public static final String CREATE = "create";
    public static final String UPDATE = "update";
    public static final String DELETE = "delete";
    public static final String ALL_EVENT = "allEvent";

    @Autowired
    private NodeService nodeService ;
    @Autowired
    private NodeBindingService nodeBindingService ;

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

    public void removeProduct(ExecuteContext context) {
        Map<String, Object> data = context.getData();
        String productIds = (String) data.get("productIds");
        String[] productIdsArray = productIds.split(",");
        for(String productId : productIdsArray){
            nodeBindingService.delete("cartProduct", productId);
            List<Map<String, Object>> cartProductItemList = nodeBindingService.list("cartProductItem", "cartProductId_in=".concat(productId));
            if(cartProductItemList.size() > 0){
                for(Map<String, Object> cartProductItem : cartProductItemList){
                    nodeBindingService.delete("cartProductItem", String.valueOf(cartProductItem.get("cartProductItemId")));
                }
            }
        }
        context.setResult(CommonService.getResult("C0001"));
    }

    private void mergeList(Map<String, Object> data, Object cartId, String tid) throws IOException {
        List<Map<String, Object>> referenced = nodeBindingService.list(tid, "cartId_in="+cartId);
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
//
////    "nodeType=data" getList
//    private List<Map<String, Object>> getList(String tid, String searchText) {
//        NodeType nodeType = NodeUtils.getNodeType(tid);
//        QueryContext queryContext = QueryContext.createQueryContextFromText(searchText, nodeType);
//        return getNodeBindingService().getNodeBindingInfo(nodeType.getTypeId()).list(queryContext);
//    }

    private void newCart(Map<String, Object> data) throws IOException {
        Node node = (Node) nodeService.executeNode(data, "cart", CREATE);
        data.put("cartId", node.getId());

        createList(data);

    }


    private List<Node> createList(Map<String, Object> data) throws IOException {
        List<Node> list = new ArrayList<>();
        for(Map<String, Object> map : JsonUtils.parsingJsonToList(data.get("cartProduct").toString())){
            map.putAll(data);
            Node node = (Node) nodeService.executeNode(map, "cartProduct", CREATE);
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
