package net.ion.ice.core.query;

import net.ion.ice.core.context.Context;
import net.ion.ice.core.context.FieldContext;
import net.ion.ice.core.context.QueryContext;

import java.util.Map;

/**
 * Created by jaeho on 2017. 6. 15..
 */
public class ResultField {
    private Context context ;
    private String fieldName;
    private String fieldValue ;
    private Map<String, Object> fieldOption ;

    private FieldContext fieldContext ;

    public ResultField(String fieldName, String fieldValue){
        this.fieldName = fieldName ;
        this.fieldValue=fieldValue ;
    }


    public ResultField(String fieldName, Context context) {
        this.fieldName = fieldName ;
        this.context = context ;
    }

    public ResultField(String fieldName, Map<String, Object> fieldOption) {
        this.fieldName = fieldName ;
        if(fieldOption.containsKey("field")){
            this.fieldValue = (String) fieldOption.get("field");
        }else{
            this.fieldValue = fieldName ;
        }

        this.fieldOption = fieldOption ;
        this.fieldContext = FieldContext.createContextFromOption(fieldOption) ;
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

    public Map<String, Object> getFieldOption(){
        return fieldOption ;
    }

    public FieldContext getFieldContext(){
        return fieldContext ;
    }
}
