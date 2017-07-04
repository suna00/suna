package net.ion.ice.core.data;

/**
 * Created by seonwoong on 2017. 7. 4..
 */
public enum DBType {
    ORACLE("oracle.jdbc.OracleDriver"),
    MARIA("org.mariadb.jdbc.Driver"),
    MYSQL("com.mysql.jdbc.Driver");

    private String driverClass;

    DBType(String driverClass) {
        this.driverClass = driverClass;
    }

    public String getDriverClass() {
        return driverClass;
    }

}
