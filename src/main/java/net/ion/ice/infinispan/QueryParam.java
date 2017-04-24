package net.ion.ice.infinispan;

/**
 * Created by jaeho on 2017. 4. 24..
 */
public class QueryParam {
    private String queryKey ;
    private String queryValue ;

    private String method ;

    public QueryParam(String queryKey, String method, String queryValue) {
        this.queryKey = queryKey ;
        this.method = method ;
        this.queryValue = queryValue ;
    }

    public QueryParam(String queryKey, String queryValue){
        this(queryKey, "matching", queryValue) ;
    }
}
