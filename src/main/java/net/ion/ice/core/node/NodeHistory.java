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
    @Field(store = Store.NO)
    @Analyzer(impl = CodeAnalyzer.class)
    private String historyId ;


    @Field(store = Store.NO)
    @Analyzer(impl = CodeAnalyzer.class)
    private String id;

    @Field(store = Store.NO)
    @Analyzer(impl = CodeAnalyzer.class)
    private String event;

    @Field(analyze = Analyze.NO, store = Store.NO)
    private Integer version ;

    @Field(store = Store.NO)
    @Analyzer(impl = CodeAnalyzer.class)
    private String modifier ;

    @Field(analyze = Analyze.NO, store = Store.NO)
    @DateBridge(resolution = Resolution.MILLISECOND)
    private Date changed ;


    private Properties<String, Object> properties ;

    public NodeHistory(Node node, String event, List<String> changePids){
        this.id = node.getId() ;
        this.modifier = node.getModifier();
        this.changed = node.getChanged();
        this.event = event ;
        if(changePids != null && changePids.size() > 0){
            this.properties = new Properties<>();
            for(String pid : changePids){
                this.properties.put(pid, node.get(pid)) ;
            }
        }
    }

}
