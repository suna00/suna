package net.ion.ice.core.response;

import net.ion.ice.core.node.QueryResult;

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

    public JsonArrayPagingResponse(QueryResult queryResult) {
        this.result = "200" ;
        this.resultMessage = "SUCCESS" ;
        this.totalCount = queryResult.getTotalSize() ;
        this.resultCount = queryResult.getResultList().size() ;
        this.items = queryResult.getResultList() ;
        this.pageSize = queryResult.getPageSize() ;
        this.pageCount = totalCount / getPageSize() + 1;
        this.currentPage = queryResult.getCurrentPage() ;
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
