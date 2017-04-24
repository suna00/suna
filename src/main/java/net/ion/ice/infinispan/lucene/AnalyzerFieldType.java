package net.ion.ice.infinispan.lucene;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.FieldType;

/**
 * Created by jaeho on 2017. 4. 21..
 */
public class AnalyzerFieldType extends FieldType{
    private Analyzer analyzer ;


    public Analyzer getAnalyzer() {
        return analyzer;
    }

    public void setAnalyzer(Analyzer analyzer) {
        this.analyzer = analyzer;
    }
}
