package net.ion.ice.response;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * Created by jaehocho on 2017. 2. 11..
 */
public class JsonArrayResponse extends JsonResponse {
    private String result ;
    private String resultMessage ;
    private Integer totatCount ;
    private Integer resultCount ;

    private Collection<Map<String,Object>> items ;

    public JsonArrayResponse(Collection<Map<String, Object>> list) {
        this.result = "200" ;
        this.resultMessage = "SUCCESS" ;
        this.totatCount = list.size() ;
        this.resultCount = list.size() ;
        this.items = list ;
    }
}
