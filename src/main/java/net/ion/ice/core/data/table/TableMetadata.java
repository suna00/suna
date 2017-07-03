package net.ion.ice.core.data.table;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by seonwoong on 2017. 6. 29..
 */

@Data
public class TableMetadata {
    private String tableNm;
    private List<Column> cols;
    private String pk;

    public List<String> getColumnNames() {
        List<String> colNames = new ArrayList<String>();
        for (Column col : cols) {
            colNames.add(col.getColumnName());
        }
        return colNames;
    }
}
