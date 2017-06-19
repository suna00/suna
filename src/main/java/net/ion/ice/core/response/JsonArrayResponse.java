package net.ion.ice.core.response;

import net.ion.ice.core.query.SimpleQueryResult;

import java.util.Collection;

/**
 * Created by jaehocho on 2017. 2. 11..
 */
public class JsonArrayResponse extends JsonResponse {
    protected Integer totalCount ;
    protected Integer resultCount ;

    protected Collection<?> items ;

    public JsonArrayResponse(Collection<?> list) {
        this.result = "200" ;
        this.resultMessage = "SUCCESS" ;
        this.totalCount = list.size() ;
        this.resultCount = list.size() ;
        this.items = list ;
    }

    public JsonArrayResponse(SimpleQueryResult simpleQueryResult) {
        this.result = "200" ;
        this.resultMessage = "SUCCESS" ;
        this.totalCount = simpleQueryResult.getTotalSize() ;
        this.resultCount = simpleQueryResult.getResultList().size() ;
        this.items = simpleQueryResult.getResultList() ;
    }

    public Integer getTotalCount(){
        return totalCount ;
    }

    public Integer getResultCount(){
        return resultCount ;
    }

    public Collection<?> getItems(){
        return items ;
    }
}
