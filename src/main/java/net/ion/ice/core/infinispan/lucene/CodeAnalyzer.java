package net.ion.ice.core.infinispan.lucene;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.*;

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
        final Tokenizer source = new CodeTokenizer();
        return new TokenStreamComponents(source, new LowerCaseFilter(source));
    }
}
