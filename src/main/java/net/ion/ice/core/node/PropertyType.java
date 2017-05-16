package net.ion.ice.core.node;

/**
 * Created by jaeho on 2017. 5. 15..
 */
public class PropertyType extends Node {


    public boolean indexing() {
        return true ;
    }

    public String getAnalyzer() {
        return (String) get("analyzer");
    }
}
