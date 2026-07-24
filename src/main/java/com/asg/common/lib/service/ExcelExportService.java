package com.asg.common.lib.service;

import com.asg.common.lib.dto.excel.*;
import com.asg.common.lib.repository.ExcelExportRepository;
import com.asg.common.lib.security.util.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.sql.Blob;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExcelExportService {

    private final ExcelExportRepository repository;
    private static final String FILE_SEPARATOR = File.separator;
    private String exportCurrency = "BHD";

    @Value("${db.connection:oracle}")
    private String dbConnection;

    private boolean isPostgres() {
        return "postgres".equalsIgnoreCase(dbConnection);
    }

    public ExcelFileData generateExcel(String docId, String docKeyPoid, Map<String, Object> parameters, String outputFileName) {
        String parametersString = convertParametersToString(parameters);
        return generateExcel(docId, docKeyPoid, parametersString, outputFileName);
    }

    private ExcelFileData generateExcel(String docId, String docKeyPoid, String parameters, String outputFileName) {
        Long groupPoid = UserContext.getGroupPoid();
        Long companyPoid = UserContext.getCompanyPoid();
        Long userPoid = UserContext.getUserPoid();
        List<ExcelSheetConfig> sheets = repository.getExcelConfig(groupPoid, companyPoid, userPoid, docId, docKeyPoid, parameters);
        
        try (XSSFWorkbook workbook = createWorkbook(sheets.getFirst());
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            
            for (ExcelSheetConfig sheet : sheets) {
                processSheet(workbook, sheet);
            }
            
            workbook.write(baos);
            log.info("Excel generated: {}", outputFileName);
            
            return ExcelFileData.builder()
                .content(baos.toByteArray())
                .fileName(outputFileName)
                .build();
        } catch (Exception e) {
            log.error("Excel generation failed", e);
            throw new RuntimeException("Excel generation failed: " + e.getMessage(), e);
        }
    }

    private XSSFWorkbook createWorkbook(ExcelSheetConfig firstSheet) throws IOException {
        if (firstSheet.getExcelTemplateFile() != null) {
            InputStream is = getClass().getResourceAsStream(
                    "/ExcelTemplates/" + firstSheet.getExcelTemplateFile());
            if (is != null) {
                return new XSSFWorkbook(is);
            }
        }
        return new XSSFWorkbook();
    }

    private void processSheet(XSSFWorkbook workbook, ExcelSheetConfig config) throws Exception {
        XSSFSheet sheet = workbook.getSheet(config.getSheetName());
        if (sheet == null && config.getSheetId() != null) {
            int sheetIndex = Integer.parseInt(config.getSheetId()) - 1;
            if (sheetIndex >= 0 && sheetIndex < workbook.getNumberOfSheets()) {
                sheet = workbook.getSheetAt(sheetIndex);
            }
        }
        if (sheet == null) {
            sheet = workbook.createSheet(config.getSheetName());
        }
        if (Boolean.TRUE.equals(config.getShowHeaderImage())) {
            writeHeaderImage(workbook, sheet, config.getHeaderImageBlob(), config.getHeaderImageEndRow());
        }
        if (config.getHeaderData() != null) {
            writeHeaderData(sheet, config);
        }
        if (config.getDetailData() != null) {
            writeDetailData(sheet, config);
        }
        if (config.getFooterData() != null) {
            writeFooterData(sheet, config);
        }
    }

    private void writeHeaderImage(XSSFWorkbook workbook, XSSFSheet sheet, Blob imageBlob, Integer endRow) throws Exception {
        if (imageBlob == null) return;

        byte[] imageBytes = imageBlob.getBytes(1, (int) imageBlob.length());
        int pictureIdx = workbook.addPicture(imageBytes, Workbook.PICTURE_TYPE_PNG);

        Drawing<?> drawing = sheet.createDrawingPatriarch();
        CreationHelper helper = workbook.getCreationHelper();
        ClientAnchor anchor = helper.createClientAnchor();
        
        anchor.setCol1(0);
        anchor.setRow1(0);
        anchor.setCol2(endRow != null ? endRow : 10);
        anchor.setRow2(4);
        anchor.setAnchorType(ClientAnchor.AnchorType.MOVE_AND_RESIZE);

        drawing.createPicture(anchor, pictureIdx);
    }

    private void writeHeaderData(XSSFSheet sheet, ExcelSheetConfig config) {
        for (Map.Entry<String, Object> entry : config.getHeaderData().entrySet()) {
            writeCellData(sheet, entry.getKey(), entry.getValue(), config, null);
        }
    }

    private void writeDetailData(XSSFSheet sheet, ExcelSheetConfig config) {
        int startRow = config.getDtl1StartRow() != null ? config.getDtl1StartRow() - 1 : 15;
        int currentRow = startRow;

        if (Boolean.TRUE.equals(config.getDtl1ShowColHdr())) {
            writeDetailHeader(sheet, config, currentRow);
            currentRow++;
        }

        for (Map<String, Object> rowData : config.getDetailData()) {
            for (Map.Entry<String, Object> entry : rowData.entrySet()) {
                writeDetailCell(sheet, entry.getKey(), entry.getValue(), config, currentRow);
            }
            currentRow++;
        }
    }

    private void writeDetailHeader(XSSFSheet sheet, ExcelSheetConfig config, int rowNum) {
        Integer firstCol = null, lastCol = null;
        
        for (ExcelColumnConfig colConfig : config.getColumnConfigMap().values()) {
            int colNum = colConfig.getColRef();
            if (firstCol == null || colNum < firstCol) firstCol = colNum;
            if (lastCol == null || colNum > lastCol) lastCol = colNum;
            
            Row row = getOrCreateRow(sheet, rowNum);
            Cell cell = getOrCreateCell(row, colNum - 1, CellType.STRING);
            cell.setCellValue(toTitleCase(colConfig.getFieldName().replace("_", " ")));
        }

        if (firstCol != null && lastCol != null) {
            formatDetailHeader(sheet, rowNum, firstCol - 1, lastCol - 1);
        }
    }

    private void writeDetailCell(XSSFSheet sheet, String fieldName, Object value, ExcelSheetConfig config, int rowNum) {
        ExcelColumnConfig colConfig = config.getColumnConfigMap().get(fieldName.toUpperCase());
        if (colConfig == null || value == null) return;

        Row row = getOrCreateRow(sheet, rowNum);
        int colNum = colConfig.getColRef() - 1;
        CellType cellType = getCellType(colConfig.getCellFormatType());
        Cell cell = getOrCreateCell(row, colNum, cellType);

        if (cellType == CellType.NUMERIC) {
            cell.setCellValue(Double.parseDouble(value.toString()));
            applyCellStyle(sheet, cell, colConfig);
        } else {
            cell.setCellValue(value.toString());
        }
    }

    private void writeFooterData(XSSFSheet sheet, ExcelSheetConfig config) {
        int lastRow = sheet.getLastRowNum();
        for (Map.Entry<String, Object> entry : config.getFooterData().entrySet()) {
            writeCellData(sheet, entry.getKey(), entry.getValue(), config, lastRow);
        }
    }

    private void writeCellData(XSSFSheet sheet, String fieldName, Object value, ExcelSheetConfig config, Integer lastRowNum) {
        if (value == null) return;

        ExcelColumnConfig colConfig = config.getColumnConfigMap().get(fieldName.toUpperCase());
        if (colConfig == null) return;

        int rowNum = colConfig.getRowRef() - 1;

        Row row = getOrCreateRow(sheet, rowNum);
        int colNum = colConfig.getColRef() - 1;
        Cell cell = getOrCreateCell(row, colNum, CellType.STRING);
        cell.setCellValue(value.toString());

        if (config.getExcelTemplateFile() == null) {
            applyCellStyle(sheet, cell, colConfig);
        }

        if (Boolean.TRUE.equals(colConfig.getShowCaption()) && colNum >= 1) {
            Cell captionCell = getOrCreateCell(row, colNum - 1, CellType.STRING);
            String caption = toTitleCase(fieldName.replace("_", " "));
            if (colConfig.getCellFormatType() == CellFormatType.TOTAL_CURRENCY_FORMAT) {
                caption += "(" + exportCurrency + ")";
            }
            captionCell.setCellValue(caption);
        }
    }

    private void applyCellStyle(XSSFSheet sheet, Cell cell, ExcelColumnConfig config) {
        XSSFWorkbook workbook = sheet.getWorkbook();
        XSSFCellStyle style = workbook.createCellStyle();
        XSSFFont font = workbook.createFont();

        switch (config.getCellFormatType()) {
            case TITLE_FORMAT:
                font.setFontHeightInPoints((short) 18);
                font.setBold(true);
                style.setFont(font);
                break;
            case CURRENCY_FORMAT, NUMBER_FORMAT:
                style.setAlignment(HorizontalAlignment.RIGHT);
                style.setDataFormat(workbook.createDataFormat().getFormat(
                    exportCurrency.equalsIgnoreCase("BHD") ? "0.000" : "0.00"));
                break;
            case TOTAL_CURRENCY_FORMAT:
                font.setFontHeightInPoints((short) 13);
                font.setBold(true);
                style.setFont(font);
                style.setAlignment(HorizontalAlignment.RIGHT);
                style.setDataFormat(workbook.createDataFormat().getFormat(
                    exportCurrency.equalsIgnoreCase("BHD") ? "0.000" : "0.00"));
                break;
            case DATE_FORMAT:
                style.setDataFormat(workbook.createDataFormat().getFormat("dd-MMM-yyyy"));
                break;
            case BOLD_FORMAT:
                font.setBold(true);
                style.setFont(font);
                break;
            default:
                break;
        }

        cell.setCellStyle(style);

        if (config.getCellFormula() != null && !config.getCellFormula().isEmpty()) {
            int rowNum = cell.getRowIndex() + 1;
            String formula = config.getCellFormula()
                .replace("#ROW_NUM#", String.valueOf(rowNum))
                .replace("#LAST_ROW_NUM#", String.valueOf(rowNum - 1));
            cell.setCellType(CellType.FORMULA);
            cell.setCellFormula(formula);
        }
    }

    private void formatDetailHeader(XSSFSheet sheet, int rowNum, int startCol, int endCol) {
        XSSFWorkbook workbook = sheet.getWorkbook();
        Row row = sheet.getRow(rowNum);

        for (int col = startCol; col <= endCol; col++) {
            Cell cell = row.getCell(col);
            if (cell == null) cell = row.createCell(col);

            Row nextRow = sheet.getRow(rowNum + 1);
            HorizontalAlignment alignment = HorizontalAlignment.LEFT;
            if (nextRow != null) {
                Cell nextCell = nextRow.getCell(col);
                if (nextCell != null) {
                    alignment = nextCell.getCellStyle().getAlignment();
                }
            }

            XSSFCellStyle style = workbook.createCellStyle();
            XSSFFont font = workbook.createFont();
            style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            style.setAlignment(alignment);
            font.setBold(true);
            style.setFont(font);
            cell.setCellStyle(style);
        }
    }

    private Row getOrCreateRow(XSSFSheet sheet, int rowNum) {
        Row row = sheet.getRow(rowNum);
        return row != null ? row : sheet.createRow(rowNum);
    }

    private Cell getOrCreateCell(Row row, int colNum, CellType cellType) {
        Cell cell = row.getCell(colNum);
        if (cell == null) {
            cell = row.createCell(colNum, cellType);
        }
        return cell;
    }

    private CellType getCellType(CellFormatType formatType) {
        return (formatType == CellFormatType.CURRENCY_FORMAT || 
                formatType == CellFormatType.TOTAL_CURRENCY_FORMAT || 
                formatType == CellFormatType.NUMBER_FORMAT) ? CellType.NUMERIC : CellType.STRING;
    }

    private String toTitleCase(String input) {
        StringBuilder result = new StringBuilder();
        boolean nextTitleCase = true;

        for (char c : input.toCharArray()) {
            if (Character.isSpaceChar(c)) {
                nextTitleCase = true;
            } else if (nextTitleCase) {
                c = Character.toTitleCase(c);
                nextTitleCase = false;
            }
            result.append(c);
        }
        return result.toString();
    }

    private void handleCompanyPoidParam(Map<String, Object> params, String paramKey) {
        Object value = params.get(paramKey);
        if (!(value instanceof List<?> list)) return;
        boolean contains999 = list.contains(999) || list.contains("999");
        if (list.size() == 1 || contains999) {
            params.put(paramKey, list.getFirst());
        } else {
            params.put(paramKey + "_CSV", list.stream().map(Object::toString).collect(Collectors.joining(",")));
            params.put(paramKey, list.getFirst());
        }
    }

    private static final DateTimeFormatter ORACLE_DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MMM-yyyy");

    private static final DateTimeFormatter ISO_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private String formatParamValue(Object value) {
        if (value instanceof LocalDate ld) return isPostgres() ? ld.format(ORACLE_DATE_FORMAT) : toOracleDate(ld);
        if (value instanceof LocalDateTime ldt) return isPostgres() ? ldt.toLocalDate().format(ORACLE_DATE_FORMAT) : toOracleDate(ldt.toLocalDate());
        if (value instanceof java.sql.Date sd) return isPostgres() ? sd.toLocalDate().format(ORACLE_DATE_FORMAT) : toOracleDate(sd.toLocalDate());
        if (value instanceof Date d) {
            LocalDate ld = new java.sql.Date(d.getTime()).toLocalDate();
            return isPostgres() ? ld.format(ORACLE_DATE_FORMAT) : toOracleDate(ld);
        }
        if (value instanceof String s) {
            try {
                LocalDate ld = LocalDate.parse(s, ISO_DATE_FORMAT);
                return isPostgres() ? ld.format(ORACLE_DATE_FORMAT) : toOracleDate(ld);
            } catch (Exception ignored) {}
        }
        return value.toString();
    }

    private String toOracleDate(LocalDate date) {
        return "TO_DATE('" + date.format(ORACLE_DATE_FORMAT) + "','DD-MON-YYYY')";
    }

    private String convertParametersToString(Map<String, Object> parameters) {
        if (parameters == null || parameters.isEmpty()) {
            return null;
        }
        Map<String, Object> modifiedParams = new HashMap<>(parameters);

        if (!isPostgres()) {
            handleCompanyPoidParam(modifiedParams, "COMPANY_POID");
            handleCompanyPoidParam(modifiedParams, "P_COMPANY_POID");
        }

        StringBuilder result = new StringBuilder();
        for (Map.Entry<String, Object> entry : modifiedParams.entrySet()) {
            if (!result.isEmpty()) {
                result.append(";");
            }
            result.append(entry.getKey()).append("=").append(formatParamValue(entry.getValue()));
        }
        return result.toString();
    }
}
