package net.ion.ice.core.context;

import net.ion.ice.core.data.bind.NodeBindingInfo;
import net.ion.ice.core.json.JsonUtils;
import net.ion.ice.core.node.*;
import net.ion.ice.core.query.QueryResult;
import net.ion.ice.core.query.QueryTerm;
import net.ion.ice.core.query.ResultField;
import org.apache.avro.generic.GenericData;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by jaehocho on 2017. 8. 11..
 */
public class ReadContext implements Context, Serializable {
    protected NodeType nodeType;
    protected Map<String, Object> data;

    protected Integer level ;

    protected Boolean includeReferenced;
    protected List<String> includeReferencedFields ;

    protected Boolean referenceView ;
    protected Map<String, Object> referenceViewFields ;


    protected List<String> searchFields ;
    protected String searchValue ;


    protected List<ResultField> resultFields;

    protected String id ;

    protected QueryTerm.QueryTermType queryTermType ;

    protected NodeBindingInfo nodeBindingInfo ;

    protected Object result ;
    protected Node node;

    protected String dateFormat ;
    protected Map<String, Object> fileUrlFormat ;
    protected List<ResultField> commonResultFields;

    protected String ifTest ;
    protected ResultField.ResultType resultType ;

    protected Boolean remote ;

    protected Boolean cacheable ;
    protected String cacheTime ;

    protected List<String> excludePids ;
    public NodeType getNodetype() {
        return nodeType;
    }

    public boolean isIncludeReferenced() {
        return includeReferenced != null && includeReferenced;
    }

    public void setIncludeReferenced(Boolean includeReference) {
        this.includeReferenced = includeReference;
    }
    public void setIncludeReferenced(String value) {
        this.includeReferenced = Boolean.parseBoolean(value) ;
    }


    protected static void makeApiContext(Map<String, Object> config, ReadContext readContext, String key) {
        if(key.equals("response")){
            ContextUtils.makeApiResponse((Map<String, Object>) config.get(key), readContext);
        }else if(key.equals("resultType")){
            readContext.resultType = ResultField.ResultType.valueOf(config.get("resultType").toString().toUpperCase());
        }else if(key.equals("if")){
            readContext.ifTest =  ContextUtils.getValue(config.get("if"), readContext.data).toString();
        }else if(config.get(key) != null){
            ContextUtils.makeContextConfig(readContext, key, config.get(key).toString());
        }
    }


    protected static void checkCacheable(Map<String, Object> config, Map<String, Object> data, ReadContext readContext) {
        if(config != null && config.containsKey("cacheTime") && config.get("cacheTime") != null && StringUtils.isNotEmpty(config.get("cacheTime").toString())){
            if(!config.get("cacheTime").toString().equals("0")) {
                readContext.cacheTime = config.get("cacheTime").toString();
                readContext.cacheable = true;
            }

        }
        if(data.containsKey("cacheTime") && data.get("cacheTime") != null && StringUtils.isNotEmpty(data.get("cacheTime").toString())){
            if(!config.get("cacheTime").toString().equals("0")) {
                readContext.cacheTime = data.get("cacheTime").toString();
                readContext.cacheable = true;
            }
        }
    }
    protected static void checkExclude(Map<String, Object> config, Map<String, Object> data, ReadContext readContext) {
        String pids = null ;
        if(config != null && config.containsKey("excludePids") && config.get("excludePids") != null && StringUtils.isNotEmpty(config.get("excludePids").toString())){
            pids = config.get("excludePids").toString() ;
        }
        if(data.containsKey("excludePids") && data.get("excludePids") != null && StringUtils.isNotEmpty(data.get("excludePids").toString())){
            pids = data.get("excludePids").toString() ;
        }
        if(pids != null) {
            readContext.excludePids = new ArrayList<>();
            for (String pid : StringUtils.split(pids, ',')){
                readContext.excludePids.add(pid.trim()) ;
            }
        }
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
//            context.setIncludeReferenced(true);
            return ;
        }
        Map<String, Object> data = ContextUtils.makeContextData(parameterMap);
        context.data = data ;

