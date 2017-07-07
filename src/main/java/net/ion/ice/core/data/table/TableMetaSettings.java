package net.ion.ice.core.data.table;

import lombok.Data;
import net.ion.ice.core.data.DBTypes;
import net.ion.ice.core.node.PropertyType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by seonwoong on 2017. 6. 29..
 */

public class TableMetaSettings {

    public String setOracleDataTypes(PropertyType.ValueType valueType) {
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

    public String setMysqlDataTypes(PropertyType.ValueType valueType) {
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
