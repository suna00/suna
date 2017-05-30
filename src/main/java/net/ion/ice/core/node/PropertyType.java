package net.ion.ice.core.node;

import net.ion.ice.core.infinispan.lucene.AnalyzerFactory;
import org.apache.lucene.analysis.Analyzer;

/**
 * Created by jaeho on 2017. 5. 15..
 */
public class PropertyType {
    public static final String PROPERTYTYPE = "propertyType";


    public enum ValueType { STRING, CODE, DATE, LONG, INT, DOUBLE, BOOLEAN, TEXT}
    public enum AnalyzerType {simple, code, whitespace, standard, cjk, korean}


    public static final String INDEXING = "indexing";
    public static final String ANALYZER = "analyzer";
    public static final String IDABLE = "idable";
    public static final String SEARCHABLE = "searchable";
    public static final String LABELABLE = "labelable";
    public static final String REQUIRED = "required";

    public static final String PID = "pid";

    private Node propertyTypeNode ;

    public PropertyType(Node propertyType) {
        this.propertyTypeNode = propertyType ;
    }

    public boolean indexing() {
        return propertyTypeNode.getBooleanValue(INDEXING) ;
    }

    public String getAnalyzer() {
        return (String) propertyTypeNode.get(ANALYZER);
    }

    public AnalyzerType getAnalyzerType() {
        return AnalyzerType.valueOf(getAnalyzer()) ;
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

    public String getPid() {
        return propertyTypeNode.getStringValue(PID);
    }

    public ValueType getValueType(){
        String valueTypeStr = (String) propertyTypeNode.get("valueType");
        return ValueType.valueOf(valueTypeStr) ;
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

}
