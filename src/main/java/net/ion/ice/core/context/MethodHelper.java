package net.ion.ice.core.context;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;

import java.sql.Timestamp;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Created by jaehocho on 2016. 12. 29..
 */
public class MethodHelper {
//    private static Logger logger = LogManager.getLogger();

    private static String[] patterns = new String[]{"yyyyMMdd", "yyyyMMddHHmmss", "yyyyMMdd HHmmss", "yyyy-MM-dd", "yyyy.MM.dd", "yyyy/MM/dd", "yyyyMMdd-HHmmss", "yyyy-MM-dd HH:mm", "yyyy-MM-dd HH:mm:ss", "yyyy.MM.dd HH:mm:ss", "yyyy/MM/dd HH:mm:ss", "yyyy-MM-dd HH:mm:ss.SSS", "yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd'T'HH:mm:ssZZ"} ;

    public static String execute(String methodStr, String[] methodParams, Object value, Map<String, Object> data) {
        switch (methodStr){
            case "dateFormat" :{
                if(value == null || StringUtils.isEmpty(value.toString())) return "" ;
                Date date = null ;
                if(value instanceof Timestamp){
                    date = new Date(((Timestamp) value).getTime()) ;
                }else {
                    try {
                        date = DateUtils.parseDate(value.toString(), patterns);
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }
                if(date != null) {
                    return DateFormatUtils.format(date, methodParams[1]);
                }
                break;
            }
            case "stringFormat" :{
                if(value == null || StringUtils.isEmpty(value.toString())) return "" ;
                return String.format(methodParams[1], value.toString()) ;
            }
            case "decimalFormat" :{
                if(value == null || StringUtils.isEmpty(value.toString())) return "" ;
                return String.format(methodParams[1], Integer.parseInt(value.toString())) ;
            }
            case "substringBefore" :{
                if(value == null || StringUtils.isEmpty(value.toString())) return "" ;
                return StringUtils.substringBefore(value.toString(), methodParams[1]) ;
            }
            case "substringAfter" :{
                if(value == null || StringUtils.isEmpty(value.toString())) return "" ;
                return StringUtils.substringAfter(value.toString(), methodParams[1]) ;
            }
            case "replace" :{
                if(value == null || StringUtils.isEmpty(value.toString())) return "" ;
                return StringUtils.replace(value.toString(), methodParams[1], methodParams[2]) ;
            }
            case "replaceAll" :{
                if(value == null || StringUtils.isEmpty(value.toString())) return "" ;
                return StringUtils.replaceAll(value.toString(), methodParams[1], methodParams[2]) ;
            }
            case "substring" :{
                if(value == null || StringUtils.isEmpty(value.toString())) return "" ;
                return StringUtils.substring(value.toString(), Integer.parseInt(methodParams[1]), Integer.parseInt(methodParams[2])) ;
            }
            case "max" :{
                if(value == null || StringUtils.isEmpty(value.toString())) return "" ;
                return StringUtils.substring(value.toString(), 0, value.toString().length() > Integer.parseInt(methodParams[1]) ? Integer.parseInt(methodParams[1]) : value.toString().length()) ;
            }
            case "or" :{
                return value != null && StringUtils.isNotEmpty(value.toString()) ? value.toString() : (String) data.get(methodParams[1]);
            }
            case "isNullYn" :{
                if(value == null || StringUtils.isEmpty(value.toString())){
                    return "Y" ;
                }else{
                    return "N" ;
                }
            }
            case "isNotNullYn" :{
                if(value == null || StringUtils.isEmpty(value.toString())){
                    return "N" ;
                }else{
                    return "Y" ;
                }
            }
            case "decode" :{
                if(value == null || StringUtils.isEmpty(value.toString())) return "" ;
                String val = getDecodeValue(methodParams, value, data);
                if (val != null) return val;
                return value.toString() ;
            }
            case "decodeEmpty" :{
                if(value == null || StringUtils.isEmpty(value.toString())) return "" ;
                String val = getDecodeValue(methodParams, value, data);
                if (val != null) return val;
                return "" ;
            }
            case "replaceMulti" :{
                if(value == null || StringUtils.isEmpty(value.toString())) return "" ;
                String result = value.toString() ;
                for(int i=1; i<methodParams.length; i++){
                    String src = methodParams[i] ;
                    i++ ;
                    if(methodParams.length == i){
                        return result ;
                    }
                    result = StringUtils.replace(result, src, methodParams[i]) ;
                }
                return result ;
            }
            case "remove" :{
                if(value == null || StringUtils.isEmpty(value.toString())) return "" ;
                String valueStr = value.toString() ;
                for(int i=1; i<methodParams.length; i++){
                    valueStr = StringUtils.replaceAll(valueStr, methodParams[i], "") ;
                }
                return valueStr ;
            }
            case "unHtml":{
                if(value == null || StringUtils.isEmpty(value.toString())) return "" ;
                return StringUtils.remove(StringUtils.remove(StringUtils.remove(StringEscapeUtils.escapeHtml4(value.toString()), '\n'), '\r'), '\t') ;
            }
            case "isNotNullStr":{
                if(value == null) return "" ;
                if(StringUtils.isEmpty(value.toString())){
                    return "" ;
                }else{
                    return methodParams[1] ;
                }
            }
            case "isNullStr":{
                if(value == null) return methodParams[1] ;
                if(StringUtils.isEmpty(value.toString())){
                    return methodParams[1] ;
                }else{
                    return "" ;
                }
            }
            case "default":{
                if(value == null || StringUtils.isEmpty(value.toString())) return methodParams[1] ;
                if(StringUtils.isEmpty(value.toString())){
                    return methodParams[1] ;
                }else{
                    return value.toString() ;
                }
            }
            case "decodeVal" :{
                if(value == null || StringUtils.isEmpty(value.toString())) return "" ;
                String val = getDecodeValue(methodParams, value, data);
                if (val != null) return val;
                return value.toString() ;
            }
            case "decodeValEmpty" :{
                if(value == null || StringUtils.isEmpty(value.toString())) return "" ;
                String val = getDecodeValue(methodParams, value, data);
                if (val != null) return val;
                return "" ;
            }
            default :
                methodStr = StringUtils.capitalize(methodStr) ;
                String methodClass = "net.ion.ice.core.context.method." + methodStr ;
                try {
                    Class<? extends MethodExec> clazz = (Class<? extends MethodExec>) Class.forName(methodClass);
                    if(clazz != null) {
                        return clazz.newInstance().execute(new String[]{}, value, data);
                    }
                } catch (Exception e) {
//                    logger.warn("NOT FOUND METHOD : " + methodStr);
                }
                if(value == null) return "" ;
                return value.toString();
        }
        if(value == null) return "" ;
        return value.toString();
    }

    private static String getDecodeValue(String[] methodParams, Object value, Map<String, Object> data) {
        for(int i=1; i<methodParams.length; i++){
            String test = methodParams[i] ;
            i++ ;
            if(methodParams.length == i){
                if(data != null && data.containsKey(test)){
                    return data.get(test).toString() ;
                }
                return test;
            }
            String val = methodParams[i] ;
            if(test.equals(value.toString())){
                if(data != null && data.containsKey(val)){
                    return data.get(val).toString() ;
                }
                return val ;
            }
        }
        return null;
    }
}
