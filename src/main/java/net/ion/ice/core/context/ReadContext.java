package net.ion.ice.core.context;

import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeType;
import net.ion.ice.core.node.NodeUtils;
import net.ion.ice.core.node.PropertyType;
import net.ion.ice.core.query.QueryResult;
import net.ion.ice.core.query.QueryTerm;
import net.ion.ice.core.query.QueryUtils;
import net.ion.ice.core.query.ResultField;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
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


    protected List<ResultField> resultFields;

    protected String id ;

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

        makeReferenceView(context, data);
    }

    protected static void makeReferenceView(ReadContext context, Map<String, Object> data) {
        String referenceView = (String) data.get("referenceView");
        if(StringUtils.isEmpty(referenceView)){
            context.referenceView = null ;
        }else if ("true".equals(referenceView)) {
            context.referenceView = true ;
        }else if ("false".equals(referenceView)) {
            context.referenceView = false;
        }else{
            context.referenceViewFields = new ArrayList<>() ;
            for(String f : StringUtils.split(referenceView, ",")){
                if(StringUtils.isNotEmpty(f.trim())){
                    context.referenceViewFields.add(f.trim()) ;
                }
            }
            if(context.referenceViewFields.size() > 0){
                context.referenceView = true ;
            }else{
                context.referenceView = false ;
            }
        }
    }

    public static ReadContext createContextFromParameter(Map<String, String[]> parameterMap, NodeType nodeType, String id) {
        ReadContext context = new ReadContext();
        context.nodeType = nodeType ;

        context.id = getId(parameterMap, nodeType, id);

        makeContextFromParameter(parameterMap, nodeType, context) ;

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


    public QueryResult makeResult() {
        Node node = NodeUtils.getNode(nodeType.getTypeId(), id) ;
        QueryResult itemResult = makeResult(node);

        QueryResult queryResult = new QueryResult() ;
        queryResult.put("result", "200") ;
        queryResult.put("resultMessage", "SUCCESS") ;
        queryResult.put("item", itemResult) ;

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
        }
        return itemResult;
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
}
