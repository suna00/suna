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
        if(fieldOption.containsKey("query")) {
            resultType = ResultType.QUERY ;
            this.fieldOption = fieldOption ;
        }else if(fieldOption.containsKey("select")){
            resultType = ResultType.SELECT ;
            this.fieldOption = fieldOption ;
        }else if(fieldOption.containsKey("value")){
            resultType = ResultType.VALUE ;
            this.staticValue = fieldOption.get("value") ;
        }else {
            resultType = ResultType.OPTION ;

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

    public enum ResultType {
        QUERY,
        SELECT,
        EVENT,
        VALUE,
        OPTION
    }
}
