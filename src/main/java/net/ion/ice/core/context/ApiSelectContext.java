package net.ion.ice.core.context;

import net.ion.ice.ApplicationContextManager;
import net.ion.ice.core.cluster.ClusterUtils;
import net.ion.ice.core.data.DBService;
import net.ion.ice.core.node.NodeType;
import net.ion.ice.core.node.NodeUtils;
import net.ion.ice.core.node.PropertyType;
import net.ion.ice.core.query.QueryResult;
import net.ion.ice.core.query.ResultField;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.context.request.NativeWebRequest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

/**
 * Created by jaehocho on 2017. 8. 24..
 */
public class ApiSelectContext extends ReadContext implements CacheableContext{
    protected Map<String, Object> config  ;
    protected HttpServletRequest httpRequest ;
    protected HttpServletResponse httpResponse ;
    protected String ds ;
    protected String sql ;

    protected JdbcTemplate jdbcTemplate ;
    protected Template sqlTemplate  ;


    public static ApiSelectContext makeContextFromConfig(Map<String, Object> config, Map<String, Object> data) {
        ApiSelectContext selectContext = new ApiSelectContext();
        selectContext.config = config ;
        selectContext.data = data ;

        checkCacheable(config, data, selectContext) ;

        for(String key : config.keySet()) {
            if(key.equals("select")) continue ;
            makeApiContext(config, selectContext, key);
        }

        Map<String, Object> select = (Map<String, Object>) config.get("select");
        selectContext.ds = (String) select.get("ds");
        selectContext.sql = (String) select.get("sql");

        if (select.containsKey("resultType")) {
            selectContext.resultType = ResultField.ResultType.valueOf(select.get("resultType").toString().toUpperCase());
        }
        DBService dbService = ApplicationContextManager.getBean(DBService.class) ;
        selectContext.jdbcTemplate = dbService.getJdbcTemplate(selectContext.ds) ;
        selectContext.sqlTemplate = new Template(selectContext.sql) ;
        selectContext.sqlTemplate.parsing();


        return selectContext;
    }


    public static ApiSelectContext makeContextFromConfig(Map<String, Object> config, Map<String, Object> data, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        ApiSelectContext selectContext = makeContextFromConfig(config, data) ;
        selectContext.httpRequest = httpRequest ;
        selectContext.httpResponse = httpResponse ;
        return selectContext ;
    }


    public QueryResult makeQueryResult() {
        if (cacheable != null && cacheable && !ClusterUtils.getClusterService().getServerMode().equals("cache")) {
            String cacheKey = makeCacheKey() ;
            return ContextUtils.makeCacheResult(cacheKey, this) ;

        }
        return makeQueryResult(null, null);
    }


    public String makeCacheKey() {
        StringBuffer params = new StringBuffer() ;
        for(String key : httpRequest.getParameterMap().keySet()){
            params.append(key);
            params.append("=") ;
            params.append(httpRequest.getParameter(key)) ;
        }
        String keySrc = httpRequest.getRequestURI() + "?" + params;
        return keySrc;
    }

    public QueryResult makeQueryResult(Object result, String fieldName) {
        if(this.ifTest != null && !(this.ifTest.equalsIgnoreCase("true"))) {
            return null ;
        }
        if(resultType == ResultField.ResultType.LIST) {
            List<Map<String, Object>> resultList = null;
            try {
                resultList = this.jdbcTemplate.queryForList(this.sqlTemplate.format(data).toString(), this.sqlTemplate.getSqlParameterValues(data));
            }catch(EmptyResultDataAccessException e){
                resultList = new ArrayList<>();
            }
            this.result = resultList;

            List<QueryResult> subList = new ArrayList<>() ;
            for(Map<String, Object> resultData : resultList){
                QueryResult itemResult = new QueryResult() ;
                makeItemQueryResult(resultData, itemResult, data) ;
                subList.add(itemResult) ;
            }

            if (result != null && result instanceof Map) {
                ((Map) result).put(fieldName == null ? "items" : fieldName, subList);
                return null;
            } else {
                QueryResult queryResult = new QueryResult();
                queryResult.put("result", "200");
                queryResult.put("resultMessage", "SUCCESS");
                queryResult.put(fieldName == null ? "items" : fieldName, subList);
                return queryResult;
            }
        }else if(resultType == ResultField.ResultType.MERGE || resultType == ResultField.ResultType.VALUE){
            Map<String, Object> resultMap = null ;
            try {
                resultMap = this.jdbcTemplate.queryForMap(this.sqlTemplate.format(data).toString(), this.sqlTemplate.getSqlParameterValues(data)) ;
            }catch(EmptyResultDataAccessException e){
                resultMap = new HashMap<>() ;
            }

            QueryResult itemResult = new QueryResult() ;
            makeItemQueryResult(resultMap, itemResult, data) ;

            if(result != null && result instanceof Map){
                ((Map) result).putAll(itemResult) ;
                return null ;
            }else{
                return getQueryResult(itemResult);
            }
        }else{

            Map<String, Object> resultMap = null ;
            try {
                resultMap = this.jdbcTemplate.queryForMap(this.sqlTemplate.format(data).toString(), this.sqlTemplate.getSqlParameterValues(data));
            }catch(EmptyResultDataAccessException e){
                resultMap = new HashMap<>() ;
            }
            this.result = resultMap ;

            QueryResult itemResult = new QueryResult() ;
            makeItemQueryResult(resultMap, itemResult, data) ;

            if(result != null && result instanceof Map){
                ((Map) result).put(fieldName == null ? "item" : fieldName, itemResult) ;
                return null ;
            }else{
                return getQueryResult(itemResult);
            }
        }
    }

