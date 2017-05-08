package net.ion.ice.core.node;

import net.ion.ice.ApplicationContextManager;
import net.ion.ice.core.infinispan.lucene.AnalyzerField;
import net.ion.ice.core.infinispan.lucene.AnalyzerFieldType;
import net.ion.ice.core.infinispan.lucene.LuceneAnalyzerFactory;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.DocValuesType;
import org.apache.lucene.index.IndexOptions;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.LuceneOptions;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

/**
 * Created by jaeho on 2017. 3. 31..
 */
public class PropertiesFieldBridge implements FieldBridge {
    @Autowired
    private NodeService nodeService ;

    public PropertiesFieldBridge(){
        nodeService = (NodeService) ApplicationContextManager.getContext().getBean("nodeService");
        System.out.println("INIT FIELD");
    }

    @Override
    public void set(String fieldName, Object value, Document document, LuceneOptions luceneOptions) {
        if ( value != null ) {
            indexNotNullMap( fieldName, value, document, luceneOptions );
        }
    }
    private void indexNotNullMap(String name, Object value, Document document, LuceneOptions luceneOptions) {
        Map<String, Object> properties = (Map<String, Object>) value;
        String tid = (String) properties.get("tid") ;
        Node nodeType = nodeService.getNodeType(tid) ;
        if(nodeType == null){
            for (Map.Entry<String, Object> entry : properties.entrySet() ) {
                indexEntry(entry, document, luceneOptions );
            }
        }else{
            for (Map.Entry<String, Object> entry : properties.entrySet() ) {
                Node propertyType = nodeService.getPropertyType(tid, entry.getKey()) ;
                indexEntry(entry, document, luceneOptions, propertyType );
            }
        }

    }

    private void indexEntry(Map.Entry<String, Object> entry, Document document, LuceneOptions luceneOptions, Node propertyType) {
        if(propertyType == null) return ;
        String pid  = entry.getKey() ;

        String indexType = (String) propertyType.get("indexType");
        if(indexType == null){
            return ;
        }

        switch (indexType) {
            case "noAnalyzer" :{
                document.add(new org.apache.lucene.document.Field(pid, entry.getValue().toString(), noAnalyzer()));
                break;
            }
            case "long" :{
                document.add(new org.apache.lucene.document.LongField(pid, (Long) entry.getValue(), numericFieldType()));
                break;
            }
            case "simple" :{
                Field field = new AnalyzerField(pid, entry.getValue().toString(), simpleAnalyzer()) ;
                document.add(field);
                break;
            }
        }


    }

    private void indexEntry(Map.Entry<String, Object> entry, Document document, LuceneOptions luceneOptions) {
        String pid  = entry.getKey() ;
        if(pid.equals("id")) return  ;

        Object value = entry.getValue() ;
        if(value == null) return ;
        if(value instanceof Long){
            document.add(new org.apache.lucene.document.LongField(pid, (Long) value, numericFieldType()));
        }else if(value instanceof Integer){
            document.add(new org.apache.lucene.document.IntField(pid, (Integer) value, numericFieldType()));
        }else{
            Field field = new AnalyzerField(pid, entry.getValue().toString(), simpleAnalyzer()) ;
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

    public static org.apache.lucene.document.FieldType noAnalyzer() {
        org.apache.lucene.document.FieldType fieldType = new org.apache.lucene.document.FieldType();
        fieldType.setIndexOptions(IndexOptions.DOCS_AND_FREQS);
        fieldType.setDocValuesType(DocValuesType.NONE);
        fieldType.setStored(false);
        fieldType.setTokenized(false);
        fieldType.setOmitNorms(false);
        fieldType.freeze();
        return fieldType;
    }

    public static org.apache.lucene.document.FieldType numericFieldType() {
        org.apache.lucene.document.FieldType fieldType = new org.apache.lucene.document.FieldType();
        fieldType.setIndexOptions(IndexOptions.DOCS_AND_FREQS);
        fieldType.setNumericType(FieldType.NumericType.INT);
        fieldType.setDocValuesType(DocValuesType.NONE);
        fieldType.setTokenized(false);
        fieldType.setOmitNorms(false);
        fieldType.freeze();
        return fieldType;
    }

    public static org.apache.lucene.document.FieldType simpleAnalyzer() {
        AnalyzerFieldType fieldType = new AnalyzerFieldType();
        fieldType.setIndexOptions(IndexOptions.DOCS_AND_FREQS);
        fieldType.setDocValuesType(DocValuesType.NONE);
        fieldType.setStored(false);
        fieldType.setTokenized(true);
        fieldType.setOmitNorms(false);
        fieldType.freeze();
        fieldType.setAnalyzer(LuceneAnalyzerFactory.simpleAnalyzer);
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
