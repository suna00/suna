package net.ion.ice.core.node;

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
}
