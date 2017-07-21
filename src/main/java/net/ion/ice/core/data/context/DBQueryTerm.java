package net.ion.ice.core.data.context;

/**
 * Created by seonwoong on 2017. 7. 20..
 */
public class DBQueryTerm {
    private String key;
    private String value;
    private DBQueryMethod method;


    public DBQueryTerm(String key, String value, String method) {
        this.key = key;
        this.value = value;
        this.method = DBQueryMethod.valueOf(method.toUpperCase());
    }

    public enum DBQueryMethod {
        MATCHING("LIKE"),
        ABOVE("<="),
        BELOW(">="),
        EXCESS("<"),
        UNDER(">");


        private String queryString;

        DBQueryMethod(String queryString) {
            this.queryString = queryString;
        }

        String getQueryString() {
            return queryString;
        }
    }


}
