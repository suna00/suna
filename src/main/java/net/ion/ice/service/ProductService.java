package net.ion.ice.service;

import net.ion.ice.core.context.ExecuteContext;
import net.ion.ice.core.json.JsonUtils;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeService;
import net.ion.ice.core.node.NodeUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service("productService")
public class ProductService {

    public static final String CREATE = "create";
    public static final String UPDATE = "update";
    public static final String DELETE = "delete";
    public static final String SAVE = "save";

    @Autowired
    private NodeService nodeService ;

    public void make(ExecuteContext context) {
        Map<String, Object> data = context.getData();

        nodeService.executeNode(data, "product", SAVE) ;
        List<Node> baseOptions = createOptions(data, "baseOption");
        List<Node> addOptions = createOptions(data, "addOption");
        createOptionItems(data, baseOptions, "baseOption");
        createOptionItems(data, addOptions, "addOption");

    }


    private List<Node> createOptions(Map<String, Object> data, String type)  {
        List<Node> list = new ArrayList<>();

        if(data.get(type) == null || data.get(type + "Item") == null) return list;

        try {
            for(Map<String, Object> map : JsonUtils.parsingJsonToList(data.get(type).toString())){
                data.putAll(map);
                if(type.equals("baseOption")){
                    data.put("productOptionType", data.get("baseOptionType"));
                    data.put("required", "true");
                }else{
                    data.put("productOptionType", "add");
                }

                Node node = nodeService.executeNode(data, "productOption", SAVE) ;
                createCodes(data, node);

                list.add(node);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return list;
    }

    private void createCodes(Map<String, Object> data, Node node) {
        for(String codeName : StringUtils.split(data.get("productOptionCodeNames").toString(),",")){
            data.put("productOptionId", node.getId());
            data.put("name", codeName);
            nodeService.executeNode(data, "productOptionCode", SAVE) ;
        }
    }


    private List<Node> createOptionItems(Map<String, Object> data, List<Node> options, String type) {
        List<Node> list = new ArrayList<>();

        if(data.get(type) == null || data.get(type + "Item") == null) return list;

        for(Map<String, Object> item : (List<Map<String, Object>>) data.get(type + "Item")){
            data.putAll(item);

            String codeIds = setItemCodes(item.get("productOptionCodes").toString(), data.get("productId").toString());
            data.put("productOptionCodes", codeIds);

            Node node = nodeService.executeNode(data, "productOptionItem", SAVE) ;
            list.add(node);
        }

        return list;
    }


    private String setItemCodes(String codeNames, String productId){
        String codeIds = "";
        List<Node> codes = NodeUtils.getNodeList("productOptionCode", "productId_matching="+productId);
        for(String name : StringUtils.split(codeNames, ",")){
            for(Node code : codes){
                if(name.equals(code.get("name").toString())){
                    codeIds = codeIds + (codeIds != null ? "," : "") + code.getId();
                }
            }
        }

        return codeIds ;

    }

}
