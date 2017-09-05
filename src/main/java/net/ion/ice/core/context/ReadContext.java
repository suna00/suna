package net.ion.ice.core.context;

import net.ion.ice.core.data.bind.NodeBindingInfo;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeType;
import net.ion.ice.core.node.NodeUtils;
import net.ion.ice.core.node.PropertyType;
import net.ion.ice.core.query.QueryResult;
import net.ion.ice.core.query.QueryTerm;
import net.ion.ice.core.query.ResultField;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by jaehocho on 2017. 8. 11..
 */
public class ReadContext implements Context {
    protected NodeType nodeType;
    protected Map<String, Object> data;

    protected Integer level ;

    protected Boolean includeReferenced;
    protected List<String> includeReferencedFields ;

    protected Boolean referenceView ;
    protected List<String> referenceViewFields ;


    protected List<String> searchFields ;
    protected String searchValue ;


    protected List<ResultField> resultFields;

    protected String id ;

    protected QueryTerm.QueryTermType queryTermType ;

    protected NodeBindingInfo nodeBindingInfo ;

    protected Object result ;

    protected String dateFormat ;
    protected Map<String, Object> fileUrlFormat ;
    protected List<ResultField> commonResultFields;


    public NodeType getNodetype() {
        return nodeType;
    }

    public boolean isIncludeReferenced() {
        return includeReferenced != null && includeReferenced;
    }

    public void setIncludeReferenced(boolean includeReference) {
        this.includeReferenced = includeReference;
    }
    public void setIncludeReferenced(String value) {
        this.includeReferenced = Boolean.parseBoolean(value) ;
    }

    protected static void makeResultField(ReadContext context, String fields) {
        if(StringUtils.contains(fields,",")) {
            addResultFields(context, fields, ",");
        }else if(StringUtils.contains(fields," ")){
            addResultFields(context, fields, " ");
        }else{
            if(StringUtils.isNotEmpty(fields)) {
                context.addResultField(new ResultField(fields, fields));
            }
        }
    }

    protected static void addResultFields(ReadContext context, String fields, String s) {
        for (String field : StringUtils.split(fields, s)) {
            field = StringUtils.trim(field) ;
            if(StringUtils.isNotEmpty(field)) {
                context.addResultField(new ResultField(field, field));
            }
        }
    }
    protected void addResultField(ResultField resultField) {
        if (this.resultFields == null) {
            this.resultFields = new ArrayList<>();
        }
        this.resultFields.add(resultField);
    }

    public List<ResultField> getResultFields() {
        return resultFields;
    }

    protected static void makeContextFromParameter(Map<String, String[]> parameterMap, NodeType nodeType, ReadContext context) {
        if (parameterMap == null || parameterMap.size() == 0) {
            context.setIncludeReferenced(true);
            return ;
        }
        Map<String, Object> data = ContextUtils.makeContextData(parameterMap);
        context.data = data ;

        if(data.containsKey("fields")){
            makeResultField(context, (String) data.get("fields"));
        }else if(data.containsKey("pids")){
            makeResultField(context, (String) data.get("pids"));
        }
        if(context.resultFields == null || context.resultFields.size() == 0 ){
            context.setIncludeReferenced(true);
        }
//        makeSearchFields(context, data) ;
    }



    public void makeReferenceView() {
        if(data == null) return ;
        String referenceView = (String) data.get("referenceView");
        makeReferenceView(referenceView);
    }

    public void makeReferenceView(String referenceView) {
        if(StringUtils.isEmpty(referenceView)){
            this.referenceView = null ;
        }else if ("true".equals(referenceView)) {
            this.referenceView = true ;
        }else if ("false".equals(referenceView)) {
            this.referenceView = false;
        }else{
            this.referenceViewFields = new ArrayList<>() ;
            for(String f : StringUtils.split(referenceView, ",")){
                if(StringUtils.isNotEmpty(f.trim())){
                    this.referenceViewFields.add(f.trim()) ;
                }
            }
            if(this.referenceViewFields.size() > 0){
                this.referenceView = true ;
            }else{
                this.referenceView = false ;
            }
        }
    }

    public void makeIncludeReferenced() {
        if(data == null) return ;

        String includeReferenced = (String) data.get("includeReferenced");
        makeIncludeReferenced(includeReferenced);
    }


    public void makeIncludeReferenced(String includeReferenced) {
        if(StringUtils.isEmpty(includeReferenced)){
            this.includeReferenced = null ;
        }else if ("true".equals(includeReferenced)) {
            this.includeReferenced = true ;
        }else if ("false".equals(includeReferenced)) {
            this.includeReferenced = false;
        }else{
            this.includeReferencedFields = new ArrayList<>() ;
            for(String f : StringUtils.split(includeReferenced, ",")){
                if(StringUtils.isNotEmpty(f.trim())){
                    this.includeReferencedFields.add(f.trim()) ;
                }
            }
            if(this.includeReferencedFields.size() > 0){
                this.includeReferenced = true ;
            }else{
                this.includeReferenced = false ;
            }
        }
    }


    public static ReadContext createContextFromParameter(Map<String, String[]> parameterMap, NodeType nodeType, String id) {
        ReadContext context = new ReadContext();
        context.nodeType = nodeType ;

        context.id = getId(parameterMap, nodeType, id);

        makeContextFromParameter(parameterMap, nodeType, context) ;

        context.makeIncludeReferenced();
        context.makeReferenceView();

        return context ;
    }

