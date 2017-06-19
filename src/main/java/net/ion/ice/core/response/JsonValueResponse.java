package net.ion.ice.core.response;

import net.ion.ice.core.node.Node;

/**
 * Created by jaeho on 2017. 6. 13..
 */
public class JsonValueResponse extends JsonResponse {

    private Object value ;

    public JsonValueResponse(Object value) {
            super();
            result = "200" ;
            resultMessage = "SUCCESS" ;
            this.value = value ;
    }

    public Object getValue(){
        return value ;
    }
}
