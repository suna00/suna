package net.ion.ice.core.infinispan.lucene;

import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.util.CharTokenizer;
import org.apache.lucene.util.AttributeFactory;

/**
 * Created by jaeho on 2017. 5. 16..
 */
public class CodeTokenizer extends CharTokenizer {

    /**
     * Construct a new WhitespaceTokenizer.
     */
    public CodeTokenizer() {
    }

    /**
     * Construct a new WhitespaceTokenizer using a given
     * {@link org.apache.lucene.util.AttributeFactory}.
     *
     * @param factory
     *          the attribute factory to use for this {@link Tokenizer}
     */
    public CodeTokenizer(AttributeFactory factory) {
        super(factory);
    }

    /** Collects only characters which do not satisfy
     * {@link Character#isWhitespace(int)}.*/
    @Override
    protected boolean isTokenChar(int c) {
        return ! (Character.isWhitespace(c) || c == ',') ;
    }
}

