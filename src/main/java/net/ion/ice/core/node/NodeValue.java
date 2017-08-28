package net.ion.ice.core.node;

import net.ion.ice.core.infinispan.lucene.CodeAnalyzer;
import org.apache.lucene.analysis.cjk.CJKAnalyzer;
import org.hibernate.search.annotations.*;

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

    public static final String NODEVALUE_SEPERATOR = "::";

    public static List<String> NODE_VALUE_KEYS = Arrays.asList(new String[] {"id", "typeId", "owner", "modifier", "created", "changed", "status"}) ;

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

    public String getOwner() {
        return owner;
    }

    public String getModifier() {
        return modifier;
    }

    public Date getCreated() {
        return created;
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

    public static boolean containsKey(String pid) {
        return NODE_VALUE_KEYS.contains(pid) ;
    }

    public Object getValue(String pid){
        switch (pid){
            case "id":
                return id ;
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

    public Object putValue(String key, Object value) {
        switch (key){
            case "id":
                this.id = value.toString();
                return id ;
            case "typeId":
                setTypeId(value.toString());
                return getTypeId() ;
            case "owner" :
                if (value instanceof Code) {
                    owner = ((Code) value).getValue().toString();
                } else {
                    owner = (String) value;
                }
                return owner ;
            case "modifier" :
                if (value instanceof Code) {
                    modifier = ((Code) value).getValue().toString();
                } else {
                    modifier = (String) value;
                }
                return modifier ;
            case "created":
                created = NodeUtils.getDateValue(value) ;
                return created ;
            case "changed":
                changed = NodeUtils.getDateValue(value) ;
                return changed ;
            case "status" :
                status = (String) value;
                return status ;
            default:
                return null ;
        }
    }
}
