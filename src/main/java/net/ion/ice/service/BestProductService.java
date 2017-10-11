package net.ion.ice.service;

import net.ion.ice.core.context.ExecuteContext;
import net.ion.ice.core.data.bind.NodeBindingService;
import net.ion.ice.core.node.NodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service("bestProductService")
public class BestProductService {

    @Autowired
    private NodeService nodeService;
    @Autowired
    private NodeBindingService nodeBindingService ;

    public ExecuteContext collect(ExecuteContext context){




        Map<String, Object> item = new LinkedHashMap<>();
        item.put("item", "");
        context.setResult(item);
        return context;
    }
}
