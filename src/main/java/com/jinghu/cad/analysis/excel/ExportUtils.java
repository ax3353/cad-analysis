package com.jinghu.cad.analysis.excel;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ExportUtils {


    public static void exportToExcel(List<Map<Object, Object>> maps, String filePath) {
        if (maps == null || maps.isEmpty()) {
            System.out.println("数据为空，无法生成 Excel");
            return;
        }

        // 创建 Excel 工作簿
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Sheet1");

        // 获取表头
        Set<Object> headers = maps.get(0).keySet();
        int rowIndex = 0;

        // 创建表头行
        Row headerRow = sheet.createRow(rowIndex++);
        int colIndex = 0;
        for (Object header : headers) {
            Cell cell = headerRow.createCell(colIndex++);
            cell.setCellValue(header.toString());
            cell.setCellStyle(createHeaderStyle(workbook));
        }

        // 填充数据
        for (Map<Object, Object> map : maps) {
            Row row = sheet.createRow(rowIndex++);
            colIndex = 0;
            for (Object key : headers) {
                Cell cell = row.createCell(colIndex++);
                Object value = map.get(key);
                if (value != null) {
                    if (value instanceof Number) {
                        cell.setCellValue(((Number) value).doubleValue());
                    } else {
                        cell.setCellValue(value.toString());
                    }
                }
            }
        }

        // 自动调整列宽
        for (int i = 0; i < headers.size(); i++) {
            sheet.autoSizeColumn(i);
        }

        // 输出到文件
        try (FileOutputStream fileOut = new FileOutputStream(filePath)) {
            workbook.write(fileOut);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                workbook.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    private static CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }
}
