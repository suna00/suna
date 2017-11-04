package net.ion.ice.core.node;

import net.ion.ice.core.infinispan.lucene.CodeAnalyzer;
import org.hibernate.search.annotations.*;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Created by jaeho on 2017. 6. 7..
 */
@Indexed
public class NodeHistory implements Serializable{


    @DocumentId
    @Field(store = Store.NO, analyze = Analyze.NO)
    private String historyId ;


    @Field(store = Store.NO, analyze = Analyze.NO)
    private String id;

    @Field(store = Store.NO, analyze = Analyze.NO)
    private String event;

    @Field(store = Store.NO, analyze = Analyze.NO)
    @SortableField
    private Integer version ;

    @Field(store = Store.NO)
    @Analyzer(impl = CodeAnalyzer.class)
    private String modifier ;

    @Field(analyze = Analyze.NO, store = Store.NO)
    @DateBridge(resolution = Resolution.MILLISECOND)
    private Date changed ;


    private Properties<String, Object> properties ;

    public NodeHistory(Node node, Integer version, String event, List<String> changePids){
        this.id = node.getId() ;
        this.version = version ;
        this.modifier = node.getModifier();
        this.changed = node.getChanged();
        this.event = event ;
        if(changePids != null && changePids.size() > 0){
            this.properties = new Properties<>();
            for(String pid : changePids){
                this.properties.put(pid, node.get(pid)) ;
            }
        }
        this.historyId = this.id + "_" + this.version ;
    }
    public void increment() {
        this.version = this.version + 1 ;
        this.historyId = this.id + "_" + this.version ;
    }

    public String getHistoryId() {
        return historyId;
    }

    public String getId() {
        return id;
    }

    public Integer getVersion() {
        return version;
    }


    public String getEvent() {
        return event;
    }

    public String getModifier() {
        return modifier;
    }

    public String getChanged() {
        return NodeUtils.getDateStringValue(changed, null);
    }

    public Properties<String, Object> getProperties(){
        return this.properties ;
    }
}
