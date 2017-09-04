package net.ion.ice.test;


import net.ion.ice.core.context.ExecuteContext;
import net.ion.ice.core.event.EventService;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service("testService")
public class TestService {

    @Autowired
    private NodeService nodeService ;

    public void itemViewsUpdate(ExecuteContext context){
        Node item = context.getNode() ;
        if(item == null) return ;
        int views = item.getIntValue("views") + 1;
        Map<String, Object> updateData = new LinkedHashMap<>() ;
        updateData.put("id", item.getId()) ;
        updateData.put("views", views) ;
        ExecuteContext updateContext = ExecuteContext.makeContextFromMap(updateData, context.getNodeType().getTypeId(), EventService.UPDATE) ;
        updateContext.execute();

        context.setResult(updateContext.getNode()) ;
    }


    public void categoryViewsUpdate(ExecuteContext context){
        Node item = context.getNode() ;
        if(item == null) return ;

        Node category = item.getReferenceNode("category") ;

        int views = category.getIntValue("itemViews") + 1;
        Map<String, Object> updateData = new LinkedHashMap<>() ;
        updateData.put("id", category.getId()) ;
        updateData.put("itemViews", views) ;
        ExecuteContext updateContext = ExecuteContext.makeContextFromMap(updateData, "testCategory", EventService.UPDATE) ;
        updateContext.execute();
//        context.setResult(updateContext.getNode()) ;
    }
}
