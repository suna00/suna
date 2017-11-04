package net.ion.ice.plugin.excel;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.*;


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
        String headerNames = parameterMap.get("headerNames") == null ? "" : parameterMap.get("headerNames")[0];

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

                List<String> headerNameList = Arrays.asList(StringUtils.split(headerNames, ","));
                for (String headerName : headerNameList) {
                    String convertHeaderName = "";
                    if (StringUtils.contains(headerName, ".")) {
                        String[] headerSplit = StringUtils.split(headerName, ".");
                        if (StringUtils.equals(sheetName, headerSplit[0])) {
                            convertHeaderName = headerSplit[1];
                        }
                    } else {
                        convertHeaderName = headerName;
                    }

                    if (!StringUtils.isEmpty(convertHeaderName)) {
                        Cell cell = row.createCell(cellIndex);
                        cell.setCellValue(convertHeaderName);
                        cell.setCellStyle(cellStyle);

                        sheet.autoSizeColumn(cellIndex);

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
            if (outputStream != null) {
                try {
                    outputStream.flush();
                    outputStream.close();
                } catch (IOException e) {
                    logger.error(e.getMessage(), e);
                }
            }
            if (workbook != null) {
                try {
                    workbook.close();
                } catch (IOException e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }
    }

    public Map<String, Object> parsingExcelFile(MultipartFile file) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (file == null) return result;

        String originalFileName = file.getOriginalFilename();
        String extension = StringUtils.substringAfterLast(originalFileName, ".");

        Workbook workbook = null;
        InputStream is = null;

        try {
            is = file.getInputStream();
            workbook = StringUtils.equals(extension, "xls") ? getXlsWorkbook(is) : getXlsxWorkbook(is);

            if (workbook != null) {
                int totalSheetCount = workbook.getNumberOfSheets();

                for (int i=0; i<totalSheetCount; i++) {
                    Sheet sheet = workbook.getSheetAt(i);

                    List<String> columnNameList = new ArrayList<>();

                    Iterator<Row> rowIterator = sheet.iterator();
                    if (rowIterator.hasNext()) {
                        Row row = rowIterator.next();
                        Iterator<Cell> cellIterator = row.iterator();
                        while (cellIterator.hasNext()) {
                            Cell cell = cellIterator.next();
                            columnNameList.add(getStringValue(cell));
                        }
                    }

                    List<Object> dataList = new ArrayList<>();

                    rowIterator = sheet.iterator();
                    int rowIndex = 0;
                    while (rowIterator.hasNext()) {
                        if (rowIndex == 0) {
                            rowIterator.next();
                        } else {
                            Row row = rowIterator.next();
                            Map<String, String> data = new LinkedHashMap<>();
                            Iterator<Cell> cellIterator = row.iterator();
                            int cellIndex = 0;
                            while (cellIterator.hasNext() && cellIndex <= columnNameList.size()) {
                                Cell cell = cellIterator.next();
                                data.put(columnNameList.get(cellIndex) , getStringValue(cell));
                                cellIndex++;
                            }

                            dataList.add(data);
                        }
                        rowIndex++;
                    }

                    result.put(sheet.getSheetName(), dataList);
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }
            if (workbook != null) {
                try {
                    workbook.close();
                } catch (IOException e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }

        return result;
    }

    public String getStringValue(Cell cell) {
        String value = "";

        switch (cell.getCellTypeEnum()) {
            case STRING:
                value = cell.getStringCellValue();
                break;
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    FastDateFormat fdf = FastDateFormat.getInstance("yyyyMMddHHmmss");
                    value = fdf.format(cell.getDateCellValue());
                } else {
                    value = String.valueOf(cell.getNumericCellValue());
                    if (StringUtils.contains(value, ".") && StringUtils.equals(StringUtils.split(value, ".")[1], "0")) {
                        value = StringUtils.substringBefore(value, ".");
                    }
                }
                break;
            case BOOLEAN:
                value = String.valueOf(cell.getBooleanCellValue());
                break;
            case FORMULA:
                value = cell.getCellFormula();
                break;
            default:
        }

        return value;
    }

    public String encodingFileName(HttpServletRequest request, String fileName) {
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

    public HSSFWorkbook getXlsWorkbook() {
        return new HSSFWorkbook();
    }

    public HSSFWorkbook getXlsWorkbook(InputStream is) {
        HSSFWorkbook workbook = null;
        try {
            workbook = new HSSFWorkbook(is);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return workbook;
    }

    public XSSFWorkbook getXlsxWorkbook() {
        return new XSSFWorkbook();
    }

    public XSSFWorkbook getXlsxWorkbook(InputStream is) {
        XSSFWorkbook workbook = null;
        try {
            workbook = new XSSFWorkbook(is);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return workbook;
    }

    public CellStyle getHeaderCellStyle(Workbook workbook) {
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
