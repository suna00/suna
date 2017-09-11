package net.ion.ice.core.node;

import net.ion.ice.core.context.ReadContext;
import org.apache.commons.lang3.StringUtils;

/**
 * Created by jaehocho on 2017. 8. 18..
 */
public class ReferenceView extends Reference{
    protected Object item ;

    public ReferenceView(Node node, NodeType nodeType, ReadContext context) {
        super(node, nodeType, context);
        this.item = node ;
    }

    public ReferenceView(String refId, String label) {
        super(refId, label);
    }


    public void setItem(Object item){
        this.item = item ;
    }


    public Object getItem(){
        return  item ;
    }
}
