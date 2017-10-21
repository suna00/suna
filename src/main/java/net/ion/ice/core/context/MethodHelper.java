package net.ion.ice.core.context;

import net.ion.ice.ApplicationContextManager;
import net.ion.ice.core.file.FileValue;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;

import java.sql.Timestamp;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
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
                }else if(value instanceof Date){
                    date = (Date) value;
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
            case "aboveInt" :{
                if(value == null || StringUtils.isEmpty(value.toString())) return "false" ;
                int val = Integer.parseInt(value.toString()) ;
                int p = Integer.parseInt(methodParams[1]) ;
                return (val >= p) ? "true" : "false";
            }
            case "belowInt" :{
                if(value == null || StringUtils.isEmpty(value.toString())) return "false" ;
                int val = Integer.parseInt(value.toString()) ;
                int p = Integer.parseInt(methodParams[1]) ;
                return (val <= p) ? "true" : "false";
            }
            case "excessInt" :{
                if(value == null || StringUtils.isEmpty(value.toString())) return "false" ;
                int val = Integer.parseInt(value.toString()) ;
                int p = Integer.parseInt(methodParams[1]) ;
                return (val > p) ? "true" : "false";
            }
            case "underInt" :{
                if(value == null || StringUtils.isEmpty(value.toString())) return "false" ;
                int val = Integer.parseInt(value.toString()) ;
                int p = Integer.parseInt(methodParams[1]) ;
                return (val < p) ? "true" : "false";
            }
            case "equalsVal" :{
                if(value == null || StringUtils.isEmpty(value.toString())) return "false" ;
                return (methodParams[0].equalsIgnoreCase(value.toString())) ? "true" : "false";
            }
            case "equals" :{
                Object p = data.get(methodParams[1]) ;
                if(value == null && p == null) return "true" ;
                if(value == null) return "false" ;
                if(p == null) return "false" ;
                return (value.toString().equals(p.toString())) ? "true" : "false";
            }
            case "makeIdValue" :{
                if(StringUtils.isEmpty(methodParams[0])) return "" ;
                String resultValue = makeIdValue(methodParams, data);
                return resultValue;
            }
            case "conditionSysdate" :{
                String defaultStr = "";
                if(methodParams.length >2 && !methodParams[2].isEmpty()){
                    defaultStr = methodParams[2].toString();
                }
                if(value == null) return defaultStr ;
                String condition= methodParams[1];
                if(StringUtils.isEmpty(methodParams[1])) condition =  "" ;
                if(value.equals(condition)){
                    if(methodParams.length >3 && !methodParams[3].isEmpty()){
                        Date nowDate = new Date();
                        return DateFormatUtils.format(nowDate, methodParams[3]);
                    }else{
                        return data.get("now").toString();
                    }
                }else{
                    return defaultStr;
                }
            }
            case "getCommaItem" :{
                String defaultStr = "";
                int paramInt = 0;

                if(!methodParams[2].isEmpty()){
                    defaultStr = methodParams[2].toString();
                }

                if(value == null || StringUtils.isEmpty(value.toString())) return defaultStr ;

                String[] valueArrays = value.toString().split(",");
                if(valueArrays.length == 0) return value.toString();

                if(!methodParams[1].isEmpty() && "last".equals(methodParams[1].toString())){
                    paramInt = valueArrays.length -1;
                }else{
                    paramInt = Integer.parseInt(methodParams[1]);
                }
                return valueArrays[paramInt] ;
            }
            case "concatStr":{
                if(value == null || StringUtils.isEmpty(value.toString())) return "" ;
                if(methodParams[1] == null) return "";
                String concatStr = value.toString()+methodParams[1];
                return concatStr;
            }
            case "convertString":{
                if(value == null) return "";
                return value.toString();
            }
            case "weekDate":{
                String defaultStr = "";
                if(methodParams.length >2 && !methodParams[2].isEmpty()){
                    defaultStr = methodParams[2].toString();
                }
                if(methodParams[0].isEmpty())return defaultStr ;
                String patternStr = "yyyyMMdd";
                if(methodParams.length >1 && !methodParams[1].isEmpty()){
                    patternStr = methodParams[1];
                }
                Calendar calendar = Calendar.getInstance();
                if(Integer.parseInt(methodParams[0])>7){
                    int weekNum = Integer.parseInt(methodParams[0])-7;
                    calendar.set(Calendar.DAY_OF_WEEK,weekNum);
                    calendar.add(Calendar.DATE, 7);
                }else{
                    calendar.set(Calendar.DAY_OF_WEEK,Integer.parseInt(methodParams[0]));
                }
                return DateFormatUtils.format(calendar.getTime(),patternStr);
            }
            case "getEnvValue":{
                if(methodParams.length <1 || methodParams[0].isEmpty()){
                    return "";
                }

                return ApplicationContextManager.getContext().getEnvironment().getProperty(methodParams[0]) ;
            }
            case "conditionDefault":{
                if(methodParams == null || methodParams.length < 2) return "";
                if(value == null || StringUtils.isEmpty(value.toString())) return methodParams[1] ;
                boolean conditionMatchYn = false;
                for(int i=2; i<methodParams.length; i++){
                    String test = methodParams[i] ;
                    if(test.equals(value.toString())){
                        conditionMatchYn = true;
                    }
                }
                if(!conditionMatchYn){
                    return methodParams[1] ;
                }else{
                    return value.toString();
                }
            }
            case "fileName":{
                if(methodParams == null || methodParams.length < 2) return "";
                if(value == null || StringUtils.isEmpty(value.toString())) return "" ;

                String typeId = methodParams[1] ;
                String pid = methodParams[2] ;

                Node node = NodeUtils.getNode(typeId, (String) value) ;
                FileValue fileValue = (FileValue) node.get(pid);
                if(fileValue != null){
                    return fileValue.getFileName() ;
                }
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

    private static String makeIdValue(String[] methodParams, Map<String, Object> data){
        String returnVal = "";
        if(methodParams.length < 3) return null;

        for(int i=1; i<methodParams.length; i++){
            String paramStr = methodParams[i];

            if (data != null && data.containsKey(paramStr)) {
                if(i == 1){
                    returnVal = data.get(paramStr).toString();
                }else{
                    returnVal += methodParams[0] + data.get(paramStr).toString();
                }
            }else{
                returnVal = "";
                break;
            }
        }
        return returnVal;
    }
}
