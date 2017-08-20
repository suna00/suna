package net.ion.ice.core.node;

import org.apache.commons.lang3.StringUtils;

/**
 * Created by jaehocho on 2017. 8. 18..
 */
public class Reference extends Code {
    protected String refId ;

    public Reference(String refId, String label) {
        super();
        this.refId = refId ;
        this.value = StringUtils.contains(refId, ">") ? StringUtils.substringAfterLast(refId, ">") : refId ;
        this.label = label ;
    }

    public Reference(Node node, NodeType nodeType) {
        this(node.getId(), node.getLabel(nodeType)) ;
    }


    public String getRefId() {
        return refId;
    }
}
