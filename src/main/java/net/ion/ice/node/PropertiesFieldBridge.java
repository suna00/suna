package net.ion.ice.node;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.index.DocValuesType;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.util.BytesRef;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.bridge.MetadataProvidingFieldBridge;
import org.hibernate.search.bridge.builtin.NumericFieldBridge;
import org.hibernate.search.bridge.spi.FieldMetadataBuilder;
import org.hibernate.search.bridge.spi.FieldType;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Created by jaeho on 2017. 3. 31..
 */
public class PropertiesFieldBridge implements FieldBridge {
    @Autowired
    private NodeService nodeService ;

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
        Map<String, Object> properties = (Map<String, Object>) value;
        Node nodeType = nodeService.getNodeType((String) properties.get("tid")) ;


        for (Map.Entry<String, Object> entry : properties.entrySet() ) {
            indexEntry(entry, document, luceneOptions );
        }
    }

    private void indexEntry(Map.Entry<String, Object> entry, Document document, LuceneOptions luceneOptions) {
        String key  = entry.getKey() ;
        if(key.equals("id")) return  ;

        Object value = entry.getValue() ;

        if (!StringUtils.isEmpty(stringValue)) {
            if (pt.isAnalyze()) {
                document.add(new org.apache.lucene.document.Field(key, stringValue, fieldType(pt)));
            } else {
                document.add(new org.apache.lucene.document.Field(key, new BytesRef(stringValue), fieldType(pt)));
            }
        }

        if (!StringUtils.isEmpty(displayStringValue)) {
            if (pt.isAnalyze()) {
                document.add(new org.apache.lucene.document.Field(key, displayStringValue, fieldType(pt)));
            } else {
                document.add(new org.apache.lucene.document.Field(key, new BytesRef(displayStringValue), fieldType(pt)));
            }
        }

        if (pt.isI18n() && langs != null) {
            List<String> langList = Arrays.asList(StringUtils.split(langs, ","));
            for (String lang : langList) {
                String i18nStringValue = field.getIndexingI18nValue(lang);
                if (!StringUtils.isEmpty(i18nStringValue)) {
                    if (pt.isAnalyze()) {
                        document.add(new org.apache.lucene.document.Field(key, i18nStringValue, fieldType(pt)));
                    } else {
                        document.add(new org.apache.lucene.document.Field(key, new BytesRef(i18nStringValue), fieldType(pt)));
                    }
                }
            }
        }
        FieldType fieldType = new FieldType();
        Field field = new Field( );
        document.add(field);
//        luceneOptions.addFieldToDocument(entry.getKey().toString(), entry.getValue().toString(), document);
    }

    public static org.apache.lucene.document.FieldType fieldType(PropertyType pt) {
        org.apache.lucene.document.FieldType fieldType = new org.apache.lucene.document.FieldType();
        fieldType.setIndexOptions(IndexOptions.DOCS_AND_FREQS);
        if (!pt.isAnalyze() && !LuceneUtils.isClob(pt)) fieldType.setDocValuesType(DocValuesType.SORTED_SET);
        fieldType.setStored(pt.isStore());
        fieldType.setTokenized(pt.isAnalyze());
        fieldType.setOmitNorms(false);
        fieldType.freeze();
        return fieldType;
    }

    public static org.apache.lucene.document.FieldType numericFieldType() {
        org.apache.lucene.document.FieldType fieldType = new org.apache.lucene.document.FieldType();
        fieldType.setIndexOptions(IndexOptions.DOCS_AND_FREQS);
        fieldType.setNumericType(org.apache.lucene.document.FieldType.NumericType.LONG);
        fieldType.setDocValuesType(DocValuesType.SORTED_NUMERIC);
        fieldType.setTokenized(false);
        fieldType.setOmitNorms(false);
        fieldType.freeze();
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
