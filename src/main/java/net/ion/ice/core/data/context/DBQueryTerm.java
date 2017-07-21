package net.ion.ice.core.data.context;

/**
 * Created by seonwoong on 2017. 7. 20..
 */
public class DBQueryTerm {
    private String key;
    private String value;
    private DBQueryMethod method;

    public DBQueryTerm(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public DBQueryTerm(String key, String method, String value) {
        this.key = key;
        this.method = DBQueryMethod.valueOf(method.toUpperCase());
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public DBQueryMethod getMethod() {
        return method;
    }

    public String getMethodQuery() {
        return method.getQueryString();
    }

    public enum DBQueryMethod {
        MATCHING("LIKE"),
        EQUALS("="),
        ABOVE(">="),
        BELOW("<="),
        EXCESS(">"),
        UNDER("<");


        private String queryString;

        DBQueryMethod(String queryString) {
            this.queryString = queryString;
        }

        String getQueryString() {
            return queryString;
        }
    }


}
