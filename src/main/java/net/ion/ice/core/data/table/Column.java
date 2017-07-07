package net.ion.ice.core.data.table;

public class Column {

    private String columnName;
    private Boolean isPk;
    private String dataType;
    private Integer dataLength;
    private Boolean isNullable;
    private Integer sqlType;
    private Object defaultValue;

    public Column() {

    }

    public Column(String columnName, Boolean isPk, String dataType, Integer dataLength, Boolean isNullable, Integer sqlType, Object defaultValue) {
        this.columnName = columnName;
        this.isPk = isPk;
        this.dataType = dataType;
        this.dataLength = dataLength;
        this.isNullable = isNullable;
        this.sqlType = sqlType;
        this.defaultValue = defaultValue;
    }

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public Boolean getPk() {
        return isPk;
    }

    public void setPk(Boolean pk) {
        isPk = pk;
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    public Integer getDataLength() {
        return dataLength;
    }

    public void setDataLength(Integer dataLength) {
        this.dataLength = dataLength;
    }

    public Boolean getNullable() {
        return isNullable;
    }

    public void setNullable(Boolean nullable) {
        isNullable = nullable;
    }

    public Integer getSqlType() {
        return sqlType;
    }

    public void setSqlType(Integer sqlType) {
        this.sqlType = sqlType;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(Object defaultValue) {
        this.defaultValue = defaultValue;
    }
}
