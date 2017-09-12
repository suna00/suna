package net.ion.ice.service;

import net.ion.ice.core.context.ExecuteContext;
import net.ion.ice.core.data.bind.NodeBindingService;
import net.ion.ice.core.json.JsonUtils;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeService;
import net.ion.ice.core.node.NodeUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.DELETE;
import java.io.IOException;
import java.util.*;

@Service("productService")
public class ProductService {
    public static final String CREATE = "create";
    public static final String UPDATE = "update";
    public static final String DELETE = "delete";
    public static final String SAVE = "save";
    private Logger logger = Logger.getLogger(ProductService.class);

    @Autowired
    private NodeService nodeService ;
    @Autowired
    private NodeBindingService nodeBindingService ;

    public void make(ExecuteContext context) {
        Map<String, Object> data = new LinkedHashMap<>(context.getData());

        try{
            nodeService.executeNode(data, "product", SAVE) ;
            List<Node> baseOptions = productOptions(data, "baseOption");
            List<Node> addOptions = productOptions(data, "addOption");
            productOptionItems(data, baseOptions, "baseOption");
            productOptionItems(data, addOptions, "addOption");
            productAttribute(data);
            productToCategoryMap(data);
            productSearchFilter(data);

        } catch (Exception e){
            e.printStackTrace();
        }
    }


    public List<Node> productOptions(Map<String, Object> data, String type) throws IOException {
        List<Node> list = new ArrayList<>();
        List<Map<String, Object>> maps = JsonUtils.parsingJsonToList(data.get(type).toString());

        if(data.get(type) == null || data.get(type + "Item") == null) return list;

        String searchText = "";
        if("baseOption".equals(type)){
            searchText = "productId_matching="+data.get("productId")+"&productOptionType_notMatching=add";
        }else{
            searchText = "productId_matching="+data.get("productId")+"&productOptionType_matching=add";
        }
        List<Node> nodeList = NodeUtils.getNodeList("productOption", searchText);

        for(Node node : nodeList){
            Boolean exist = false;
            for(Map<String, Object> map : maps){
                String name = map.get("name").toString();
                if(name.equals(node.getValue("name"))){
                    exist = true;
                }
            }
            if(!exist){
                node.put("productOptionStatus", "n");
                nodeService.executeNode(node, "productOption", UPDATE);
            }
        }

        for(Map<String, Object> map : maps) {

            map = setMap(data, map);
            if (type.equals("baseOption")) {
                map.put("productOptionType", map.get("baseOptionType"));
                map.put("required", "true");
            } else {
                map.put("productOptionType", "add");
            }

            List<Node> nodes = NodeUtils.getNodeList("productOption", "productId_matching="+data.get("productId")+"&name_matching="+map.get("name").toString());
            if(nodes.size() > 0){
                map.put("productOptionId", nodes.get(0).getId());
            }
            Node node = (Node) nodeService.executeNode(map, "productOption", SAVE);

            list.add(node);
        }
        return list;
    }

    private Map<String, Object>  setMap(Map<String, Object> data, Map<String, Object> map) {
        Map<String, Object> m = new LinkedHashMap<>(data);
        m.putAll(map);
        return m;
    }

    private List<Node> productOptionItems(Map<String, Object> data, List<Node> options, String type) throws IOException {
        List<Node> list = new ArrayList<>();
        List<Map<String, Object>> maps = JsonUtils.parsingJsonToList(data.get(type + "Item").toString());
        if(data.get(type) == null || data.get(type + "Item") == null) return list;

        String searchText = "";
        if("baseOption".equals(type)){
            searchText = "productId_matching="+data.get("productId")+"&productOptionType_notMatching=add";
        }else{
            searchText = "productId_matching="+data.get("productId")+"&productOptionType_matching=add";
        }

        List<Node> nodeList = NodeUtils.getNodeList("productOptionItem", searchText);
        for(Node node : nodeList){
            Boolean exist = false;
            for(Map<String, Object> map : maps){
                String name = map.get("name").toString();
                if(name.equals(node.getValue("name"))){
                    exist = true;
                }
            }
            if(!exist){
                node.put("productOptionItemStatus", "n");
                nodeService.executeNode(node, "productOptionItem", UPDATE);
            }
        }

        for(Map<String, Object> item : maps){

            List<Node> nodes = NodeUtils.getNodeList("productOptionItem", "productId_matching="+data.get("productId")+"&productOptionCodeCase_matching="+item.get("productOptionCodeCase").toString());
            if(nodes.size() > 0){
                item.put("productOptionItemId", nodes.get(0).getId());
            }

            item = setMap(data, item);
            item.put("productOptionType", ("baseOption".equals(type) ? options.get(0).get("productOptionType").toString() : "add"));
            item.put("productOptionCodeCase", item.get("productOptionCodeCase").toString());

            Node node = (Node) nodeService.executeNode(item, "productOptionItem", SAVE);
            list.add(node);
        }

        return list;
    }

    private List<Node> productAttribute(Map<String, Object> data) throws IOException {
        List<Node> list = new ArrayList<>();
        if(data.get("productAttributeCategoryId") == null || data.get("productAttribute") == null) return list;

        List<Map<String, Object>> maps = JsonUtils.parsingJsonToList(data.get("productAttribute").toString());
        for(Map<String, Object> map : maps){
            map = setMap(data, map);

            Node node = (Node) nodeService.executeNode(map, "productAttribute", SAVE);
            list.add(node);
        }

//        nodes 검색에 문제가 있는듯
//        List<Node> nodes = NodeUtils.getNodeList("productAttribute", "productId_matching="+data.get("productId")+"&productAttributeCategoryId_notMatching="+data.get("productAttributeCategoryId").toString());
//        for(Node node : nodes){
//            nodeService.executeNode(node, "productAttribute", DELETE);
//        }


        return list;
    }

    private List<Node> productToCategoryMap(Map<String, Object> data) throws IOException {
        List<Node> list = new ArrayList<>();
        if(data.get("productToCategoryMap") == null) return list;

        List<Map<String, Object>> referenced = nodeBindingService.list("productToCategoryMap", "productId_matching="+data.get("productId").toString());
        List<Map<String, Object>> maps = JsonUtils.parsingJsonToList(data.get("productToCategoryMap").toString());
        for(Map<String, Object> map : referenced){
            if(!maps.contains(map)){
                nodeService.executeNode(map, "productToCategoryMap", DELETE);
            }
        }
        for(Map<String, Object> map : maps){
            if(!referenced.contains(map)){
                map = setMap(data, map);
                nodeService.executeNode(map, "productToCategoryMap", CREATE);
            }
        }

        return list;
    }

    private List<Node> productSearchFilter(Map<String, Object> data) throws IOException {
        List<Node> list = new ArrayList<>();
        if(data.get("productSearchFilter") == null) return list;

        List<Map<String, Object>> referenced = nodeBindingService.list("productSearchFilter", "productId_matching="+data.get("productId").toString());
        List<Map<String, Object>> maps = JsonUtils.parsingJsonToList(data.get("productSearchFilter").toString());
        for(Map<String, Object> map : referenced){
            if(!maps.contains(map)){
                nodeService.executeNode(map, "productSearchFilter", DELETE);
            }
        }
        for(Map<String, Object> map : maps){
            if(!referenced.contains(map)){
                map = setMap(data, map);
                nodeService.executeNode(map, "productSearchFilter", CREATE);
            }
        }

        return list;
    }
}
