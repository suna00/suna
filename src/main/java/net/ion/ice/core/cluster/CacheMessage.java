package net.ion.ice.core.cluster;

import net.ion.ice.core.node.Node;

import java.io.Serializable;

public class CacheMessage implements Serializable{
    private String event ;
    private Node node ;

    public CacheMessage(String event, Node node){
        this.event = event ;
        this.node = node ;
    }

    public String getEvent(){
        return this.event ;
    }

    public Node getNode(){
        return this.node ;
    }
}
