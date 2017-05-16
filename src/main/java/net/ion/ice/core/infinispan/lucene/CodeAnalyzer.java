package net.ion.ice.core.infinispan.lucene;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;

/**
 * Created by jaeho on 2017. 5. 16..
 */
public class CodeAnalyzer extends Analyzer {

    /**
     * Creates a new {@link WhitespaceAnalyzer}
     */
    public CodeAnalyzer() {
    }

    @Override
    protected TokenStreamComponents createComponents(final String fieldName) {
        return new TokenStreamComponents(new CodeTokenizer());
    }
}
