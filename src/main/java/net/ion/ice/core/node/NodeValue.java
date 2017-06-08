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
public class NodeValue implements Serializable, Cloneable{
    @Id
    @DocumentId
    private Object id ;

    @Field
    @Analyzer(impl = CodeAnalyzer.class)
    private String typeId;


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

    public NodeValue(Object id, String typeId, String userId) {
        this(id, typeId, userId, userId, new Date(), new Date(), "created") ;
    }

    public NodeValue(Object id, String typeId, String owner, String modifier, Date created, Date changed, String status) {
        this.id = id ;
        this.typeId = typeId ;
        this.owner = owner ;
        this.modifier = modifier ;
        this.created = created ;
        this.changed = changed ;
        this.status = status ;

    }


    public String getTypeId() {
        return typeId;
    }

    public void setTypeId(String typeId) {
        this.typeId = typeId;
    }

    public void setId(Object id) {
        this.id = id;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Date getChanged() {
        return changed;
    }

    public NodeValue clone(){
        return new NodeValue(id, typeId, owner, modifier, created, changed, status) ;
    }

    public void setChanged(Date changed) {
        this.changed = changed;
    }

    public void setModifier(String modifier) {
        this.modifier = modifier;
    }
}
