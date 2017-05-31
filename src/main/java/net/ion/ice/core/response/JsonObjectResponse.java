package net.ion.ice.core.response;

import net.ion.ice.core.node.Node;

/**
 * Created by jaehocho on 2017. 2. 11..
 */
public class JsonObjectResponse extends JsonResponse {
    public JsonObjectResponse(Node node) {
        super();
        result = "200" ;
        resultMessage = "success" ;
    }
}
