package net.ion.ice.core.context;


import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeType;
import org.apache.commons.lang3.StringUtils;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by jaehocho on 2016. 12. 1..
 */
public class Template {
    protected static final Pattern templateParamPattern = Pattern.compile("\\{\\{:(.*?)\\}\\}");
    protected static final Pattern templateArrayStartPattern = Pattern.compile("\\{\\{\\[:(\\w+)(\\.)?(\\w+)\\]\\}\\}");
    protected static final Pattern templateArrayEndPattern = Pattern.compile("\\{\\{\\[\\\\(.*)\\]\\}\\}");

    protected static final Pattern templateSqlParamPattern = Pattern.compile("\\@\\{((\\\\}|[^}])*)\\}");

    protected String templateStr ;

    protected List<TemplateArray> templateArrays ;
    protected List<TemplateParam> templateParams ;
    protected List<SqlParam> sqlParams ;


    public Template(String templateStr){
        this.templateStr = templateStr ;
        this.templateArrays = new ArrayList<>();
        this.templateParams = new ArrayList<>();
        this.sqlParams = new ArrayList<>();

    }


    public void parsing() {
        String tempStr = templateStr ;

        tempStr = parsingArray(tempStr);

        for(TemplateArray templateArray : templateArrays){
            templateArray.parsing();
        }

        Matcher templateParamMatcher = templateParamPattern.matcher(tempStr);
        while (templateParamMatcher.find()) {
            templateParams.add(new TemplateParam(templateParamMatcher.group(0)));
        }

        Matcher sqlParamMatcher = templateSqlParamPattern.matcher(tempStr);
        while (sqlParamMatcher.find()) {
            sqlParams.add(new SqlParam(sqlParamMatcher.group(0)));
        }
//        System.out.println(templateParams);
    }



    protected String parsingArray(String tempStr) {
        Matcher templateArrayEscapeMatcher = templateArrayStartPattern.matcher(tempStr);
        if (templateArrayEscapeMatcher.find()) {
            TemplateArray templateArray = new TemplateArray(templateArrayEscapeMatcher.group(0)) ;
            String templateArrayStr = StringUtils.substringBetween(tempStr, templateArray.getStart(), templateArray.getEnd()) ;
            templateArray.setBody(templateArrayStr) ;
            tempStr = StringUtils.replace(tempStr, templateArray.getTemplateStr(), "") ;
            templateStr = StringUtils.replace(templateStr, templateArray.getTemplateStr(), templateArray.getReplaceStr()) ;

            templateArrays.add(templateArray);
            return parsingArray(tempStr) ;
        }
        return tempStr;
    }

    public String getTemplateStr() {
        return templateStr;
    }


    public String format(Map<String, Object> data) {
        String resultStr = templateStr ;
        for(TemplateArray templateArray : templateArrays){
            resultStr = StringUtils.replace(resultStr, templateArray.getReplaceStr(), templateArray.format(data)) ;
        }

        for(TemplateParam templateParam : templateParams){
            resultStr = StringUtils.replace(resultStr, templateParam.getTemplateStr(), templateParam.format(data)) ;
        }

        for(SqlParam sqlParam : sqlParams){
            resultStr = StringUtils.replace(resultStr, sqlParam.getTemplateStr(), "?") ;
        }

        return resultStr ;
    }

    public String format(Map<String, Object> data, ReadContext readContext, NodeType nodeType, Node node) {
        String resultStr = templateStr ;
//        for(TemplateArray templateArray : templateArrays){
//            resultStr = StringUtils.replace(resultStr, templateArray.getReplaceStr(), templateArray.format(data)) ;
//        }

        for(TemplateParam templateParam : templateParams){
            resultStr = StringUtils.replace(resultStr, templateParam.getTemplateStr(), templateParam.format(data, readContext, nodeType, node)) ;
        }

        for(SqlParam sqlParam : sqlParams){
            resultStr = StringUtils.replace(resultStr, sqlParam.getTemplateStr(), "?") ;
        }

        return resultStr ;
    }


    public List<Map<String, Object>> getArray(Map<String, Object> data, String key) {
        if(StringUtils.contains(key, '.')){
            String pre = StringUtils.substringBefore(key, ".");
            String pos = StringUtils.substringAfter(key, ".");

            Object value = data.get(pre) ;
            if( value != null && value instanceof Map){
                Map<String, Object> subData = (Map<String, Object>)value ;
                return getArray(subData, pos) ;
            }else if(value != null && value instanceof List){
                return (List<Map<String, Object>>) value;
            }else if(value == null){
                return null;
            }
        }else{
            Object value = data.get(key) ;
            if(value != null && value instanceof List){
                return (List<Map<String, Object>>) value;
            }
        }
        return null ;
    }

    public boolean hasTemplateArrayTag(String tag){
        for(TemplateArray templateArray : templateArrays){
            if(templateArray.getStart().equals(tag))
                return true ;
        }
        return false ;
    }

    public Object[] getSqlParameterValues(Map<String, Object> data)  {
        if(this.sqlParams == null || this.sqlParams.size() == 0){
            return new Object[0] ;
        }
        List<Object> values = new ArrayList<Object> () ;
        for(SqlParam param : this.sqlParams){
            values.add(param.getValue(data)) ;
        }
        return values.toArray();
    }


}