    private QueryResult getQueryResult(Map<String, Object> resultMap) {
        QueryResult queryResult = new QueryResult() ;
        queryResult.put("result", "200");
        queryResult.put("resultMessage", "SUCCESS");
        queryResult.putAll(resultMap);
        return queryResult ;
    }

    protected void makeItemQueryResult(Map<String, Object> resultData, QueryResult itemResult, Map<String, Object> contextData) {
        if(getResultFields() == null){
            for(String key : resultData.keySet()){
                itemResult.put(key, getResultValue(resultData, key));
            }
            return ;
        }

        for (ResultField resultField : getResultFields()) {
            if(resultField.getFieldName().equals("_all_")){
                for(String key : resultData.keySet()){
                    itemResult.put(key, getResultValue(resultData, key));
                }

            }else if (resultField.getContext() != null) {
                ReadContext subQueryContext = (ReadContext) resultField.getContext();
                if (resultData != null) {
                    subQueryContext.setNodeData(resultData);
                }
                subQueryContext.makeQueryResult(itemResult, resultField.getFieldName(), resultField.getResultType());
            }else if(resultField.getExecuteType() != null){
                Map<String, Object> _data = new HashMap<>();
                _data.putAll(contextData);
                _data.putAll(resultData);
                switch (resultField.getExecuteType()) {
                    case QUERY: {
                        ApiQueryContext apiQueryContext = ApiQueryContext.makeContextFromConfig(resultField.getFieldOption(), _data, httpRequest, httpResponse);
                        apiQueryContext.dateFormat = this.dateFormat ;
                        apiQueryContext.fileUrlFormat = this.fileUrlFormat ;
                        apiQueryContext.makeQueryResult(itemResult, resultField.getFieldName(), resultField.getResultType());
                        break ;
                    }
                    case SELECT: {
                        ApiSelectContext apiQueryContext = ApiSelectContext.makeContextFromConfig(resultField.getFieldOption(), _data, httpRequest, httpResponse);
                        apiQueryContext.dateFormat = this.dateFormat ;
                        apiQueryContext.fileUrlFormat = this.fileUrlFormat ;
                        apiQueryContext.makeQueryResult(itemResult, resultField.getFieldName());
                        break ;
                    }
                    case VALUE: {
                        itemResult.put(resultField.getFieldName(), ContextUtils.getValue(resultField.getStaticValue(), _data));
                        break ;
                    }
                    case OPTION: {
                        String fieldValue = resultField.getFieldValue();
                        fieldValue = fieldValue == null || org.apache.commons.lang3.StringUtils.isEmpty(fieldValue) ? resultField.getFieldName() : fieldValue;

                        FieldContext fieldContext = FieldContext.makeContextFromConfig(resultField.getFieldOption(), _data);
                        fieldContext.dateFormat = this.dateFormat ;
                        fieldContext.fileUrlFormat = this.fileUrlFormat ;

                        if(resultField.getFieldOption().get("propertyType") != null) {
                            String propertyType = (String) resultField.getFieldOption().get("propertyType");
                            NodeType _nodeType = NodeUtils.getNodeType(StringUtils.substringBefore(propertyType, ".")) ;
                            PropertyType pt = _nodeType.getPropertyType(StringUtils.substringAfter(propertyType, ".")) ;
                            if(StringUtils.isNotEmpty(pt.getReferenceType())){
                                fieldContext.nodeType = NodeUtils.getNodeType(pt.getReferenceType()) ;
                            }
                            if(fieldContext.referenceView == true && fieldContext.getResultFields() != null ){
                                fieldContext.referenceView = false ;
                                itemResult.put(resultField.getFieldName(), fieldContext.makeQueryResult(NodeUtils.getReferenceNode(getResultValue(resultData, fieldValue), pt)));
                            }else {
                                itemResult.put(resultField.getFieldName(), NodeUtils.getResultValue(fieldContext, pt, resultData, getResultValue(resultData, fieldValue)));
                            }
                        }else {
                            itemResult.put(resultField.getFieldName(), getResultValue(resultData, fieldValue));
                        }
                        break ;
                    }
                }
            } else {
                String fieldValue = resultField.getFieldValue();
                fieldValue = fieldValue == null || org.apache.commons.lang3.StringUtils.isEmpty(fieldValue) ? resultField.getFieldName() : fieldValue;
                itemResult.put(resultField.getFieldName(), getResultValue(resultData, fieldValue));
            }
        }
    }

    private Object getResultValue(Map<String, Object> resultData, String fieldValue) {
        Object value = resultData.get(fieldValue) ;
        if(value == null) return null ;

        if(value instanceof Date){
            return NodeUtils.getDateStringValue(value, getDateFormat());
        }
        return value ;
    }

    @Override
    public String getCacheTime() {
        return cacheTime;
    }

    @Override
    public QueryResult makeCacheResult() {
        return makeQueryResult(null, null);
    }
}
