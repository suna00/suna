package net.ion.ice.core.node;

import net.ion.ice.core.infinispan.lucene.AnalyzerFactory;
import net.ion.ice.core.infinispan.lucene.AnalyzerField;
import net.ion.ice.core.infinispan.lucene.AnalyzerFieldType;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.DocValuesType;
import org.apache.lucene.index.IndexOptions;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.LuceneOptions;

import java.util.Date;
import java.util.Map;

/**
 * Created by jaeho on 2017. 3. 31..
 */
public class PropertiesFieldBridge implements FieldBridge {

    public PropertiesFieldBridge(){
        System.out.println("INIT FIELD");
    }

    @Override
    public void set(String fieldName, Object value, Document document, LuceneOptions luceneOptions) {
        if ( value != null ) {
            indexNotNullMap( fieldName, value, document, luceneOptions );
        }
    }
    private void indexNotNullMap(String name, Object value, Document document, LuceneOptions luceneOptions) {
        Properties properties = (Properties) value;
        String typeId = properties.getTypeId() ;
        NodeType nodeType = NodeUtils.getNodeType(typeId) ;
        if(nodeType == null){
            for (Map.Entry<String, Object> entry : properties.entrySet() ) {
                indexEntry(entry, document, luceneOptions );
            }
        }else{
            for (Map.Entry<String, Object> entry : properties.entrySet() ) {
                PropertyType propertyType = nodeType.getPropertyType(entry.getKey()) ;
                indexEntry(entry, document, luceneOptions, propertyType );
            }
        }

    }

    private void indexEntry(Map.Entry<String, Object> entry, Document document, LuceneOptions luceneOptions, PropertyType propertyType) {
        if(propertyType == null) return ;
        String pid  = entry.getKey() ;

        if(!propertyType.isIndexable()) return ;

        PropertyType.ValueType valueType = propertyType.getValueType() ;

        switch (valueType) {
            case LONG :{
                document.add(new org.apache.lucene.document.LongField(pid, NodeUtils.getLongValue(entry.getValue()), numericFieldType(valueType)));
                break;
            }
            case INT :{
                document.add(new org.apache.lucene.document.IntField(pid, NodeUtils.getIntValue(entry.getValue()), numericFieldType(valueType)));
                break;
            }
            case DOUBLE :{
                document.add(new org.apache.lucene.document.DoubleField(pid, NodeUtils.getDoubleValue(entry.getValue()), numericFieldType(valueType)));
                break;
            }
            case DATE :{
                document.add(new org.apache.lucene.document.LongField(pid,NodeUtils.getDateLongValue(entry.getValue()), numericFieldType(PropertyType.ValueType.LONG)));
                break;
            }
//            case CODE :{
//                Field field = new AnalyzerField(pid, entry.getValue().to,  fieldAnalyzer(propertyType.getLuceneAnalyzer())) ;
//                document.add(field);
//            }
            default:{
//                Field field = new AnalyzerField(pid, entry.getValue().toString(), propertyType.isSorted() ? sortedFieldAnalyzer(propertyType.getLuceneAnalyzer()) : fieldAnalyzer(propertyType.getLuceneAnalyzer())) ;
                if(propertyType.isI18n()){
                    if(entry.getValue() instanceof Map){
                        Map<String, Object> i18nMap = (Map<String, Object>) entry.getValue();
                        for(String key : i18nMap.keySet()){
                            document.add(getKeywordField(propertyType, pid + "_" + key, i18nMap.get(key).toString()));
                        }
                    }
                }else {
                    document.add(getKeywordField(propertyType, pid, entry.getValue().toString()));
                }
                break;
            }
        }
    }

    private Field getKeywordField(PropertyType propertyType, String key, String value){
        if(propertyType.isSorted()) {
            return new AnalyzerField(key, value, sortedFieldAnalyzer()) ;
        }else{
            return new AnalyzerField(key, value, fieldAnalyzer(propertyType.getLuceneAnalyzer()));
        }

    }

