package net.ion.ice.core.node;


import net.ion.ice.core.context.ExecuteContext;
import net.ion.ice.core.infinispan.InfinispanRepositoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.List;

@Service("nodeHistoryService")
public class NodeHistoryService {

    @Autowired
    private InfinispanRepositoryService repositoryService ;

    public void createHistory(ExecuteContext executeContext){
        if(executeContext.isExecute() && executeContext.getNodeType().isHistoryable()) {
            Node node = executeContext.getNode();
            Integer version = repositoryService.lastHistoryVersion(node.getTypeId(), node.getId()) ;

            NodeHistory history = new NodeHistory(node, version + 1, executeContext.getEvent(), executeContext.getChangedProperties()) ;
            repositoryService.cacheHistory(node.getTypeId(), history) ;
        }
    }

    public Object getHistoryList(NodeType nodeType, Node node) {
        List<NodeHistory> histories = repositoryService.getHistories(nodeType.getTypeId(), node.getId()) ;
        return histories;
    }
}
