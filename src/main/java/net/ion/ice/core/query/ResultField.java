package net.ion.ice.core.query;

import net.ion.ice.core.context.QueryContext;

/**
 * Created by jaeho on 2017. 6. 15..
 */
public class ResultField {
    private QueryContext queryContext ;
    private String fieldName;
    private String fieldValue ;

    public ResultField(String fieldName, String fieldValue){
        this.fieldName = fieldName ;
        this.fieldValue=fieldValue ;
    }


    public ResultField(String fieldName, QueryContext queryContext) {
        this.fieldName = fieldName ;
        this.queryContext = queryContext ;
    }

    public String getFieldName() {
        return fieldName;
    }


    public QueryContext getQueryContext() {
        return queryContext;
    }

    public String getFieldValue() {
        return fieldValue;
    }
}
