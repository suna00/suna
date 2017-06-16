package net.ion.ice.core.response;

import net.ion.ice.core.query.SimpleQueryResult;

import java.util.Collection;

/**
 * Created by jaeho on 2017. 5. 31..
 */
public class JsonArrayPagingResponse extends JsonResponse {
    protected Integer pageSize ; // 페이지당 item 건수
    protected Integer pageCount ; // 총 페이지 수
    protected Integer currentPage;
    protected Integer totalCount ;
    protected Integer resultCount ;

    protected Collection<?> items ;

    public JsonArrayPagingResponse(SimpleQueryResult simpleQueryResult) {
        this.result = "200" ;
        this.resultMessage = "SUCCESS" ;
        this.totalCount = simpleQueryResult.getTotalSize() ;
        this.resultCount = simpleQueryResult.getResultList().size() ;
        this.items = simpleQueryResult.getResultList() ;
        this.pageSize = simpleQueryResult.getPageSize() ;
        this.pageCount = totalCount / getPageSize() + 1;
        this.currentPage = simpleQueryResult.getCurrentPage() ;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public Integer getPageCount() {
        return pageCount;
    }

    public Integer getCurrentPage() {
        return currentPage;
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
