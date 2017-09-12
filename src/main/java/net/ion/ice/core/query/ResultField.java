package net.ion.ice.core.query;

import net.ion.ice.core.context.*;

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

    private Object staticValue ;

    private ResultType resultType ;

    private ExecuteType executeType ;


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
        if(fieldOption.containsKey("resultType")){
            resultType = ResultType.valueOf(fieldOption.get("resultType").toString().toUpperCase()) ;
        }else{
            resultType = ResultType.LIST ;
        }

        if(fieldOption.containsKey("query")) {
            executeType = ExecuteType.QUERY ;
            this.fieldOption = fieldOption ;
        }else if(fieldOption.containsKey("select")){
            executeType = ExecuteType.SELECT ;
            this.fieldOption = fieldOption ;
        }else if(fieldOption.containsKey("value")){
            executeType = ExecuteType.VALUE ;
            this.staticValue = fieldOption.get("value") ;
        }else {
            executeType = ExecuteType.OPTION ;

            if (fieldOption.containsKey("field")) {
                this.fieldValue = (String) fieldOption.get("field");
            } else {
                this.fieldValue = fieldName;
            }

            this.fieldOption = fieldOption;
            this.fieldContext = FieldContext.createContextFromOption(fieldOption);
        }
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

    public boolean isStaticValue(){
        return this.staticValue != null ;
    }

    public Object getStaticValue(){
        return staticValue ;
    }

    public ResultType getResultType() {
        return resultType;
    }

    public ExecuteType getExecuteType() {
        return executeType;
    }

    public enum ResultType {
        MERGE,
        NESTED,
        LIST,
        READ,
        NONE,
        SIZE,
        VALUE
    }

    public enum ExecuteType {
        QUERY,
        SELECT,
        EVENT,
        VALUE,
        OPTION
    }
}
