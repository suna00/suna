package net.ion.ice.core.node;

import java.util.List;

/**
 * Created by jaeho on 2017. 5. 18..
 */
public class QueryResult {
    private List<Node> resultList ;

    private Integer totalSize ;

    public QueryResult(List nodes, Integer totalSize){
        this.resultList = nodes ;
        this.totalSize = totalSize ;
    }

    public QueryResult(List nodes){
        this(nodes, nodes.size()) ;
    }

    public List<Node> getResultList() {
        return resultList;
    }

    public Integer getTotalSize() {
        return totalSize;
    }
}
