package net.ion.ice.core.node;

import net.ion.ice.core.infinispan.lucene.AnalyzerFactory;
import org.apache.lucene.analysis.Analyzer;
import org.stagemonitor.util.StringUtils;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by jaeho on 2017. 5. 15..
 */
public class PropertyType implements Serializable{
    public static final String PROPERTYTYPE = "propertyType";
    public static final String DEFAULT_VALUE = "defaultValue";
    public static final String ID_TYPE = "idType";
    public static final String VALUE_TYPE = "valueType";
    public static final String REFERENCE_TYPE = "referenceType";
    public static final String REFERENCE_VALUE = "referenceValue";
    public static final String REFERENCE_VIEW = "referenceView";
    public static final String CODE_FILTER = "codeFilter";
    public static final String REFERENCED_FILTER = "referencedFilter";

    public static final String FILE_HANDLER = "fileHandler";
    public static final String SORTABLE = "sortable";


    public enum ValueType {STRING, CODE, DATE, LONG, INT, DOUBLE, BOOLEAN, REFERENCED, REFERENCE, REFERENCES, TEXT, ARRAY, OBJECT, JSON, FILE, FILES, CONTENT}

    public enum AnalyzerType {simple, code, whitespace, standard, cjk, korean}

    public enum IdType {autoIncrement, UUID}


    public static final String INDEXABLE = "indexable";
    public static final String ANALYZER = "analyzer";
    public static final String IDABLE = "idable";
    public static final String SEARCHABLE = "searchable";
    public static final String LABELABLE = "labelable";
    public static final String REQUIRED = "required";
    public static final String TREEABLE = "treeable";
    public static final String IGNORE_HIERARCHY_VALUE = "ignoreHierarchyValue";
    public static final String LENGTH = "length";

    public static final String I18N = "i18n";


    public static final String TID = "tid";
    public static final String PID = "pid";

    private Node propertyTypeNode;

    private Map<Object, Code> codeMap;

    public PropertyType(Node propertyType) {
        this.propertyTypeNode = propertyType;
    }

    public boolean isIndexable() {
        return propertyTypeNode.getBooleanValue(INDEXABLE);
    }

    public String getAnalyzer() {
        return (String) propertyTypeNode.get(ANALYZER);
    }

    public AnalyzerType getAnalyzerType() {
        try {
            return AnalyzerType.valueOf(getAnalyzer());
        } catch (NullPointerException e) {
            if (isIdable()) {
                return PropertyType.AnalyzerType.code;
            } else if (isLabelable()) {
                return PropertyType.AnalyzerType.cjk;
            } else {
                switch (getValueType()) {
                    case CODE: {
                        return PropertyType.AnalyzerType.code;
                    }
                    case TEXT: {
                        return PropertyType.AnalyzerType.standard;
                    }
                    default: {
                        return PropertyType.AnalyzerType.simple;
                    }
                }
            }
        }
    }


    public Analyzer getLuceneAnalyzer() {
        AnalyzerType analyzerType = getAnalyzerType();

        return AnalyzerFactory.getAnalyzer(analyzerType);
    }

    public boolean isSorted(){
        switch (getAnalyzerType()){
            case simple:
                return true ;
        }
        return false ;
    }


    public boolean isIdable() {
        return propertyTypeNode.getBooleanValue(IDABLE);
    }

    public IdType getIdType() {
        Object idType = propertyTypeNode.get(ID_TYPE);

        if (idType instanceof IdType) return (IdType) idType;
        return IdType.valueOf((String) idType);
    }

    public String getTid() {
        return propertyTypeNode.getStringValue(TID);
    }

    public String getPid() {
        return propertyTypeNode.getStringValue(PID);
    }

    public ValueType getValueType() {
        try {
            String valueTypeStr = (String) propertyTypeNode.get(VALUE_TYPE);
            return ValueType.valueOf(valueTypeStr);
        } catch (Exception e) {
            System.out.println("VALUE TYPE ERROR : " + propertyTypeNode.get(VALUE_TYPE));
            System.out.println("VALUE TYPE ERROR : " + propertyTypeNode);
        }
        return null;
    }

    public boolean isSeacheable() {
        return propertyTypeNode.getBooleanValue(SEARCHABLE);
    }

    public boolean isLabelable() {
        return propertyTypeNode.getBooleanValue(LABELABLE);
    }

    public boolean isRequired() {
        return propertyTypeNode.getBooleanValue(IDABLE) || propertyTypeNode.getBooleanValue(REQUIRED);
    }

    public boolean isReferenced() {
        return getValueType() == ValueType.REFERENCED;
    }

    public boolean isFile() {
        return getValueType() == ValueType.FILE;
    }


    public String getReferenceType() {
        return propertyTypeNode.getStringValue(REFERENCE_TYPE);
    }

    public String getReferenceValue() {
        return propertyTypeNode.getStringValue(REFERENCE_VALUE);
    }

    public String getCodeFilter() {
        return propertyTypeNode.getStringValue(CODE_FILTER);
    }

    public String getReferencedFilter() {
        return propertyTypeNode.getStringValue(REFERENCED_FILTER);
    }

    public boolean isTreeable() {
        return propertyTypeNode.getBooleanValue(TREEABLE);
    }

    public boolean isIgnoreHierarchyValue() {
        return propertyTypeNode.getBooleanValue(IGNORE_HIERARCHY_VALUE);
    }

    public boolean hasDefaultValue() {
        return !propertyTypeNode.isNullValue(DEFAULT_VALUE);
    }

    public Object getDefaultValue() {
        return propertyTypeNode.getValue(DEFAULT_VALUE);
//        return propertyTypeNode.getValue(DEFAULT_VALUE, getValueType()) ;
    }

    public Map<Object, Code> getCode() {
        if (codeMap == null) {
            codeMap = new HashMap<>();
            Collection<Map<String, Object>> codeValues = (Collection<Map<String, Object>>) propertyTypeNode.get("code");
            if(codeValues == null){
                codeValues =  (Collection<Map<String, Object>>) propertyTypeNode.get("code");
            }
            if(codeValues == null){
                return null ;
            }
            for (Map<String, Object> codeValue : codeValues) {
                codeMap.put(codeValue.get("value"), new Code(codeValue));
            }
        }
        return codeMap;
    }

    public String getFileHandler() {
        String handler = propertyTypeNode.getStringValue(FILE_HANDLER);
        if(StringUtils.isEmpty(handler)){
            return "default" ;
        }
        return handler ;
    }

    public Integer getLength() {
        Integer length = (Integer) propertyTypeNode.getValue(LENGTH);
        String valueTypeStr = (String) propertyTypeNode.get(VALUE_TYPE);

        if (length == null) {
            switch (ValueType.valueOf(valueTypeStr)) {
                case STRING:
                    return 1000;
                case BOOLEAN:
                    return 1;
                default:
                    return 500;
            }
        }
        return (Integer) propertyTypeNode.getValue(LENGTH);
    }

    public boolean isI18n() {
        return propertyTypeNode.getBooleanValue(I18N);
    }
    public boolean isReferenceView() {
        return propertyTypeNode.getBooleanValue(REFERENCE_VIEW);
    }

    public boolean isSortable() {
        return propertyTypeNode.getBooleanValue(SORTABLE);
    }

    public boolean isNumeric(){
        switch (getValueType()){
            case INT: case LONG: case DOUBLE: case DATE:
                return true ;
        }
        return false ;
    }

    public boolean isList() {
        switch (getValueType()){
            case ARRAY: case REFERENCED: case REFERENCES:
                return true ;
        }
        return false ;
    }

}
