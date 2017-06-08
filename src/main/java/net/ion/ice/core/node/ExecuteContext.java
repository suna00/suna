package net.ion.ice.core.node;

import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by jaeho on 2017. 5. 31..
 */
public class ExecuteContext {
    private Map<String, Object> data  ;
    private Node node;
    private NodeType nodeType;

    public static ExecuteContext makeContextFromParameter(Map<String, String[]> parameterMap, NodeType nodeType) {
        ExecuteContext ctx = new ExecuteContext();

        Map<String, Object> data = new HashMap<>();
        for (String paramName : parameterMap.keySet()) {

            String[] values = parameterMap.get(paramName);
            String value = null;

            if (values != null && values.length > 0 && StringUtils.isNotEmpty(values[0])) {
                value = values[0];
            }

            data.put(paramName, value);
        }

        ctx.setData(data);
        ctx.setNodeType(nodeType);

        ctx.makeNode() ;
        return ctx ;
    }

    private void makeNode() {
        this.node = new Node(data, nodeType.getTypeId()) ;
    }

    public void setData(Map<String,Object> data) {
        this.data = data;
    }

    public Node getNode() {
        return node;
    }

    public void setNodeType(NodeType nodeType) {
        this.nodeType = nodeType;
    }
}
