package net.ion.ice.core.response;

import net.ion.ice.core.node.QueryResult;

import java.util.Collection;

/**
 * Created by jaeho on 2017. 6. 5..
 */
public class JsonTreeResponse extends JsonResponse {
    protected Collection<?> items ;

    public JsonTreeResponse(QueryResult queryResult) {
        this.result = "200" ;
        this.resultMessage = "SUCCESS" ;
        this.items = queryResult.getResultList() ;
    }
    public Collection<?> getItems(){
        return items ;
    }

}
