package net.ion.ice.core.context;

import org.apache.commons.lang3.StringUtils;

import java.text.ParseException;
import java.util.List;
import java.util.Map;

/**
 * Created by jaeho on 2017. 7. 13..
 */
public class SqlParam extends TemplateParam{

    public SqlParam(String templateStr){
        this.templateStr = templateStr ;
        this.paramStr = StringUtils.substringBetween(templateStr, "@{", "}").trim() ;
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

}
