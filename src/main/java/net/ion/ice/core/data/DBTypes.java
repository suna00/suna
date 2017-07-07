package net.ion.ice.core.data;

/**
 * Created by seonwoong on 2017. 7. 4..
 */
public enum DBTypes {
    oracle("oracle.jdbc.OracleDriver"),
    maria("org.mariadb.jdbc.Driver"),
    mysql("com.mysql.jdbc.Driver");

    private String driverClass;

    DBTypes(String driverClass) {
        this.driverClass = driverClass;
    }

    public String getDriverClassName() {
        return driverClass;
    }


}
