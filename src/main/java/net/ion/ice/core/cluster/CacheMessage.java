package net.ion.ice.core.cluster;

import com.hazelcast.core.Member;
import net.ion.ice.core.node.Node;

import java.io.Serializable;

public class CacheMessage implements Serializable{
    private String event;
    private String typeId ;
    private String id ;

    private String server;

    private Integer retry;

//    private Node node ;

    public CacheMessage(String event, Node node){
        this.event = event ;
//        this.node = node ;
        this.typeId = node.getTypeId() ;
        this.id = node.getId() ;
    }

    public CacheMessage(Member member, String typeId, String id) {
        this.server = member.getAddress().getHost() + ":" + member.getStringAttribute("port") ;
        this.typeId = typeId;
        this.id=id;
        this.retry = 0;
    }

    public String getEvent(){
        return this.event ;
    }

    public String getTypeId() {
        return typeId;
    }

    public String getId() {
        return id;
    }

    public String toString(){
        return this.event + Node.TYPE_SEPERATOR + this.typeId + Node.TYPE_SEPERATOR + this.id ;
    }

    public String getServer() {
        return server;
    }

    public Integer getRetry() {
        return retry;
    }

    public void incrementRetry() {
        this.retry = this.retry +  1;
    }

//    public Node getNode(){
//        return this.node ;
//    }
}
