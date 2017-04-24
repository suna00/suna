package net.ion.ice.infinispan;

import org.apache.lucene.analysis.cjk.CJKBigramFilterFactory;
import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.core.WhitespaceTokenizerFactory;
import org.apache.lucene.analysis.miscellaneous.WordDelimiterFilterFactory;
import org.apache.lucene.analysis.ngram.NGramFilterFactory;
import org.apache.lucene.analysis.standard.StandardTokenizerFactory;
import org.hibernate.search.annotations.*;

import javax.persistence.Transient;
import java.io.Serializable;

/**
 * Created by jaeho on 2017. 4. 21..
 */

@Indexed
@ProvidedId
@AnalyzerDefs({
        @AnalyzerDef(name = "ko",
                tokenizer = @TokenizerDef(factory = StandardTokenizerFactory.class),
                filters = {
//	  	  @TokenFilterDef(factory = StandardFilterFactory.class),
//	      @TokenFilterDef(factory = CJKWidthFilterFactory.class),
                        @TokenFilterDef(factory = WordDelimiterFilterFactory.class),
                        @TokenFilterDef(factory = LowerCaseFilterFactory.class),
//	  	  @TokenFilterDef(factory = StopFilterFactory.class),
                        @TokenFilterDef(factory = CJKBigramFilterFactory.class)
                }),
        @AnalyzerDef(name = "en",
                tokenizer = @TokenizerDef(factory = StandardTokenizerFactory.class),
                filters = {
//	  	  @TokenFilterDef(factory = StandardFilterFactory.class),
//	      @TokenFilterDef(factory = CJKWidthFilterFactory.class),
                        @TokenFilterDef(factory = WordDelimiterFilterFactory.class),
                        @TokenFilterDef(factory = LowerCaseFilterFactory.class),
//	  	  @TokenFilterDef(factory = StopFilterFactory.class),
                        @TokenFilterDef(factory = NGramFilterFactory.class)
                }),
        @AnalyzerDef(name = "str",
                tokenizer = @TokenizerDef(factory = WhitespaceTokenizerFactory.class),
                filters = {
//	  	  @TokenFilterDef(factory = StandardFilterFactory.class),
//	      @TokenFilterDef(factory = CJKWidthFilterFactory.class),
                        @TokenFilterDef(factory = LowerCaseFilterFactory.class)
//	  	  @TokenFilterDef(factory = StopFilterFactory.class),
                })
})
public class CacheTestData implements Serializable{

    @DocumentId
    @Field(analyze = Analyze.YES, indexNullAs=Field.DEFAULT_NULL_TOKEN, analyzer=@Analyzer(definition="str"))
    private String id ;

    @Field(analyze = Analyze.YES, indexNullAs=Field.DEFAULT_NULL_TOKEN, analyzer=@Analyzer(definition="ko"))
    private String text1 ;

    @Field(analyze = Analyze.YES, indexNullAs=Field.DEFAULT_NULL_TOKEN, analyzer=@Analyzer(definition="str"))
    private String string1 ;

    @Field(analyze = Analyze.NO, indexNullAs=Field.DEFAULT_NULL_TOKEN)
    private String value1 ;

    @Field(analyze = Analyze.NO, store = Store.NO)
    @NumericField
    private Long number1 ;

    public Long getNumber1() {
        return number1;
    }

    public void setNumber1(Long number1) {
        this.number1 = number1;
    }

    public String getValue1() {
        return value1;
    }

    public void setValue1(String value1) {
        this.value1 = value1;
    }

    public String getString1() {
        return string1;
    }

    public void setString1(String string1) {
        this.string1 = string1;
    }

    public String getText1() {
        return text1;
    }

    public void setText1(String text1) {
        this.text1 = text1;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
