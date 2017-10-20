package net.ion.ice.plugin.excel;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.List;
import java.util.Map;


@Service("excelService")
public class ExcelService {
    private Logger logger = LoggerFactory.getLogger(ExcelService.class);

    public static final String DEFAULT_FILE_NAME = "export";
    public static final String DEFAULT_EXTENSION = "xlsx";

    public void downloadForm(HttpServletRequest request, HttpServletResponse response) {
        Map<String, String[]> parameterMap = request.getParameterMap();

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

                CellStyle cellStyle = getHeaderCellStyle(workbook);

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

            response.setHeader(HttpHeaders.CONTENT_TYPE, "application/vnd.ms-excel");
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\""+encodingFileName(request, fileName)+"\"."+extension);
            outputStream = response.getOutputStream();
            workbook.write(outputStream);
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
    }

    private String encodingFileName(HttpServletRequest request, String fileName) {
        String encodingFilenName = fileName;
        String header = request.getHeader("User-Agent");
        String browser = "Firefox";

        if (StringUtils.contains(header, "MSIE")) {
            browser = "MSIE";
        } else if(StringUtils.contains(header, "Chrome")) {
            browser = "Chrome";
        } else if(StringUtils.contains(header, "Opera")) {
            browser = "Opera";
        }

        try {
            if (StringUtils.equals(browser, "MSIE")) {
                encodingFilenName = URLEncoder.encode(fileName,"UTF-8").replaceAll("\\+", "%20");
            } else if (StringUtils.equals(browser, "Firefox")) {
                encodingFilenName = new String(fileName.getBytes("UTF-8"), "ISO-8859-1");
            } else if (StringUtils.equals(browser, "Opera")) {
                encodingFilenName = new String(fileName.getBytes("UTF-8"), "ISO-8859-1");
            } else if (StringUtils.equals(browser, "Chrome")) {
                encodingFilenName = new String(fileName.getBytes("UTF-8"), "ISO-8859-1");
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

        return encodingFilenName;
    }

    private HSSFWorkbook getXlsWorkbook() {
        return new HSSFWorkbook();
    }

    private XSSFWorkbook getXlsxWorkbook() {
        return new XSSFWorkbook();
    }

    private CellStyle getHeaderCellStyle(Workbook workbook) {
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 15);

        CellStyle cellStyle = workbook.createCellStyle();
        cellStyle.setBorderTop(BorderStyle.THIN);
        cellStyle.setBorderRight(BorderStyle.THIN);
        cellStyle.setBorderBottom(BorderStyle.THIN);
        cellStyle.setBorderLeft(BorderStyle.THIN);
        cellStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        cellStyle.setAlignment(HorizontalAlignment.CENTER);
        cellStyle.setFont(font);

        return cellStyle;
    }
}
