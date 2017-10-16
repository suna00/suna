package net.ion.ice.cjmwave.db.sync.utils;

import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeService;
import net.ion.ice.core.node.NodeType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by juneyoungoh on 2017. 9. 5..
 */
public class NodeMappingUtils {

    public static Map<String, String> extractPropertyColumnMap(List<Node> mappers) {
        Map<String, String> mapperMap = new HashMap<>();
        for(Node mapper : mappers) {
            mapperMap.put(String.valueOf(mapper.get("propertyId")), String.valueOf(mapper.get("columnName")));
        }
        return mapperMap;
    }

    /*
    * jdbc 쿼리 결과를 node.pid 에 맞춰줌
    * */
    public static Map<String, Object> mapData (String targetNodeType, Map<String, Object> singleQueryResult, Map<String, String> mapperStore) {
        if(mapperStore == null || mapperStore.isEmpty()) {
            singleQueryResult.put("typeId", targetNodeType);
            return singleQueryResult;
        }

        Map<String, Object> combined = new HashMap<String, Object>();
        mapperStore.forEach((k, v) -> {
            combined.put(k, singleQueryResult.get(v));
            if(k.equals("mnetIfTrtYn")){
                combined.put(k, true);
            }
        });
        combined.put("typeId", targetNodeType);
        return combined;
    }

    public static String retrieveNodePrimaryKey (NodeService nodeService, String nodeTypeId) {
        String pk = null;
        NodeType nodeType = nodeService.getNodeType(nodeTypeId);
        List<String> idables = nodeType.getIdablePIds();
        if(idables != null && idables.size() == 1) pk = idables.get(0);
        return pk;
    }
}
