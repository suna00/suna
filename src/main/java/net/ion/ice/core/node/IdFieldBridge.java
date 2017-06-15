package net.ion.ice.core.node;

import net.ion.ice.core.infinispan.lucene.AnalyzerFactory;
import net.ion.ice.core.infinispan.lucene.AnalyzerField;
import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.DocValuesType;
import org.apache.lucene.index.IndexOptions;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.LuceneOptions;

import java.util.Date;

/**
 * Created by jaeho on 2017. 6. 14..
 */
public class IdFieldBridge implements FieldBridge{

    @Override
    public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
        if(value == null) return ;
        if(value instanceof Long) {
            document.add(new org.apache.lucene.document.LongField(name, (Long) value, numericFieldType()));
        }else if(value instanceof  String){
            Field field = new AnalyzerField(name, value.toString(), noAnalyzer()) ;
            document.add(field);
        }else if(value instanceof Integer){
            document.add(new org.apache.lucene.document.LongField(name, ((Integer) value).longValue(), numericFieldType()));
        }else if(value instanceof Double){
            document.add(new org.apache.lucene.document.LongField(name, ((Double) value).longValue(), numericFieldType()));
        }else if(value instanceof Date){
            Field field = new AnalyzerField(name, DateTools.dateToString((Date) value, DateTools.Resolution.SECOND), noAnalyzer()) ;
            document.add(field);
        }else{
            Field field = new AnalyzerField(name, value.toString(), noAnalyzer()) ;
            document.add(field);
        }
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
        fieldType.setNumericType(FieldType.NumericType.LONG);
        fieldType.setDocValuesType(DocValuesType.SORTED_NUMERIC);
        fieldType.setTokenized(false);
        fieldType.setOmitNorms(false);
        fieldType.freeze();
        return fieldType;
    }
}
