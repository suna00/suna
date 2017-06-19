package net.ion.ice.core.query;

import net.ion.ice.core.node.Node;
import net.ion.ice.core.query.QueryContext;

import java.util.List;

/**
 * Created by jaeho on 2017. 5. 18..
 */
public class SimpleQueryResult {

    private List<Node> resultList ;

    private Integer totalSize ;

    private QueryContext queryContext ;
    private boolean tree;

    public SimpleQueryResult(List nodes, QueryContext queryContext){
        this.resultList = nodes ;
        this.queryContext = queryContext ;
        this.totalSize = queryContext.getResultSize() ;
        this.tree = queryContext.isTreeable() ;
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

    public boolean isTree() {
        return tree;
    }
}
