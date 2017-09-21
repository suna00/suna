package net.ion.ice.core.node;

import net.ion.ice.core.event.*;
import net.ion.ice.core.event.EventListener;
import org.stagemonitor.util.StringUtils;

import java.util.*;

/**
 * Created by jaeho on 2017. 5. 15..
 */
public class NodeType {
	public static final String NODETYPE = "nodeType";
	public static final String TABLE_NAME = "tableName";
	public static final String REPOSITORY_TYPE = "repositoryType";

	public static final String NODE = "node";
	public static final String DATA = "data";

	private Node nodeTypeNode ;
	private Map<String, PropertyType> propertyTypes ;
	private Map<String, Event> events ;



	public NodeType(Node nodeTypeNode) {
		this.nodeTypeNode = nodeTypeNode ;
	}


	public void setPropertyTypes(List<Node> propertyTypeList){
		if(propertyTypes == null){
			propertyTypes = new LinkedHashMap<>() ;
		}
		for(Node _node : propertyTypeList){
			PropertyType propertyType = new PropertyType(_node);
			propertyTypes.put(propertyType.getPid(), propertyType) ;
		}
	}

	public PropertyType getPropertyType(String pid){
		return propertyTypes.get(pid) ;
	}

	public List<String> getIdablePIds() {
		List<String> ids = new ArrayList<>() ;
		for(PropertyType pt : propertyTypes.values()){
			if(pt.isIdable()){
				ids.add(pt.getPid()) ;
			}
		}
		return ids ;
	}

	public List<PropertyType> getIdablePropertyTypes() {
		List<PropertyType> idPts = new ArrayList<>() ;
		for(PropertyType pt : propertyTypes.values()){
			if(pt.isIdable()){
				idPts.add(pt) ;
			}
		}
		return idPts ;
	}

	public List<String> getLabelablePIds() {
		List<String> labels = new ArrayList<>() ;
		for(PropertyType pt : propertyTypes.values()){
			if(pt.isLabelable()){
				labels.add(pt.getPid()) ;
			}
		}
		return labels ;
	}

	public List<String> getI18nPIds () {
		List<String> i18ns = new ArrayList<>() ;
		for(PropertyType pt : propertyTypes.values()){
			if(pt.isI18n()){
				i18ns.add(pt.getPid()) ;
			}
		}
		return i18ns;
	}


	public List<PropertyType> getLabelablePropertyTypes() {
		List<PropertyType> labelPts = new ArrayList<>() ;
		for(PropertyType pt : propertyTypes.values()){
			if(pt.isLabelable()){
				labelPts.add(pt) ;
			}
		}
		return labelPts ;
	}


	public void addPropertyType(PropertyType propertyType) {
		if(propertyTypes == null){
			propertyTypes = new LinkedHashMap<>() ;
		}

		propertyTypes.put(propertyType.getPid(), propertyType) ;
	}

	public String getTypeId() {
		return nodeTypeNode.getId().toString();
	}

	public Collection<PropertyType> getPropertyTypes() {
		return propertyTypes.values();
	}

	public boolean hasReferenced() {
		for(PropertyType pt : propertyTypes.values()){
			if(pt.isReferenced()) return true ;
		}
		return false ;
	}

	public Collection<PropertyType> getPropertyTypes(PropertyType.ValueType valueType) {
		List<PropertyType> results = new ArrayList<PropertyType>() ;

		for(PropertyType pt : propertyTypes.values()){
			if(pt.getValueType() == valueType){
				results.add(pt) ;
			}
		}
		return results ;
	}
	public Object getNodeTypeNode(String nodeTypeNodeId) {
		return nodeTypeNode.get(nodeTypeNodeId);
	}
	public String getTableName() {
		return String.valueOf(nodeTypeNode.get(TABLE_NAME)).split("#")[1];
	}

	public String getDsId() {
		return String.valueOf(nodeTypeNode.get(TABLE_NAME)).split("#")[0];
	}

	public boolean isInit(){
		return this.propertyTypes != null && this.propertyTypes.size() > 0 ;
	}

	public boolean hasTableName() {
		return nodeTypeNode.get(TABLE_NAME) != null && StringUtils.isNotEmpty((String) nodeTypeNode.get(TABLE_NAME));
	}

	public boolean isDataType() {
		return nodeTypeNode.get(REPOSITORY_TYPE).equals(NodeType.DATA);
	}

	public void setEvents(List<Node> eventList){
		if(events == null){
			events = new HashMap<>() ;
		}
		for(Node _node : eventList){
			Event event = new Event(_node);
			event.setEventActions(NodeUtils.getNodeList("eventAction", "event_matching=" + event.getId()));
			event.setEventListeners(NodeUtils.getNodeList("eventListener", "event_matching=" + event.getId()));


			events.put(event.getEvent(), event) ;
		}
	}
	public Event getEvent(String event) {
		if(events == null){
			return null ;
		}
		return events.get(event) ;
	}

	public void addEvent(Event event) {
		if(events == null){
			events = new HashMap<>() ;
		}
		events.put(event.getEvent(), event) ;
	}

	public void addEventAction(EventAction eventAction) {
		if(eventAction != null && getEvent(eventAction.getEvent()) != null) {
			getEvent(eventAction.getEvent()).addEventAction(eventAction);
		}
	}

	public void addEventListener(EventListener eventListener) {
		if(eventListener != null && getEvent(eventListener.getEvent()) != null) {
			getEvent(eventListener.getEvent()).addEventListener(eventListener);
		}
	}

	public String getRepositoryType(){
		return (String) nodeTypeNode.get(REPOSITORY_TYPE);
	}


	public boolean isNode() {
		return NodeType.NODE.equals(nodeTypeNode.getStringValue(NodeType.REPOSITORY_TYPE)) || NodeType.NODE.equals(nodeTypeNode.getStoreValue(NodeType.REPOSITORY_TYPE));
	}

	public Collection<PropertyType> getReferencePropertyTypes() {
		List<PropertyType> results = new ArrayList<PropertyType>() ;

		for(PropertyType pt : propertyTypes.values()){
			if(pt.getValueType() == PropertyType.ValueType.REFERENCE || pt.getValueType() == PropertyType.ValueType.REFERENCES){
				results.add(pt) ;
			}
		}
		return results ;
	}

}