        if(data.containsKey("fields")){
            makeResultField(context, (String) data.get("fields"));
        }else if(data.containsKey("pids")){
            makeResultField(context, (String) data.get("pids"));
        }
//        if(context.resultFields == null || context.resultFields.size() == 0 ){
//            context.setIncludeReferenced(true);
//        }
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
            this.referenceViewFields = new ConcurrentHashMap<>() ;
            for(String f : StringUtils.split(referenceView, ",")){
                if(StringUtils.isNotEmpty(f.trim())){
                    makeNestedMap(this.referenceViewFields, f.trim()) ;
                }
            }
            if(this.referenceViewFields.size() > 0){
                this.referenceView = true ;
            }else{
                this.referenceView = false ;
            }
        }
    }

    private Map<String, Object> makeNestedMap(Map<String, Object> map, String key) {
        String k = key ;
        if(key.contains(".")){
            k = StringUtils.substringBefore(key, ".") ;
            Map<String, Object> subMap = new HashMap<>() ;
            map.put(k, makeNestedMap(subMap, StringUtils.substringAfter(key, ".")));
            return map ;
        }
        map.put(k, new HashMap<>()) ;
        return map ;
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
//        if(context.includeReferenced == null) context.includeReferenced = true ;
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

    public static String getParamId(Map<String, String[]> parameterMap, String typeId){
        return getId(parameterMap, NodeUtils.getNodeType(typeId), null) ;
    }

    public QueryResult makeQueryResult() {
        return makeResult() ;
    }

    public QueryResult makeQueryResult(Object result, String fieldName, ResultField.ResultType resultType) {
        return makeResult() ;
    }


    public QueryResult makeResult() {
        QueryResult queryResult = new QueryResult() ;
        queryResult.put("result", "200") ;
        queryResult.put("resultMessage", "SUCCESS") ;
        if(result != null){
            if(result instanceof Node){
                queryResult.put("item", makeResult((Node) result));
            }else if(result instanceof Map){
                queryResult.putAll((Map<? extends String, ?>) result);
            }else{
                queryResult.put("response", result.toString()) ;
            }
        }else if(this.node != null){
            this.result = node ;
            queryResult.put("item", makeResult(node));
        }else{
            Node node = NodeUtils.getNode(nodeType, id) ;
            if(node != null) {
                queryResult.put("item", makeResult(node));
            }else{
                queryResult.put("result", "404") ;
                queryResult.put("resultMessage", "Not Found") ;
            }
        }

        return queryResult ;
    }



    protected QueryResult makeResult(Node node) {
        QueryResult itemResult = new QueryResult() ;

        if(this.resultFields == null){
            for(PropertyType pt : nodeType.getPropertyTypes()){
                itemResult.put(pt.getPid(), NodeUtils.getResultValue(this, pt, node));
            }

//            if (isIncludeReferenced()) {
//                for (PropertyType pt : nodeType.getPropertyTypes(PropertyType.ValueType.REFERENCED)) {
//                    QueryContext subQueryContext = QueryContext.makeQueryContextForReferenced(nodeType, pt, node);
//                    subQueryContext.makeQueryResult(itemResult, pt.getPid(),null);
//                }
//            }
        }else{
            this.setNodeData(node);
            makeItemQueryResult(node, itemResult, this.data, 0);
        }
        return itemResult;
    }

    protected void makeItemQueryResult(Node node, QueryResult itemResult, Map<String, Object> contextData, int i) {
        NodeType _nodeType = NodeUtils.getNodeType(node.getTypeId()) ;
        for (ResultField resultField : getResultFields()) {
            if(resultField.getFieldName().equals("_all_")){
                for(PropertyType pt : _nodeType.getPropertyTypes()){
                    itemResult.put(pt.getPid(), NodeUtils.getResultValue(this, pt, node));
                }
            }else if (resultField.getContext() != null) {
                ReadContext subQueryContext = (ReadContext) resultField.getContext();
                subQueryContext.dateFormat = this.dateFormat ;
                subQueryContext.fileUrlFormat = this.fileUrlFormat ;
                if (node != null) {
                    subQueryContext.setNodeData(node);
                }
                subQueryContext.makeQueryResult(itemResult, resultField.getFieldName(), resultField.getResultType());
            }else if(resultField.getExecuteType() != null){
                Map<String, Object> _data = new HashMap<>();
                _data.putAll(contextData);
                _data.putAll(node);
                switch (resultField.getExecuteType()) {
                    case QUERY: {
                        ApiQueryContext apiQueryContext = ApiQueryContext.makeContextFromConfig(resultField.getFieldOption(), _data);
                        apiQueryContext.dateFormat = this.dateFormat ;
                        apiQueryContext.fileUrlFormat = this.fileUrlFormat ;
                        apiQueryContext.makeQueryResult(itemResult, resultField.getFieldName(), resultField.getResultType());
                        break ;
                    }
                    case SELECT: {
                        ApiSelectContext apiQueryContext = ApiSelectContext.makeContextFromConfig(resultField.getFieldOption(), _data);
                        apiQueryContext.dateFormat = this.dateFormat ;
                        apiQueryContext.fileUrlFormat = this.fileUrlFormat ;
                        apiQueryContext.makeQueryResult(itemResult, resultField.getFieldName());
                        break ;
                    }
                    case VALUE: {
                        itemResult.put(resultField.getFieldName(), ContextUtils.getValue(resultField.getStaticValue(), _data, this, _nodeType, node));
                        break ;
                    }
                    case OPTION: {
                        String fieldValue = resultField.getFieldValue();
                        fieldValue = fieldValue == null || StringUtils.isEmpty(fieldValue) ? resultField.getFieldName() : fieldValue;

                        PropertyType pt = _nodeType.getPropertyType(fieldValue) ;
                        if(pt == null) continue;

                        FieldContext fieldContext = FieldContext.makeContextFromConfig(resultField.getFieldOption(), _data);
                        fieldContext.dateFormat = this.dateFormat ;
                        fieldContext.fileUrlFormat = this.fileUrlFormat ;
                        if(StringUtils.isNotEmpty(pt.getReferenceType()) && NodeUtils.getNodeType(pt.getReferenceType()) != null ){
                            fieldContext.nodeType = NodeUtils.getNodeType(pt.getReferenceType()) ;
                        }
                        if(resultField.getResultType() == ResultField.ResultType.SIZE){
                            fieldContext.includeReferenced = true ;
                            List list = (List) NodeUtils.getResultValue(fieldContext, pt, node);
                            itemResult.put(resultField.getFieldName(), list == null ? 0 : list.size());
                        }else {
                            if(fieldContext.referenceView != null && fieldContext.referenceView == true && fieldContext.getResultFields() != null ){
                                fieldContext.referenceView = false ;
                                if(pt.getValueType() == PropertyType.ValueType.REFERENCES){
                                    String values = (String) node.get(pt.getPid());
                                    if (values != null && StringUtils.isNotEmpty(values)) {
                                        List<QueryResult> refsResults = new ArrayList<>() ;
                                        for (String refVal : StringUtils.split(values, ",")) {
                                            Node refNode = NodeUtils.getReferenceNode(refVal, pt);
                                            if(refNode != null) {
                                                refsResults.add(fieldContext.makeQueryResult(refNode)) ;
                                            }
                                        }
                                        itemResult.put(resultField.getFieldName(), refsResults);
                                    }
                                }else {
                                    Node refNode = NodeUtils.getReferenceNode(node.get(pt.getPid()), pt);
                                    if (refNode != null) {
                                        itemResult.put(resultField.getFieldName(), fieldContext.makeQueryResult(refNode));
                                    }
                                }
                            }else {
                                itemResult.put(resultField.getFieldName(), NodeUtils.getResultValue(fieldContext, pt, node));
                            }
                        }
                        break ;
                    }
                }
            } else {
                String fieldValue = resultField.getFieldValue();
                fieldValue = fieldValue == null || StringUtils.isEmpty(fieldValue) ? resultField.getFieldName() : fieldValue;

                if (resultField.getFieldValue().equals("_position_")) {
                    itemResult.put(resultField.getFieldName(), ((QueryContext) this).getStart() + i + 1);
                }else {
                    PropertyType pt = _nodeType.getPropertyType(fieldValue);
                    if (pt == null) continue;
                    itemResult.put(resultField.getFieldName(), NodeUtils.getResultValue(this, pt, node));
                }
            }
        }
    }



    public boolean isReferenceView(PropertyType pt) {
        if (referenceView == null) {
            return false;
        }
        if (referenceView && referenceViewFields == null) {
            return true;
        } else if (referenceView && referenceViewFields.containsKey(pt.getPid())) {
            return true;
        }

        return false;
    }

    public boolean isIncludeReferenced(PropertyType pt) {
        if (includeReferenced == null) {
            return false;
        }
        if (includeReferenced && includeReferencedFields == null) {
            if(this.excludeReferenceList != null && excludeReferenceList.contains(pt.getReferenceType())){
                return false ;
            }
            return true;
        } else if (includeReferenced && includeReferencedFields.contains(pt.getPid())) {
            if(this.excludeReferenceList != null && excludeReferenceList.contains(pt.getReferenceType())){
                return false ;
            }

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

    public void setReferenceViewFields(Map<String, Object> referenceViewFields) {
        this.referenceViewFields = referenceViewFields;
    }

    public void setIncludeReferencedFields(List<String> includeReferencedFields) {
        this.includeReferencedFields = includeReferencedFields;
    }

    public void setNodeData(Map<String, Object> nodeData) {
        Map<String, Object> _data = new HashMap<>() ;
        _data.putAll(data);
        _data.putAll(nodeData);

        this.data = _data ;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    public String getDateFormat() {
        return dateFormat;
    }

    public Map<String, Object> getFileUrlFormat() {
        return fileUrlFormat;
    }

    public boolean hasLocale() {
//        return this.data != null && ((this.data.containsKey("locale") && StringUtils.isNotEmpty((String) data.get("locale"))) || (this.data.containsKey("langCd") && StringUtils.isNotEmpty((String) data.get("langCd")))) ;
        return this.data != null && ((this.data.containsKey("locale")) || (this.data.containsKey("langCd"))) ;

    }

    public String getLocale() {
        String locale = (String) data.get("locale");
        if(StringUtils.isEmpty(locale) && this.data.containsKey("langCd")){
            return (String) data.get("langCd");
        }
        return locale ;
    }

    public void setDateFormat(String dateFormat) {
        this.dateFormat = dateFormat;
    }

    public void setFileUrlFormat(Map<String,Object> fileUrlFormat) {
        this.fileUrlFormat = fileUrlFormat;
    }

    public NodeType getNodeType() {
        return nodeType;
    }

    public String getDataStringValue(String key) {
        Object val = JsonUtils.getValue(data, key) ;
        if(val == null) return "" ;
        if(val instanceof String){
            return (String) val;
        }
        return val.toString() ;
    }

    public Object getDataValue(String key) {
        return JsonUtils.getValue(data, key) ;
    }

    public Boolean getReferenceView() {
        return referenceView;
    }


    protected String excludeReferenceType ;
    protected List<String> excludeReferenceList ;

    public void setExcludeReferenceType(List<String> excludeReferenceList, String excludeReferenceType) {
        this.excludeReferenceType = excludeReferenceType;

        if(this.excludeReferenceList == null){
            this.excludeReferenceList = new ArrayList<>() ;
        }

        this.excludeReferenceList.add(excludeReferenceType) ;
    }

    public List<String> getIncludeReferencedFields() {
        return includeReferencedFields;
    }

    public Boolean isIncludeReferencedValue() {
        return this.includeReferenced;
    }

    public List<String> getExcludeReferenceList() {
        return excludeReferenceList;
    }

    protected List<String> referencePath ;
    public void addReferencepath(List<String> referencePath, String pid) {
        this.referencePath = referencePath ;
        if(this.referencePath == null){
            this.referencePath = new ArrayList<>() ;
        }
        this.referencePath.add(pid) ;
    }


    public List<String> getReferencePath() {
        return referencePath;
    }

    public Map<String, Object> getSubReferenceViewFields(String pid) {
        if(this.referenceViewFields == null || !this.referenceViewFields.containsKey(pid)){
            return new HashMap<>() ;
        }
        return (Map<String, Object>) this.referenceViewFields.get(pid);
    }

    public static ReadContext makeQueryContextForReference(PropertyType pt, Node refNode) {
        ReadContext readContext = new ReadContext() ;
        return readContext ;
    }

    public void setData(Map<String,Object> data) {
        this.data = data;
    }

    public List<String> getExcludePids() {
        return excludePids;
    }

    public void setExcludePids(List<String> excludePids) {
        this.excludePids = excludePids;
    }
}
