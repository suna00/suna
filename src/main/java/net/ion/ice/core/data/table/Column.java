package net.ion.ice.core.data.table;

import lombok.Data;

@Data
public class Column {

    private String columnName;
    private Boolean isPk;
    private String dataType;
    private Integer dataLength;
    private Boolean isNullable;
    private Integer sqlType;
    private String dataDefault;

    public Column(String columnName, Boolean isPk, String dataType, Integer dataLength, Boolean isNullable, Integer sqlType, String dataDefault) {
        this.columnName = columnName;
        this.isPk = isPk;
        this.dataType = dataType;
        this.dataLength = dataLength;
        this.isNullable = isNullable;
        this.sqlType = sqlType;
        this.dataDefault = dataDefault;
    }
}
