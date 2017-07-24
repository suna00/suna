package net.ion.ice.core.node;

import net.ion.ice.core.infinispan.lucene.CodeAnalyzer;
import org.apache.lucene.analysis.cjk.CJKAnalyzer;
import org.hibernate.search.annotations.*;

import javax.persistence.Id;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Created by jaehocho on 2017. 5. 17..
 */

@ProvidedId
@Indexed
public class NodeValue implements Serializable, Cloneable{
    public static List<String> NODE_VALUE_KEYS = Arrays.asList(new String[] {"typeId", "owner", "modifier", "created", "changed", "status"}) ;

    @Id
    @DocumentId
    @Field
    @Analyzer(impl = CodeAnalyzer.class)
    private String id ;

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

    public NodeValue(String id, String typeId, String userId, Date changed) {
        this(id, typeId, userId, userId, changed, changed, "created") ;
    }

    public NodeValue(String id, String typeId, String owner, String modifier, Date created, Date changed, String status) {
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

    public void setId(String id) {
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

    public boolean containsKey(String pid) {
        return NODE_VALUE_KEYS.contains(pid) ;
    }

    public Object getValue(String pid){
        switch (pid){
            case "typeId":
                return getTypeId() ;
            case "owner" :
                return owner ;
            case "modifier" :
                return modifier ;
            case "created":
                return created ;
            case "changed":
                return changed ;
            case "status" :
                return status ;
            default:
                return null ;
        }

    }
}