    private void indexEntry(Map.Entry<String, Object> entry, Document document, LuceneOptions luceneOptions) {
        String pid  = entry.getKey() ;
        Object value = entry.getValue() ;
        if(value == null) return ;
        if(value instanceof Long){
            document.add(new org.apache.lucene.document.LongField(pid, (Long) value, numericFieldType(PropertyType.ValueType.LONG)));
        }else if(value instanceof Integer){
            document.add(new org.apache.lucene.document.IntField(pid, (Integer) value, numericFieldType(PropertyType.ValueType.INT)));
        }else if(value instanceof Double){
            document.add(new org.apache.lucene.document.DoubleField(pid, (Double) value, numericFieldType(PropertyType.ValueType.DOUBLE)));
        }else if(value instanceof Date){
            document.add(new org.apache.lucene.document.LongField(pid, NodeUtils.getDateLongValue(value), numericFieldType(PropertyType.ValueType.LONG)));
        }else{

            Field field = new AnalyzerField(pid, entry.getValue().toString(), fieldAnalyzer(AnalyzerFactory.getAnalyzer(PropertyType.AnalyzerType.code))) ;
            document.add(field);
        }

//        if (!StringUtils.isEmpty(stringValue)) {
//            if (pt.isAnalyze()) {
//                document.add(new org.apache.lucene.document.Field(key, stringValue, fieldType(pt)));
//            } else {
//                document.add(new org.apache.lucene.document.Field(key, new BytesRef(stringValue), fieldType(pt)));
//            }
//        }
//
//        if (!StringUtils.isEmpty(displayStringValue)) {
//            if (pt.isAnalyze()) {
//                document.add(new org.apache.lucene.document.Field(key, displayStringValue, fieldType(pt)));
//            } else {
//                document.add(new org.apache.lucene.document.Field(key, new BytesRef(displayStringValue), fieldType(pt)));
//            }
//        }
//
//        if (pt.isI18n() && langs != null) {
//            List<String> langList = Arrays.asList(StringUtils.split(langs, ","));
//            for (String lang : langList) {
//                String i18nStringValue = field.getIndexingI18nValue(lang);
//                if (!StringUtils.isEmpty(i18nStringValue)) {
//                    if (pt.isAnalyze()) {
//                        document.add(new org.apache.lucene.document.Field(key, i18nStringValue, fieldType(pt)));
//                    } else {
//                        document.add(new org.apache.lucene.document.Field(key, new BytesRef(i18nStringValue), fieldType(pt)));
//                    }
//                }
//            }
//        }
//        FieldType fieldType = new FieldType();
//        Field field = new Field( );
//        document.add(field);
//        luceneOptions.addFieldToDocument(entry.getKey().toString(), entry.getValue().toString(), document);
    }


    public static org.apache.lucene.document.FieldType numericFieldType(PropertyType.ValueType valueType) {
        AnalyzerFieldType fieldType = new AnalyzerFieldType();
        fieldType.setIndexOptions(IndexOptions.DOCS_AND_FREQS);
        fieldType.setNumericType(FieldType.NumericType.valueOf(valueType.toString()));
        fieldType.setDocValuesType(DocValuesType.NONE);
        fieldType.setStored(false);
        fieldType.setTokenized(false);
        fieldType.setOmitNorms(false);
        fieldType.freeze();
        fieldType.setAnalyzer(AnalyzerFactory.getAnalyzer(PropertyType.AnalyzerType.code));
        return fieldType;
    }

    public static org.apache.lucene.document.FieldType fieldAnalyzer(Analyzer analyzer) {
        AnalyzerFieldType fieldType = new AnalyzerFieldType();
        fieldType.setIndexOptions(IndexOptions.DOCS_AND_FREQS);
        fieldType.setDocValuesType(DocValuesType.NONE);
        fieldType.setStored(false);
        fieldType.setTokenized(true);
        fieldType.setOmitNorms(false);
        fieldType.freeze();
        fieldType.setAnalyzer(analyzer);
        return fieldType;
    }


    public static org.apache.lucene.document.FieldType sortedFieldAnalyzer() {
        AnalyzerFieldType fieldType = new AnalyzerFieldType();
        fieldType.setIndexOptions(IndexOptions.DOCS_AND_FREQS);
        fieldType.setDocValuesType(DocValuesType.NONE);
        fieldType.setStored(false);
        fieldType.setTokenized(false);
        fieldType.setOmitNorms(false);
        fieldType.freeze();
        fieldType.setAnalyzer(AnalyzerFactory.getAnalyzer(PropertyType.AnalyzerType.simple));
        return fieldType;
    }

//    @Override
//    public void set(String name, Object value, Document document, LuceneOptions luceneOps) {
//
//        Map<String, String> map = (Map<String, String>) value;
//
//        String firstName = map.get( "firstName" );
//        String lastName = map.get( "lastName" );
//
//        // add regular document fields
//        luceneOps.addFieldToDocument( name + "_firstName", lastName, document );
//        luceneOps.addFieldToDocument( name + "_lastName", lastName, document );
//        luceneOptions.addNumericFieldToDocument(name + COUNT_SUFFIX, size, document);
//        document.add(new NumericDocValuesField(name + COUNT_SUFFIX, size.longValue()));
//
//        // add doc value fields to allow for sorting
//        document.addSortedDocValuesFieldToDocument( name + "_firstName", firstName );
//        document.addSortedDocValuesFieldToDocument( name + "_lastName", lastName );
//    }

}
