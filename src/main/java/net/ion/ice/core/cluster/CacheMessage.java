package net.ion.ice.core.cluster;

import net.ion.ice.core.node.Node;

import java.io.Serializable;

public class CacheMessage implements Serializable{
    private String event;
    private String typeId ;
    private String id ;


//    private Node node ;

    public CacheMessage(String event, Node node){
        this.event = event ;
//        this.node = node ;
        this.typeId = node.getTypeId() ;
        this.id = node.getId() ;
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
//    public Node getNode(){
//        return this.node ;
//    }
}
