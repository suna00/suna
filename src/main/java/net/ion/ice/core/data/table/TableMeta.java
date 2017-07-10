package net.ion.ice.core.data.table;

import net.ion.ice.core.data.DBTypes;
import net.ion.ice.core.node.PropertyType;
import org.springframework.context.annotation.Configuration;

/**
 * Created by seonwoong on 2017. 6. 29..
 */

public class TableMeta {

    private String tableName;
    private String DBType;

    public TableMeta() {
    }

    public TableMeta(String DBType) {
        this.DBType = DBType;
    }

    public TableMeta(String tableName, String DBType) {
        this.tableName = tableName;
        this.DBType = DBType;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getDBType() {
        return DBType;
    }

    public void setDBType(String DBType) {
        this.DBType = DBType;
    }

    public String getDBDataType(PropertyType.ValueType valueType) {
        if(DBType.equalsIgnoreCase(DBTypes.oracle.name())){
            return oracleDataTypes(valueType);
        }else{
            return mySqlDataTypes(valueType);
        }
    }

    public String oracleDataTypes(PropertyType.ValueType valueType) {
        switch (valueType) {
            case INT:
            case LONG:
            case DOUBLE:
            case BOOLEAN:
                return "NUMBER";

            case DATE:
                return "DATE";

            case TEXT:
                return "CLOB";

            case STRING:
                return "VARCHAR2";

            default:
                return "VARCHAR2";
        }
    }

    public String mySqlDataTypes(PropertyType.ValueType valueType) {
        switch (valueType) {
            case INT:
            case BOOLEAN:
                return "INT";

            case LONG:
                return "BIGINT";

            case DOUBLE:
                return "DOUBLE";

            case DATE:
                return "DATE";

            case TEXT:
                return "BLOB";

            case STRING:
                return "VARCHAR";

            default:
                return "VARCHAR";
        }
    }

}
