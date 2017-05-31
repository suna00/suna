package net.ion.ice.core.node;

import net.ion.ice.core.infinispan.QueryContext;

import java.util.List;

/**
 * Created by jaeho on 2017. 5. 18..
 */
public class QueryResult {

    private List<Node> resultList ;

    private Integer totalSize ;

    private QueryContext queryContext ;

    public QueryResult(List nodes, Integer totalSize, QueryContext queryContext){
        this.resultList = nodes ;
        this.totalSize = totalSize ;
        this.queryContext = queryContext ;
    }

    public QueryResult(List nodes){
        this(nodes, nodes.size(), null) ;
    }

    public List<Node> getResultList() {
        return resultList;
    }

    public Integer getTotalSize() {
        return totalSize;
    }


    public Integer getPageSize() {
        return queryContext.getPageSize();
    }

    public Integer getCurrentPage() {
        return queryContext.getCurrentPage();
    }

    public boolean isPaging(){
        return queryContext.isPaging() ;
    }

}
