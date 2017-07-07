package net.ion.ice.core.context;


import org.apache.commons.lang3.StringUtils;

import java.text.ParseException;
import java.util.List;
import java.util.Map;

/**
 * Created by jaehocho on 2016. 12. 1..
 */
public class TemplateArray extends Template {
//    protected static final Pattern templateInnerArrayEscapePattern = Pattern.compile("\\{\\[\\{\\[:(.|\\n)*?\\]\\}\\]\\}");
    private String tag ;
    private String start ;
    private String end ;

    public TemplateArray(String tagStr){
        super(null);
        this.tag = StringUtils.substringBetween(tagStr, "{{[:", "]}}");
        this.start = tagStr ;
        this.end = StringUtils.replace(tagStr, ":", "/");
    }


//    public String format(Node node) throws ParseException {
//        LIST list = (LIST) NodeUtils.getField(node, tag);
//        String resultStr = "" ;
//
//        for(Node subNode : list.getAllNodes()) {
//            String subResult = templateStr ;
//            for (TemplateArray templateArray : templateArrays) {
//                subResult = StringUtils.replace(subResult, templateArray.getReplaceStr(), templateArray.format(subNode));
//            }
//
//            for (TemplateParam templateParam : templateParams) {
//                subResult = StringUtils.replace(subResult, templateParam.getTemplateStr(), templateParam.format(subNode));
//            }
//            resultStr += subResult ;
//        }
//
//        return resultStr ;
//    }

    public String format(Map<String, Object> data) throws ParseException {
        List<Map<String, Object>> list = getArray(data, tag) ;

        String resultStr = "" ;

        if(list == null) return resultStr ;

        for(Map<String, Object> subData : list) {
            String subResult = templateStr ;
            for (TemplateArray templateArray : templateArrays) {
                subResult = StringUtils.replace(subResult, templateArray.getTemplateStr(), templateArray.format(subData));
            }

            for (TemplateParam templateParam : templateParams) {
                subResult = StringUtils.replace(subResult, templateParam.getTemplateStr(), templateParam.format(subData));
            }
            resultStr += subResult ;
        }

        return resultStr ;
    }

    public String getReplaceStr() {
        return StringUtils.replaceChars(StringUtils.replaceChars(this.start, "{", "["), "}", "]") + " " +
                StringUtils.replaceChars(StringUtils.replaceChars(this.end, "{", "["), "}", "]") ;
    }


    public String getTemplateStr() {
        return this.start + this.templateStr + this.end ;
    }

//    public String getReplaceStr() {
//        if(this.templateStr.contains("{{[:")){
//            return StringUtils.replace(StringUtils.replace(this.templateStr, "{{[:", ""), "]}}" ,"");
//        }else{
//            return StringUtils.replace(StringUtils.replace(this.templateStr, "{[{[:", ""), "]}]}" ,"");
//        }
//    }

    public String getStart() {
        return start;
    }

    public String getEnd() {
        return end;
    }

    public void setBody(String body) {
        this.templateStr = body;
    }
}
