package net.ion.ice.cjmwave.db.sync.utils;

import net.logstash.logback.encoder.org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.*;

/**
 * Created by juneyoungoh on 2017. 9. 16..
 */
public class QueryUtils {

    private static final Logger LOGGER = Logger.getLogger(QueryUtils.class);

    /*
    * 인서트 쿼리와 맵에 담긴 데이터를 순서에 맞게 정렬함
    * */
    public static Map<String, Object> prepareInsertQuery (String table, Map<String, Object> params) {
        Map<String, Object> rtn = new HashMap<String, Object>();
        StringBuilder sb = new StringBuilder();
        sb.append("INSERT INTO ").append(table).append(" ").append("(");
        ArrayList<String> qFields = new ArrayList<>();
        ArrayList<String> qMarks = new ArrayList<>();

        Iterator<String> iter = params.keySet().iterator();
        while(iter.hasNext()) {
            String key = iter.next();
            qFields.add(key);
            qMarks.add("?");
        }

        sb.append(StringUtils.join(qFields, ","))
                .append(") VALUES (")
                .append(StringUtils.join(qMarks, ","))
                .append(")");

        String insert = sb.toString();
        rtn.put("insertQ", insert);
        rtn.put("orderedParameterKeys", qFields);
        LOGGER.info(
                "\nGENERATED INSERT QUERY [ " + insert + " ]\n"
                + "\nOrderedFields :: " + qFields);
        return rtn;
    }

    /*
    * 업데이트 쿼리와 맵에 담긴 데이터를 순서에 맞게 정렬함
    * 테이블에 PK 가 없으면 에러남
    * */
    public static Map<String, Object> prepareUpdateQuery (JdbcTemplate template, String tableName, Map<String, Object> params) throws Exception {

        Map<String, Object> rtn = new HashMap<String, Object>();
        // 어차피 PK 없는 테이블은 삑구임
        String findPKQuery = "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE WHERE TABLE_NAME = ?";
        List<Map<String, Object>> pks = template.queryForList(findPKQuery, tableName);
        List<String> pkFields = new ArrayList<>();

        for(Map<String, Object> pk : pks) {
            pkFields.add(pk.get("COLUMN_NAME").toString()); // 여기서는 toString 이 안되면 throw 되어줘야 함
        }

        StringBuilder sb = new StringBuilder();
        sb.append("UPDATE ").append(tableName).append(" SET ");

        ArrayList<String> qFields = new ArrayList<>();
        ArrayList<String> qQueries = new ArrayList<>();

        Iterator<String> iter = params.keySet().iterator();
        while(iter.hasNext()) {
            String key = iter.next();
            qFields.add(key);
            qQueries.add(key + "= ?");
        }
        sb.append(StringUtils.join(qQueries, ",")).append(" WHERE ");

        for(int i = 0; i < pkFields.size(); i++) {
            sb.append(pkFields.get(i)).append("=?");
            qFields.add(pkFields.get(i));
            if(i < pkFields.size() -1) {
                // 마지막이 아닐 때
                sb.append(" AND ");
            }
        }
        String update = sb.toString();
        rtn.put("updateQ", update);
        rtn.put("orderedParameterKeys", qFields);
        LOGGER.info(
                "\nGENERATED UPDATE QUERY [ " +update + " ]\n"
                        + "\nOrderedFields :: " + qFields);
        return rtn;
    }

    // 단독으로 쓸 수 없음
    public static Object[] prepareQueryParams (List<String> qFields, Map<String, Object> dataMap) {
        List<Object> qParams = new ArrayList<>();
        for(String qField : qFields) {
            qParams.add(dataMap.get(qField));
        }
        return qParams.toArray();
    }

}
