package net.ion.ice.service;

import net.ion.ice.core.context.ExecuteContext;
import net.ion.ice.core.node.NodeService;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service("sabangnetService")
public class SabangnetService {
    public static final String UPDATE = "update";
    public static final String SAVE = "save";
    private Logger logger = Logger.getLogger(ProductService.class);

    @Autowired
    private NodeService nodeService ;


    public void make(ExecuteContext context) {
        Map<String, Object> data = new LinkedHashMap<>(context.getData());

        try{
            nodeService.executeNode(data, "product", SAVE) ;



        } catch (Exception e){

        }
    }
}
