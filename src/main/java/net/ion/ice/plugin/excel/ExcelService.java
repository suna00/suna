package net.ion.ice.plugin.excel;

import net.ion.ice.core.file.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;


@Service("excelService")
public class ExcelService {
    private Logger logger = LoggerFactory.getLogger(ExcelService.class);

    public static final String DEFAULT_FILE_NAME = "export";
    public static final String DEFAULT_EXTENSION = "xlsx";

    public ResponseEntity downloadForm(Map<String, String[]> parameterMap, HttpServletResponse response) {
        String fileName = parameterMap.get("fileName") == null ? DEFAULT_FILE_NAME : parameterMap.get("fileName")[0];
        String extension = parameterMap.get("extension") == null ? DEFAULT_EXTENSION : parameterMap.get("extension")[0];
        String sheetNames = parameterMap.get("sheetNames") == null ? "" : parameterMap.get("sheetNames")[0];
        String headers = parameterMap.get("headers") == null ? "" : parameterMap.get("headers")[0];

        Workbook workbook = null;
        OutputStream outputStream = null;

        try {
            workbook = StringUtils.equals(extension, "xls") ? getXlsWorkbook() : getXlsxWorkbook();

            List<String> sheetNameList = Arrays.asList(StringUtils.split(sheetNames, ","));

            for (String sheetName : sheetNameList) {
                Sheet sheet = workbook.createSheet(sheetName);

                CellStyle cellStyle = StringUtils.equals(extension, "xls") ? getXlsHeaderCellStyle(workbook) : getXlsxHeaderCellStyle(workbook);

                Row row = sheet.createRow(0);
                int cellIndex = 0;

                List<String> headerList = Arrays.asList(StringUtils.split(headers, ","));
                for (String header : headerList) {
                    String headerName = "";
                    if (StringUtils.contains(header, ".")) {
                        String[] headerSplit = StringUtils.split(header, ".");
                        if (StringUtils.equals(sheetName, headerSplit[0])) {
                            headerName = headerSplit[1];
                        }
                    } else {
                        headerName = header;
                    }

                    if (!StringUtils.isEmpty(headerName)) {
                        Cell cell = row.createCell(cellIndex);
                        cell.setCellValue(headerName);
                        cell.setCellStyle(cellStyle);
                        cellIndex++;
                    }
                }
            }

            outputStream = response.getOutputStream();
            workbook.write(outputStream);

            return ResponseEntity
                    .ok()
                    .header(HttpHeaders.CONTENT_TYPE, "application/vnd.ms-excel")
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\""+fileName+"\"")
                    .body(outputStream);

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        } finally {
            if (workbook != null) {
                try {
                    workbook.close();
                } catch (IOException e) {
                    logger.error(e.getMessage(), e);
                }
            }
            if (outputStream != null) {
                try {
                    outputStream.flush();
                    outputStream.close();
                } catch (IOException e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }

        return null;
    }

    private HSSFWorkbook getXlsWorkbook() {
        return new HSSFWorkbook();
    }

    private CellStyle getXlsHeaderCellStyle(Workbook workbook) {
        CellStyle cellStyle = workbook.createCellStyle();
        cellStyle.setFillBackgroundColor(HSSFColor.HSSFColorPredefined.GREY_25_PERCENT.getIndex());
        return cellStyle;
    }

    private XSSFWorkbook getXlsxWorkbook() {
        return new XSSFWorkbook();
    }

    private CellStyle getXlsxHeaderCellStyle(Workbook workbook) {
        CellStyle cellStyle = workbook.createCellStyle();
        cellStyle.setFillBackgroundColor(HSSFColor.HSSFColorPredefined.GREY_25_PERCENT.getIndex());
        return cellStyle;
    }
}
