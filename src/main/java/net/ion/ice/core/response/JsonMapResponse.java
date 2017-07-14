package net.ion.ice.core.response;

import java.util.Map;

/**
 * Created by seonwoong on 2017. 7. 14..
 */
public class JsonMapResponse extends JsonResponse{

    private Map<String, Object> item;

    public JsonMapResponse(Map<String, Object> map) {
        super();
        result = "200" ;
        resultMessage = "SUCCESS" ;
        this.item = map;
    }

    public Map<String, Object> getItem() {
        return item;
    }

}
