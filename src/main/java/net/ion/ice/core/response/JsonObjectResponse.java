package net.ion.ice.core.response;

import net.ion.ice.core.node.Node;

/**
 * Created by jaehocho on 2017. 2. 11..
 */
public class JsonObjectResponse extends JsonResponse {

    private Node item;

    public JsonObjectResponse(Node node) {
        super();
        result = "200";
        resultMessage = "SUCCESS";
        this.item = node;
    }

    public Node getItem() {
        return item;
    }
}
