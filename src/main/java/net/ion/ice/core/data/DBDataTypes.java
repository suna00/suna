package net.ion.ice.core.data;

import net.ion.ice.core.data.DBTypes;
import net.ion.ice.core.node.PropertyType;
import org.springframework.context.annotation.Configuration;

/**
 * Created by seonwoong on 2017. 6. 29..
 */

public class DBDataTypes {

    public static String convertDataType(String DBType, PropertyType.ValueType valueType) {
        if(DBType.equalsIgnoreCase(DBTypes.oracle.name())){
            return oracleDataTypes(valueType);
        }else{
            return mySqlDataTypes(valueType);
        }
    }

    public static String oracleDataTypes(PropertyType.ValueType valueType) {
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

    public static String mySqlDataTypes(PropertyType.ValueType valueType) {
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
