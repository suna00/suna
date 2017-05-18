package net.ion.ice.core.node;

import java.nio.file.attribute.UserDefinedFileAttributeView;
import java.util.List;

/**
 * Created by jaeho on 2017. 5. 18..
 */
public class QueryResult {
    private List<Object> resultList ;

    private Integer totalSize ;

    public QueryResult(List nodes, Integer totalSize){
        this.resultList = nodes ;
        this.totalSize = totalSize ;
    }

    public QueryResult(List nodes){
        this(nodes, nodes.size()) ;
    }

    public List<Object> getResultList() {
        return resultList;
    }

    public Integer getTotalSize() {
        return totalSize;
    }
}
