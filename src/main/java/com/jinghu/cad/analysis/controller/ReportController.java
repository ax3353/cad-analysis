package com.jinghu.cad.analysis.controller;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jinghu.cad.analysis.req.ReportRequest;
import com.jinghu.cad.analysis.utils.BuildingPipelineAnalyzer;
import com.jinghu.cad.analysis.utils.OutboundPipelineAnalyzer;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

@Slf4j
@RestController
@RequestMapping("/")
public class ReportController {

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${file.upload-path}")
    private String uploadPath;

    @PostMapping("/report")
    public String report(@RequestBody ReportRequest request) {
        log.info("开始设计CAD图纸, 文件地址: " + request.getCad_files_url());

        // 下载文件
        File cadZipFile = null;
        File supplementFile = null;
        try {
            // 1. 下载 CAD 文件
            cadZipFile = downloadToTempFileEnhance(request.getCad_files_url());
            if (cadZipFile == null) {
                return "CAD 文件下载失败";
            }

            String cadZipFileAbsPath = cadZipFile.getAbsolutePath();

            // 2. 解析出地管数据
            List<Map<String, Object>> pipeData = new ArrayList<>();
            OutboundPipelineAnalyzer extractor = new OutboundPipelineAnalyzer();
            if (extractor.executeAnalysis(cadZipFileAbsPath)) {
                pipeData = objectMapper.readValue(extractor.generateSummaryJson(), new TypeReference<List<Map<String, Object>>>() {
                });
            }

            // 3. 解析建筑管道数据
            BuildingPipelineAnalyzer analyzer = new BuildingPipelineAnalyzer();
            Map<String, String> buildingData = analyzer.calcTotalLength(cadZipFileAbsPath);
            List<Map<String, Object>> buildingPipeData = convertBuildingData(buildingData);

            // 4. 合并出地管和建筑管道数据
            List<Map<String, Object>> mergedData = mergeData(pipeData, buildingPipeData);

            // 5. 合并补充文件数据
            if (StringUtils.hasText(request.getSupplement_file_url())) {
                supplementFile = downloadToTempFileEnhance(request.getSupplement_file_url());
                List<Map<String, Object>> supplementData = excelToJson(supplementFile);
                mergedData = mergeData(supplementData, mergedData);
            }

            // 6. 返回结果
            log.info("完成CAD图纸识别");
            return JSON.toJSONString(mergedData);
        } catch (Exception e) {
            e.printStackTrace();
            return "处理失败: " + e.getMessage();
        } finally {
            // 清理临时文件
            deleteTempFile(cadZipFile);
            deleteTempFile(supplementFile);
            log.info("删除CAD临时文件");
        }
    }

    private File downloadToTempFile(String url) throws IOException {
        if (!StringUtils.hasText(url)) {
            return null;
        }

        URL fileUrl = new URL(url);
        String suffix = url.contains(".zip") ? ".zip" : ".xlsx";
        File tempFile = File.createTempFile("cad_", suffix);

        try (InputStream in = fileUrl.openStream();
             FileOutputStream out = new FileOutputStream(tempFile)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }
        return tempFile;
    }

    private File downloadToTempFileEnhance(String url) throws IOException {
        if (!StringUtils.hasText(url)) {
            return null;
        }

        String fileName = url.substring(url.lastIndexOf("/"));
        File dest = Paths.get(uploadPath, "files", fileName).toFile();
        if (dest.exists()) {
            return dest;
        } else {
            return downloadToTempFile(url);
        }
    }

    private List<Map<String, Object>> convertBuildingData(Map<String, String> data) {
        Map<String, Object> item = new HashMap<>();
        item.put("type", data.get("type"));
        item.put("spec", data.get("spec"));
        item.put("alias", data.get("alias"));
        item.put("data", new BigDecimal(data.get("data")));
        item.put("unit", data.get("unit"));
        return Collections.singletonList(item);
    }

    private List<Map<String, Object>> excelToJson(File excelFile) throws IOException {
        Workbook workbook = WorkbookFactory.create(excelFile);
        Sheet sheet = workbook.getSheetAt(0);
        List<Map<String, Object>> result = new ArrayList<>();

        for (Row row : sheet) {
            if (row.getRowNum() == 0) {
                continue;
            }

            Map<String, Object> item = new HashMap<>();
            item.put("type", getCellStringValue(row.getCell(0)));
            item.put("spec", getCellStringValue(row.getCell(1)));
            item.put("alias", getCellStringValue(row.getCell(2)));
            item.put("data", convertCellToBigDecimal(row.getCell(3)));
            item.put("unit", getCellStringValue(row.getCell(4)));
            result.add(item);
        }
        return result;
    }

    private String getCellStringValue(Cell cell) {
        if (cell == null) {
            return "";
        }
        cell.setCellType(CellType.STRING);
        return cell.getStringCellValue().trim();
    }

    private BigDecimal convertCellToBigDecimal(Cell cell) {
        if (cell == null) {
            return BigDecimal.ZERO;
        }
        if (cell.getCellType() == CellType.NUMERIC) {
            return BigDecimal.valueOf(cell.getNumericCellValue());
        }
        try {
            return new BigDecimal(cell.getStringCellValue().trim());
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private List<Map<String, Object>> mergeData(List<Map<String, Object>> dataA,
                                                List<Map<String, Object>> dataB) {
        Map<String, Map<String, Object>> merged = new HashMap<>();

        Stream.concat(dataA.stream(), dataB.stream()).forEach(item -> {
            String key = item.get("type") + "|" + item.get("spec");
            Map<String, Object> existing = merged.get(key);
            BigDecimal currentValue = convertToBigDecimal(item.get("data"));

            if (existing != null) {
                BigDecimal sum = ((BigDecimal) existing.get("data")).add(currentValue);
                existing.put("data", sum);
            } else {
                Map<String, Object> newItem = new HashMap<>(item);
                newItem.put("data", currentValue);
                merged.put(key, newItem);
            }
        });

        return new ArrayList<>(merged.values());
    }

    private BigDecimal convertToBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        } else if (value instanceof Number) {
            return new BigDecimal(value.toString());
        } else if (value instanceof String) {
            return new BigDecimal((String) value);
        }
        throw new IllegalArgumentException("无法转换类型: " + value.getClass());
    }

    private void deleteTempFile(File file) {
        if (file != null && file.exists()) {
            try {
                Files.delete(file.toPath());
            } catch (IOException e) {
                System.err.println("删除临时文件失败: " + file.getAbsolutePath());
            }
        }
    }
}