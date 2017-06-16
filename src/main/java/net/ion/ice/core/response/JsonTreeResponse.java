package net.ion.ice.core.response;

import net.ion.ice.core.query.SimpleQueryResult;

import java.util.Collection;

/**
 * Created by jaeho on 2017. 6. 5..
 */
public class JsonTreeResponse extends JsonResponse {
    protected Collection<?> items ;

    public JsonTreeResponse(SimpleQueryResult simpleQueryResult) {
        this.result = "200" ;
        this.resultMessage = "SUCCESS" ;
        this.items = simpleQueryResult.getResultList() ;
    }
    public Collection<?> getItems(){
        return items ;
    }

}
