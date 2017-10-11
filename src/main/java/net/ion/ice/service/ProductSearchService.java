package net.ion.ice.service;

import net.ion.ice.core.context.ExecuteContext;
import net.ion.ice.core.data.bind.NodeBindingService;
import net.ion.ice.core.json.JsonUtils;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service("productSearchService")
public class ProductSearchService {

    public static final String category = "category";

    @Autowired
    private NodeService nodeService;
    @Autowired
    private NodeBindingService nodeBindingService ;

//    "categoryMap": {
//                    "1" : 5
//                    "10 : 10,
//                    "20 : 11,
//                    "21 : 1,
//                    "81 : 2,
//                    "82 : 3,
//                }
    public ExecuteContext getGroupCount(ExecuteContext context) throws IOException {
        Map<String, Object> data = context.getData();
        if(data.get("categoryMap") == null) return null;

        Map<String, Object> result = new LinkedHashMap<>();

        Map<String, Object> map = JsonUtils.parsingJsonToMap((String) data.get("categoryMap"));
        List<Node> levelA = nodeService.getNodeList("category", "level=A");
        for(Node A : levelA){
            int count = 0;
            for(String key : map.keySet()){
                if(key.equals(A.getId())){
                    count = JsonUtils.getIntValue(map, key);
                }
            }
            A.put("count",count);
            A.put("totalCount",count);
            A.put("lowerCategoryList", new ArrayList<>());
            result.put(A.getId(), A);
        }

        List<Node> levelC = nodeService.getNodeList("category", "level=C");
        for(Node C : levelC){
            for(String key : map.keySet()){
                if(key.equals(C.getId())){
                    C.put("count", map.get(key));
                }
            }
        }

        List<Node> levelB = nodeService.getNodeList("category", "level=B");
        for(Node B : levelB){
            B.put("count", 0);
            B.put("totalCount", 0);
            Map<String, Object> A = (Map<String, Object>) result.get(B.getValue("upperId"));
            for(String key : map.keySet()){
                if(key.equals(B.getId())){
                    B.put("count", map.get(key));
                    int totalCount = JsonUtils.getIntValue(map, key);
                    for(Node C : levelC){
                        if(B.getId().equals(C.getValue("upperId"))){
                            totalCount += JsonUtils.getIntValue(C, "count");
                        }
                    }
                    B.put("totalCount", totalCount);

                    A.put("totalCount", JsonUtils.getIntValue(A, "totalCount") + totalCount);
                }
            }
            List<Map<String, Object>> lower = (List<Map<String, Object>>) A.get("lowerCategoryList");
            lower.add(B);
        }


        Map<String, Object> item = new LinkedHashMap<>();
        item.put("item", result);
        context.setResult(item);
        return context;
    }
}

