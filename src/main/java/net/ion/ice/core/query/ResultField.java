package net.ion.ice.core.query;

import net.ion.ice.core.context.Context;
import net.ion.ice.core.context.QueryContext;

/**
 * Created by jaeho on 2017. 6. 15..
 */
public class ResultField {
    private Context context ;
    private String fieldName;
    private String fieldValue ;

    public ResultField(String fieldName, String fieldValue){
        this.fieldName = fieldName ;
        this.fieldValue=fieldValue ;
    }


    public ResultField(String fieldName, Context context) {
        this.fieldName = fieldName ;
        this.context = context ;
    }

    public String getFieldName() {
        return fieldName;
    }


    public Context getQueryContext() {
        return context;
    }
    public Context getContext() {
        return context;
    }


    public String getFieldValue() {
        return fieldValue;
    }
}
