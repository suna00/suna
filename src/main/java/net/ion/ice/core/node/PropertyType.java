package net.ion.ice.core.node;

import infinispan.com.mchange.lang.ObjectUtils;
import net.ion.ice.core.infinispan.lucene.AnalyzerFactory;
import org.apache.lucene.analysis.Analyzer;

import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by jaeho on 2017. 5. 15..
 */
public class PropertyType {
    public static final String PROPERTYTYPE = "propertyType";
    public static final String DEFAULT_VALUE = "defaultValue";
    public static final String ID_TYPE = "idType";
    public static final String VALUE_TYPE = "valueType";
    public static final String REFERENCE_TYPE = "referenceType";


    public enum ValueType { STRING, CODE, DATE, LONG, INT, DOUBLE, BOOLEAN, REFERENCED, REFERENCE, TEXT, ARRAY}
    public enum AnalyzerType {simple, code, whitespace, standard, cjk, korean}
    public enum IdType {autoIncrement, UUID}


    public static final String INDEXABLE = "indexable";
    public static final String ANALYZER = "analyzer";
    public static final String IDABLE = "idable";
    public static final String SEARCHABLE = "searchable";
    public static final String LABELABLE = "labelable";
    public static final String REQUIRED = "required";
    public static final String TREEABLE = "treeable";

    public static final String PID = "pid";

    private Node propertyTypeNode ;

    private Map<Object, Code> codeMap ;

    public PropertyType(Node propertyType) {
        this.propertyTypeNode = propertyType ;
    }

    public boolean isIndexable() {
        return propertyTypeNode.getBooleanValue(INDEXABLE) ;
    }

    public String getAnalyzer() {
        return (String) propertyTypeNode.get(ANALYZER);
    }

    public AnalyzerType getAnalyzerType() {
        try {
            return AnalyzerType.valueOf(getAnalyzer());
        }catch(NullPointerException e){
            return null ;
        }
    }

    public Analyzer getLuceneAnalyzer(){
        AnalyzerType analyzerType = getAnalyzerType() ;
        if(analyzerType == null){
            if(isIdable()){
                analyzerType = PropertyType.AnalyzerType.code ;
            }else if(isLabelable()){
                analyzerType =  PropertyType.AnalyzerType.cjk ;
            }else{
                switch (getValueType()) {
                    case CODE:{
                        analyzerType =  PropertyType.AnalyzerType.code ;
                        break ;
                    }
                    case TEXT:{
                        analyzerType = PropertyType.AnalyzerType.standard ;
                        break ;
                    }
                    default :{
                        analyzerType = PropertyType.AnalyzerType.simple ;
                        break ;
                    }
                }
            }
        }
        return AnalyzerFactory.getAnalyzer(analyzerType) ;
    }

    public boolean isIdable() {
        return propertyTypeNode.getBooleanValue(IDABLE) ;
    }

    public IdType getIdType() {
        Object idType =  propertyTypeNode.get(ID_TYPE);
        if(idType instanceof IdType) return (IdType) idType;
        return IdType.valueOf((String) idType);
    }

    public String getPid() {
        return propertyTypeNode.getStringValue(PID);
    }

    public ValueType getValueType(){
        try {
            String valueTypeStr = (String) propertyTypeNode.get(VALUE_TYPE);
            return ValueType.valueOf(valueTypeStr);
        }catch(Exception e){
            System.out.println("VALUE TYPE ERROR : " +  propertyTypeNode.get(VALUE_TYPE) ) ;
            System.out.println("VALUE TYPE ERROR : " +  propertyTypeNode) ;
        }
        return null ;
    }

    public boolean isSeacheable() {
        return propertyTypeNode.getBooleanValue(SEARCHABLE) ;
    }

    public boolean isLabelable() {
        return propertyTypeNode.getBooleanValue(LABELABLE) ;
    }

    public boolean isRequired(){
        return propertyTypeNode.getBooleanValue(IDABLE) || propertyTypeNode.getBooleanValue(REQUIRED) ;
    }

    public boolean isReferenced() {
        return getValueType() == ValueType.REFERENCED ;
    }

    public String getReferenceType() {
        return propertyTypeNode.getStringValue(REFERENCE_TYPE);

    }

    public boolean isTreeable() {
        return propertyTypeNode.getBooleanValue(TREEABLE);
    }

    public boolean hasDefaultValue() {
        return !propertyTypeNode.isNullValue(DEFAULT_VALUE) ;
    }

    public Object getDefaultValue() {
        return propertyTypeNode.getValue(DEFAULT_VALUE) ;
//        return propertyTypeNode.getValue(DEFAULT_VALUE, getValueType()) ;
    }

    public Map<Object, Code> getCode() {
        if(codeMap == null) {
            codeMap = new HashMap<>() ;
            Collection<Map<String, Object>> codeValues = (Collection<Map<String, Object>>) propertyTypeNode.get("code");
            for(Map<String, Object> codeValue : codeValues){
                codeMap.put(codeValue.get("value"), new Code(codeValue)) ;
            }
        }
        return codeMap ;
    }

}
