package net.ion.ice.core.context;

import org.apache.commons.lang3.StringUtils;

import java.text.ParseException;
import java.util.List;
import java.util.Map;

/**
 * Created by jaehocho on 2016. 12. 1..
 */
public class TemplateParam {
    private String templateStr ;
    private String paramStr ;
    private String methodStr ;
    private String methodParamStr ;
    private String[] methodParams ;
    private String valueStr ;
    private String arrayStr ;

    public TemplateParam(String templateStr){
        this.templateStr = templateStr ;
        this.paramStr = StringUtils.substringBetween(templateStr, "{{:", "}}").trim() ;
        this.valueStr = paramStr ;

        if(paramStr.contains("(") && paramStr.contains(")")){
            methodStr = StringUtils.substringBefore(paramStr, "(").trim() ;
            methodParamStr = StringUtils.substringBetween(paramStr, "(", ")").trim() ;
            if(methodParamStr.contains(",")){
                methodParams = StringUtils.split(methodParamStr, ",") ;
                for(int i=0; i<methodParams.length; i++){
                    methodParams[i] = methodParams[i].trim() ;
                }
                valueStr = methodParams[0] ;
            }else{
                valueStr = methodParamStr ;
            }
        }
    }

    public String getTemplateStr() {
        return templateStr;
    }

//    public String format(Node node) throws ParseException {
//        Field value = NodeUtils.getField(node, valueStr) ;
//        if(value == null || value.isNull()) return "" ;
//
//        String result = null;
//
//        if(value instanceof DisplayValue){
//            result = ((DisplayValue) value).getDisplayValue();
//        }else{
//            result = value.getValue().toString() ;
//        }
//
//        if(StringUtils.isNotEmpty(methodStr)){
//            return MethodHelper.execute(methodStr, methodParams, result, null) ;
//        }
//        return result ;
//    }

    public String format(Map<String, Object> data) throws ParseException {
        Object value = getValue(data, valueStr) ;

        if(StringUtils.isNotEmpty(methodStr)){
            return MethodHelper.execute(methodStr, methodParams, value, data) ;
        }

        if(value == null) return "" ;

        return value.toString() ;
    }

    private Object getValue(Map<String, Object> data, String key) {
        if(StringUtils.contains(key, '.')){
            String pre = StringUtils.substringBefore(key, ".");
            String pos = StringUtils.substringAfter(key, ".");

            Object value = data.get(pre) ;
            if( value != null && value instanceof Map){
                Map<String, Object> subData = (Map<String, Object>)value ;
                return getValue(subData, pos) ;
            }else if(value != null && value instanceof List){
                return null ;
            }else if(value == null){
                return null;
            }
        }else{
            return data.get(key) ;
        }
        return null ;
    }


    private List<Map<String, Object>> getArrayList(Map<String, Object> data, String key) {
        if(StringUtils.contains(key, '.')){
            String pre = StringUtils.substringBefore(key, ".");
            String pos = StringUtils.substringAfter(key, ".");

            this.arrayStr = StringUtils.isEmpty(this.arrayStr) ? pre : (this.arrayStr + "." + pre);

            Object value = data.get(pre) ;
            if( value != null && value instanceof Map){
                Map<String, Object> subData = (Map<String, Object>)value ;
                this.arrayStr = this.arrayStr + "." + pos ;
                return getArrayList(subData, pos) ;
            }else if(value != null && value instanceof List){
                this.valueStr = StringUtils.substringAfter(this.valueStr, this.arrayStr + ".") ;
                return (List<Map<String, Object>>) value;
            }else if(value == null){
                this.arrayStr = null ;
                return null;
            }
        }else{
            Object value = data.get(key) ;
            if(value != null && value instanceof List){
                this.valueStr = key;
                return (List<Map<String, Object>>) value;
            }
        }
        return null;
    }

//    public LIST getFieldList(Node node, String key){
//        if (StringUtils.contains(key, '.')) {
//            String pre = StringUtils.substringBefore(key, ".");
//            String pos = StringUtils.substringAfter(key, ".");
//
//            this.arrayStr = StringUtils.isEmpty(this.arrayStr) ? pre : (this.arrayStr + "." + pre);
//
//            Field value = NodeUtils.getField(node, pre);
//            if (value instanceof Nodeable && !value.isNull()) {
//                Node ref = ((Nodeable) value).getNode();
//                if (ref != null) {
//                    this.arrayStr = this.arrayStr + "." + pos ;
//                    return getFieldList(ref, pos);
//                } else {
//                    this.arrayStr = null ;
//                    return null;
//                }
//            } else if (value != null && value instanceof LIST) {
//                this.valueStr = StringUtils.substringAfter(this.valueStr, this.arrayStr + ".") ;
//                return (LIST) value;
//            }
//            this.arrayStr = null ;
//            return null;
//        }
//        return null;
//    }


}
