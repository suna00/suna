package net.ion.ice.core.node;

import net.ion.ice.core.infinispan.lucene.CodeAnalyzer;
import org.apache.lucene.analysis.cjk.CJKAnalyzer;
import org.hibernate.search.annotations.*;

import javax.persistence.Id;
import java.io.Serializable;
import java.util.Date;

/**
 * Created by jaehocho on 2017. 5. 17..
 */

@ProvidedId
@Indexed
public class NodeValue implements Serializable{
    @Id
    @DocumentId
    private Object id ;

    @Field
    @Analyzer(impl = CodeAnalyzer.class)
    private String tid ;


    @Field
    @Analyzer(impl = CodeAnalyzer.class)
    private String owner ;


    @Field
    @Analyzer(impl = CodeAnalyzer.class)
    private String modifier ;


    @Field(analyze = Analyze.NO)
    @DateBridge(resolution = Resolution.SECOND)
    private Date created ;

    @Field(analyze = Analyze.NO)
    @DateBridge(resolution = Resolution.SECOND)
    private Date changed ;

    @Field(analyze = Analyze.NO)
    @Analyzer(impl = CodeAnalyzer.class)
    private String status ;

    @Field
    @Analyzer(impl = CJKAnalyzer.class)
    private String content ;

    public NodeValue(Object id, String tid, String userId) {
        this.id = id ;
        this.tid = tid ;
        this.owner = userId ;
        this.modifier = userId ;
        this.created = new Date() ;
        this.changed = this.created ;
        this.status = "created" ;
    }

    public String getTid() {
        return tid;
    }

    public void setTid(String tid) {
        this.tid = tid;
    }

    public void setId(Object id) {
        this.id = id;
    }
}
