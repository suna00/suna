package net.ion.ice.core.context;

import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeType;
import net.ion.ice.core.node.NodeUtils;
import net.ion.ice.core.node.PropertyType;
import net.ion.ice.core.query.QueryResult;
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
    protected Boolean includeReference;
    protected Boolean referenceView ;
    protected List<String> referenceViewFields ;


    protected List<ResultField> resultFields;

    protected String id ;

    public NodeType getNodetype() {
        return nodeType;
    }

    public boolean isIncludeReferenced() {
        return includeReference != null && includeReference;
    }

    public void setIncludeReference(boolean includeReference) {
        this.includeReference = includeReference;
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


    public static ReadContext makeContextFormParameter(Map<String, String[]> parameterMap, NodeType nodeType, String id) {
        ReadContext ctx = new ReadContext();
        ctx.nodeType = nodeType ;

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

        ctx.id = id ;

        if(parameterMap.containsKey("fields")){
            makeResultField(ctx, parameterMap.get("fields")[0]);
        }else if(parameterMap.containsKey("pids")){
            makeResultField(ctx, parameterMap.get("pids")[0]);
        }

        if(ctx.resultFields == null || ctx.resultFields.size() == 0 ){
            ctx.setIncludeReference(true);
        }

        String referenceView = ContextUtils.getParameterValue("referenceView", parameterMap) ;

        if(StringUtils.isEmpty(referenceView)){
            ctx.referenceView = null ;
        }else if ("true".equals(referenceView)) {
            ctx.referenceView = true ;
        }else if ("false".equals(referenceView)) {
            ctx.referenceView = false;
        }else{
            ctx.referenceViewFields = new ArrayList<>() ;
            for(String f : StringUtils.split(referenceView, ",")){
                if(StringUtils.isNotEmpty(f.trim())){
                    ctx.referenceViewFields.add(f.trim()) ;
                }
            }
            if(ctx.referenceViewFields.size() > 0){
                ctx.referenceView = true ;
            }else{
                ctx.referenceView = false ;
            }
        }

        return ctx ;
    }

    public Node read() {

        return null ;
    }

    public QueryResult makeResult() {
        Node node = NodeUtils.getNode(nodeType.getTypeId(), id) ;
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
        QueryResult queryResult = new QueryResult() ;
        queryResult.put("result", "200") ;
        queryResult.put("resultMessage", "SUCCESS") ;
        queryResult.put("item", itemResult) ;

        return queryResult ;
    }

    public boolean isReferenceView(String pid) {
        if(referenceView == null){
            return false ;
        }
        if(referenceView && referenceViewFields == null){
            return true ;
        }else if(referenceView && referenceViewFields.contains(pid)){
            return true ;
        }

        return false ;
    }
}
