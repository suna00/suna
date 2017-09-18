package net.ion.ice.core.data;

import net.ion.ice.core.context.QueryContext;
import net.ion.ice.core.query.QueryTerm;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class DBQuery {

    private String tableName = "";
    private String listParamSql= "";
    private String totalCountSql= "";

    private List<String> searchListQuery;
    private List<Object> searchListValue;
    private List<Object> resultCountValue;

    public DBQuery(String tableName, QueryContext queryContext) {
        searchListQuery = new ArrayList<>();
        searchListValue = new ArrayList<>();

        if (queryContext.getQueryTerms() != null && !queryContext.getQueryTerms().isEmpty()) {
            for (QueryTerm queryTerm : queryContext.getQueryTerms()) {
                if(queryTerm.getMethodQuery().equals("IN")){
                    String[] values = queryTerm.getQueryValue().split(",");
                    List<String> holder = new ArrayList<>();
                    for(String value : values){
                        holder.add("?");
                        searchListValue.add(value);
                    }
                    String query = String.format("%s %s (%s)", queryTerm.getQueryKey(), queryTerm.getMethodQuery(), StringUtils.join(holder, ", "));
                    searchListQuery.add(query);
                }else{
                    String query = String.format("%s %s ?", queryTerm.getQueryKey(), queryTerm.getMethodQuery());
                    String value = queryTerm.getQueryValue();
                    searchListQuery.add(query);
                    searchListValue.add(value);
                }
            }

            listParamSql = String.format("SELECT * FROM %s WHERE %s", tableName, StringUtils.join(searchListQuery.toArray(), " AND "));
            totalCountSql = String.format("SELECT COUNT(*) as totalCount FROM %s WHERE %s", tableName, StringUtils.join(searchListQuery.toArray(), " AND "));

            resultCountValue = new ArrayList<>(searchListValue);

        } else {
            listParamSql = String.format("SELECT * FROM %s", tableName);
            totalCountSql = String.format("SELECT COUNT(*) as totalCount FROM %s", tableName);

            resultCountValue = new ArrayList<>();
        }

        if (queryContext.hasSorting()) {
            listParamSql = listParamSql.concat(String.format(" ORDER BY ").concat(queryContext.getSorting()));
        }

        listParamSql = listParamSql.concat(String.format(" LIMIT ?").concat(String.format(" OFFSET ?")));
        if(queryContext.isPaging()) {
            searchListValue.add(queryContext.getPageSize());
        }else{
            searchListValue.add(queryContext.getLimit());
        }
        searchListValue.add(queryContext.getOffset());
    }

    public String getTableName() {
        return tableName;
    }

    public String getListParamSql() {
        return listParamSql;
    }

    public String getTotalCountSql() {
        return totalCountSql;
    }

    public List<String> getSearchListQuery() {
        return searchListQuery;
    }

    public List<Object> getSearchListValue() {
        return searchListValue;
    }

    public List<Object> getResultCountValue() {
        return resultCountValue;
    }
}
