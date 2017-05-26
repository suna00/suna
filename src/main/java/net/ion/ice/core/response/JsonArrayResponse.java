package net.ion.ice.core.response;

import net.ion.ice.core.node.QueryResult;

import java.util.Collection;
import java.util.Map;

/**
 * Created by jaehocho on 2017. 2. 11..
 */
public class JsonArrayResponse extends JsonResponse {
    protected Integer totalCount ;
    protected Integer resultCount ;

    private Collection<?> items ;

    public JsonArrayResponse(Collection<?> list) {
        this.result = "200" ;
        this.resultMessage = "SUCCESS" ;
        this.totalCount = list.size() ;
        this.resultCount = list.size() ;
        this.items = list ;
    }

    public JsonArrayResponse(QueryResult queryResult) {
        this.result = "200" ;
        this.resultMessage = "SUCCESS" ;
        this.totalCount = queryResult.getTotalSize() ;
        this.resultCount = queryResult.getResultList().size() ;
        this.items = queryResult.getResultList() ;
    }

    public Integer getTotalCount(){
        return totalCount ;
    }

    public Integer getResultCount(){
        return resultCount ;
    }
}