    protected static String getId(Map<String, String[]> parameterMap, NodeType nodeType, String id) {
        if(StringUtils.isEmpty(id)){
            if(parameterMap.containsKey("id")){
                id = parameterMap.get("id")[0] ;
            }else{
                List<String> idablePids = nodeType.getIdablePIds() ;
                id = "" ;
                for(int i = 0 ; i < idablePids.size(); i++){
                    id = id + parameterMap.get(idablePids.get(i))[0] + (i < (idablePids.size() - 1) ? Node.ID_SEPERATOR : "") ;
                }
            }
        }
        return id;
    }

    public QueryResult makeQueryResult(Object result, String fieldName) {
        return makeResult() ;
    }

    public QueryResult makeResult() {
        Node node = NodeUtils.getNode(nodeType.getTypeId(), id) ;
        if(node == null){
            this.result = data ;
        }else{
            this.result = node ;
        }

        QueryResult queryResult = new QueryResult() ;
        queryResult.put("result", "200") ;
        queryResult.put("resultMessage", "SUCCESS") ;
        if(node != null) {
            queryResult.put("item", makeResult(node));
        }

        return queryResult ;
    }

    protected QueryResult makeResult(Node node) {
        QueryResult itemResult = new QueryResult() ;

        if(this.resultFields == null){
            for(PropertyType pt : nodeType.getPropertyTypes()){
                itemResult.put(pt.getPid(), NodeUtils.getResultValue(this, pt, node));
            }

            if (isIncludeReferenced()) {
                for (PropertyType pt : nodeType.getPropertyTypes(PropertyType.ValueType.REFERENCED)) {
                    QueryContext subQueryContext = QueryContext.makeQueryContextForReferenced(nodeType, pt, node);
                    subQueryContext.makeQueryResult(itemResult, pt.getPid());
                }
            }
        }else{
            this.setNodeData(node);
            makeItemQueryResult(node, itemResult, this.data);
        }
        return itemResult;
    }

    protected void makeItemQueryResult(Node node, QueryResult itemResult, Map<String, Object> contextData) {

        for (ResultField resultField : getResultFields()) {
            if (resultField.getContext() != null) {
                ReadContext subQueryContext = (ReadContext) resultField.getContext();
                if (node != null) {
                    subQueryContext.setNodeData(node);
                }
                subQueryContext.makeQueryResult(itemResult, resultField.getFieldName());
            } else if(resultField.isStaticValue()){
                itemResult.put(resultField.getFieldName(), ContextUtils.getValue(resultField.getStaticValue(), contextData));
            }else if(resultField.getResultType() != null){
                Map<String, Object> _data = new HashMap<>();
                _data.putAll(contextData);
                _data.putAll(node);
                switch (resultField.getResultType()) {
                    case QUERY: {
                        ApiQueryContext apiQueryContext = ApiQueryContext.makeContextFromConfig(resultField.getFieldOption(), _data);
                        apiQueryContext.makeQueryResult(itemResult, resultField.getFieldName());
                    }
                    case SELECT: {
                        ApiSelectContext apiQueryContext = ApiSelectContext.makeContextFromConfig(resultField.getFieldOption(), _data);
                        apiQueryContext.makeQueryResult(itemResult, resultField.getFieldName());
                    }
                    case VALUE: {
                        itemResult.put(resultField.getFieldName(), ContextUtils.getValue(resultField.getStaticValue(), _data));
                    }
                    case OPTION: {
                        String fieldValue = resultField.getFieldValue();
                        fieldValue = fieldValue == null || StringUtils.isEmpty(fieldValue) ? resultField.getFieldName() : fieldValue;
                        itemResult.put(resultField.getFieldName(), NodeUtils.getResultValue(resultField.getFieldContext(), nodeType.getPropertyType(fieldValue), node));
                    }
                }
            } else {
                String fieldValue = resultField.getFieldValue();
                fieldValue = fieldValue == null || StringUtils.isEmpty(fieldValue) ? resultField.getFieldName() : fieldValue;
                itemResult.put(resultField.getFieldName(), NodeUtils.getResultValue(resultField.getFieldContext() != null ? resultField.getFieldContext() : this, nodeType.getPropertyType(fieldValue), node));
            }
        }
    }

    public boolean isReferenceView(String pid) {
        if (referenceView == null) {
            return false;
        }
        if (referenceView && referenceViewFields == null) {
            return true;
        } else if (referenceView && referenceViewFields.contains(pid)) {
            return true;
        }

        return false;
    }

    public boolean isIncludeReferenced(String pid) {
        if (includeReferenced == null) {
            return true;
        }
        if (includeReferenced && includeReferencedFields == null) {
            return true;
        } else if (includeReferenced && includeReferencedFields.contains(pid)) {
            return true;
        }

        return false;
    }

    public Integer getLevel() {
        if(level  == null){
            return 0 ;
        }
        return level;
    }

    public void setLevel(Integer level){
        this.level = level ;
    }

    public Map<String,Object> getData() {
        return data;
    }


    public void setReferenceView(Boolean referenceView) {
        this.referenceView = referenceView;
    }

    public void setReferenceViewFields(List<String> referenceViewFields) {
        this.referenceViewFields = referenceViewFields;
    }

    public void setIncludeReferencedFields(List<String> includeReferencedFields) {
        this.includeReferencedFields = includeReferencedFields;
    }

    public void setNodeData(Node nodeData) {
        Map<String, Object> _data = new HashMap<>() ;
        _data.putAll(data);
        _data.putAll(nodeData);

        this.data = _data ;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Node result) {
        this.result = result;
    }

    public String getDateFormat() {
        return dateFormat;
    }

    public Map<String, Object> getFileUrlFormat() {
        return fileUrlFormat;
    }

    public boolean hasLocale() {
        return this.data != null && this.data.containsKey("locale") && StringUtils.isNotEmpty((String) data.get("locale")) ;
    }

    public String getLocale() {
        return (String) data.get("locale");
    }
}
