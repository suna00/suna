package net.ion.ice.service;

import net.ion.ice.core.context.ExecuteContext;
import net.ion.ice.core.json.JsonUtils;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeService;
import net.ion.ice.core.node.NodeUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

@Service("productService")
public class ProductService {
    public static final String UPDATE = "update";
    public static final String SAVE = "save";
    private Logger logger = Logger.getLogger(ProductService.class);

    @Autowired
    private NodeService nodeService ;

    /**
     * Make.
     *
     * @param context the context
     */
    public void make(ExecuteContext context) {
        Map<String, Object> data = new LinkedHashMap<>(context.getData());

        try{
            nodeService.executeNode(data, "product", SAVE) ;
            List<Node> baseOptions = productOptions(data, "baseOption");
            List<Node> addOptions = productOptions(data, "addOption");
            productOptionItems(data, baseOptions, "baseOption");
            productOptionItems(data, addOptions, "addOption");

        } catch (Exception e){

        }
    }


    /**
     * Product options list.
     *
     * @param data the data
     * @param type the type
     * @return the list
     * @throws IOException the io exception
     */
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

}
