package net.ion.ice.core.context;

import net.ion.ice.ApplicationContextManager;
import net.ion.ice.core.data.DBService;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeUtils;
import net.ion.ice.core.query.QueryResult;
import net.ion.ice.core.query.QueryTerm;
import net.ion.ice.core.query.QueryUtils;
import net.ion.ice.core.query.ResultField;
import org.springframework.jdbc.core.JdbcTemplate;
import org.stagemonitor.util.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * Created by jaehocho on 2017. 8. 24..
 */
public class ApiSelectContext extends ReadContext{
    protected Map<String, Object> config  ;

    protected String ds ;
    protected String sql ;
    protected String resultType ;

    protected JdbcTemplate jdbcTemplate ;
    protected Template sqlTemplate  ;

    public static ApiSelectContext makeContextFromConfig(Map<String, Object> config, Map<String, Object> data) {
        ApiSelectContext selectContext = new ApiSelectContext();

        selectContext.config = config ;
        selectContext.data = data ;


        Map<String, Object> select = (Map<String, Object>) config.get("select");
        selectContext.ds = (String) select.get("ds");
        selectContext.sql = (String) select.get("sql");
        selectContext.resultType = (String) select.get("resultType");

        DBService dbService = ApplicationContextManager.getBean(DBService.class) ;
        selectContext.jdbcTemplate = dbService.getJdbcTemplate(selectContext.ds) ;
        selectContext.sqlTemplate = new Template(selectContext.sql) ;
        selectContext.sqlTemplate.parsing();


        return selectContext;
    }


    public QueryResult makeQueryResult(Object result, String fieldName) {
        if(resultType.equals("list")){
            List<Map<String, Object>> resultList = this.jdbcTemplate.queryForList(this.sqlTemplate.format(data), this.sqlTemplate.getSqlParameterValues(data)) ;
            if(result != null && result instanceof Map){
                ((Map) result).put(fieldName == null ? "items" : fieldName, resultList) ;
                return null ;
            }else{
                QueryResult queryResult = new QueryResult() ;
                queryResult.put(fieldName == null ? "items" : fieldName, resultList) ;
                return queryResult ;
            }
        }else{
            Map<String, Object> resultMap = this.jdbcTemplate.queryForMap(this.sqlTemplate.format(data), this.sqlTemplate.getSqlParameterValues(data)) ;
            if(result != null && result instanceof Map){
                ((Map) result).put(fieldName == null ? "item" : fieldName, resultMap) ;
                return null ;
            }else{
                QueryResult queryResult = new QueryResult() ;
                queryResult.putAll(resultMap); ;
                return queryResult ;
            }
        }
    }
}
