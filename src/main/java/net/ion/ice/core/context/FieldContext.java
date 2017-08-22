package net.ion.ice.core.context;

import java.util.Map;

/**
 * Created by jaehocho on 2017. 8. 22..
 */
public class FieldContext extends ReadContext{

    public static FieldContext createContextFromOption(Map<String, Object> fieldOption){
        FieldContext context = new FieldContext() ;

        for(String key : fieldOption.keySet()){
            if(key.equals("field")) continue;

            switch (key){
                case "includeReferenced" :{
                    context.setIncludeReferenced((Boolean) fieldOption.get("includeReferenced"));
                    break ;
                }
                case "referenceView" :{
                    context.setReferenceView((Boolean) fieldOption.get("referenceView"));
                    break ;
                }
                default :{
                    break ;
                }

            }

        }
        return context ;
    }
}
