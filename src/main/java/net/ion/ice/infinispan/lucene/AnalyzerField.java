package net.ion.ice.infinispan.lucene;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;

/**
 * Created by jaeho on 2017. 4. 21..
 */
public class AnalyzerField extends Field{

    public AnalyzerField(String name, String value, FieldType type) {
        super(name, value, type);
    }

    @Override
    public TokenStream tokenStream(Analyzer analyzer, TokenStream reuse) {
       if(fieldType() instanceof AnalyzerFieldType){
           analyzer = ((AnalyzerFieldType) fieldType()).getAnalyzer() ;
       }
       return super.tokenStream(analyzer, reuse) ;
    }
}
