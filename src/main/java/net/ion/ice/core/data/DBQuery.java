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
                if(queryTerm.getMethodQuery().equals("IN") || queryTerm.getMethodQuery().equals("EXISTS")){
                    String[] values = queryTerm.getQueryValue().split(",");
                    List<String> holder = new ArrayList<>();
                    for(String value : values){
                        holder.add("?");
                        searchListValue.add(value);
                    }
                    String query = String.format("%s %s (%s)", queryTerm.getQueryKey(), queryTerm.getMethodQuery(), StringUtils.join(holder, ", "));
                    searchListQuery.add(query);
                }else if(queryTerm.getMethodQuery().equals("BETWEEN")){
                    String query = String.format("(%s %s ? AND ?)", queryTerm.getQueryKey(), queryTerm.getMethodQuery(), StringUtils.substringBefore(queryTerm.getQueryValue(),"~"), StringUtils.substringAfter(queryTerm.getQueryValue(),"~"));
                    String[] values = queryTerm.getQueryValue().split("~");
                    for(String value : values){
                        searchListValue.add(value);
                    }
                    searchListQuery.add(query);
                }else{
                    String value = queryTerm.getQueryValue();
                    String term = queryTerm.getMethodQuery();
                    if("null".equals(value)){
                        term = "is";
                        if(queryTerm.getMethodQuery().contains("!")){
                            term += " not ";
                        }
                        String query = String.format("%s %s null", queryTerm.getQueryKey(), term);
                        searchListQuery.add(query);
                    }else{
                        String query = String.format("%s %s ?", queryTerm.getQueryKey(), term);
                        searchListQuery.add(query);
                        searchListValue.add(value);
                    }
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
