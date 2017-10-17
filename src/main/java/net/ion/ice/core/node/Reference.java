package net.ion.ice.core.node;

import net.ion.ice.core.context.ReadContext;
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

    public Reference(Node node, ReadContext context) {
        this(node.getId(), node.getLabel(context)) ;
    }


    public String getRefId() {
        return refId;
    }


    @Override
    public boolean equals(Object reference){
        if(reference instanceof Reference){
            return getRefId().equals(((Reference) reference).getRefId());
        }

        return getRefId().equals(reference) ;
    }


}
