package net.ion.ice.core.node;

/**
 * Created by jaeho on 2017. 5. 15..
 */
public class PropertyType extends Node {
    public static final String PROPERTYTYPE = "propertyType";


    public static final String INDEXING = "indexing";
    public static final String ANALYZER = "analyzer";
    public static final String IDABLE = "idable";
    public static final String PID = "pid";

    public PropertyType(Object id, String typeId) {
        super(id, typeId);
    }

    public boolean indexing() {
        return getBooleanValue(INDEXING) ;
    }

    public String getAnalyzer() {
        return (String) get(ANALYZER);
    }

    public boolean isIdable() {
        return getBooleanValue(IDABLE) ;
    }

    public String getPid() {
        return getStringValue(PID);
    }
}
