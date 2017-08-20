package net.ion.ice.service;

import net.ion.ice.core.context.ExecuteContext;
import net.ion.ice.core.json.JsonUtils;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;

@Service("productService")
public class ProductService {

    public static final String CREATE = "create";
    public static final String UPDATE = "update";
    public static final String DELETE = "delete";
    public static final String ALL_EVENT = "allEvent";

    @Autowired
    private NodeService nodeService ;

    public void create(ExecuteContext context){
        Map<String, Object> data = context.getData();

//        nodeService.executeNode(data, data.get("contentsType").toString(), CREATE) ;    // goods, securities
        nodeService.executeNode(data, "product", CREATE) ;

        try {
            createList(data,"productOption");
            createList(data,"productOptionItem");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void createList(Map<String, Object> data, String tid) throws IOException {
        for(Map<String, Object> map : JsonUtils.parsingJsonToList(data.get(tid).toString())){
            data.putAll(map);
            nodeService.executeNode(data, tid, CREATE) ;
        }
    }


    public void update(ExecuteContext context){
        Node node = context.getNode() ;

    }

    public void delete(ExecuteContext context){
        Node node = context.getNode() ;

    }


}
