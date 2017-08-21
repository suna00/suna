package net.ion.ice.core.node;

import java.io.Serializable;
import java.util.Map;

/**
 * Created by jaeho on 2017. 6. 5..
 */
public class Code implements Serializable{
    protected Object value ;
    protected String label ;

    public Code (){
    }

    public Code(Object value, String label){
        this.value = value ;
        this.label = label ;
    }

    public Code(Map<String, Object> codeValue) {
        this.value = codeValue.get("value") ;
        this.label = (String) codeValue.get("label");
    }

    public String getLabel() {
        return label;
    }

    public Object getValue() {
        return value;
    }

    public String toString(){
        return value + " " +  label ;
    }
}
