package net.ion.ice.service;

import net.ion.ice.core.context.ExecuteContext;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service("cartService")
public class CartService {

    public static final String CREATE = "create";
    public static final String UPDATE = "update";
    public static final String DELETE = "delete";
    public static final String ALL_EVENT = "allEvent";

    @Autowired
    private NodeService nodeService ;

    public void addCart(ExecuteContext context){
        Map<String, Object> data = context.getData();

        nodeService.executeNode(data, context.getNode().getTypeId(), CREATE) ;    // goods, securities
        nodeService.executeNode(data, "product", CREATE) ;

        try {

        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    public void update(ExecuteContext context){
        Node node = context.getNode() ;

    }

    public void delete(ExecuteContext context){
        Node node = context.getNode() ;

    }

}
