package com.jinghu.cad.analysis.controller;

import com.alibaba.fastjson.JSON;
import com.jinghu.cad.analysis.pojo.CadItem;
import com.jinghu.cad.analysis.req.ReportRequest;
import com.jinghu.cad.analysis.utils.BuildingPipeAnalyzer;
import com.jinghu.cad.analysis.utils.OutboundPipeAnalyzer;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
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

    @Value("${file.upload-path}")
    private String uploadPath;

    @PostMapping("/report")
    public String report(@RequestBody ReportRequest request) {
        String cadFilesUrl = request.getCad_files_url();
        String supplementFileUrl = request.getSupplement_file_url();
        String confirmFileUrl = request.getConfirm_file_url();

        log.info("开始分析CAD图纸, 文件地址: " + cadFilesUrl);

        // 下载文件
        File cadZipFile = null;
        File supplementFile = null;
        File confirmFile = null;
        try {
            // 1. 下载 CAD 文件
            cadZipFile = downloadToTempFileEnhance(cadFilesUrl);
            if (cadZipFile == null) {
                return "CAD 文件下载失败";
            }

            String cadZipFileAbsPath = cadZipFile.getAbsolutePath();

            // 2. 解析出地管数据
            OutboundPipeAnalyzer outboundPipeAnalyzer = new OutboundPipeAnalyzer();
            List<CadItem> outboundPipeData = outboundPipeAnalyzer.executeAnalysis(cadZipFileAbsPath);

            // 3. 解析建筑管道数据
            BuildingPipeAnalyzer buildingPipeAnalyzer = new BuildingPipeAnalyzer();
            CadItem buildingData = buildingPipeAnalyzer.executeAnalysis(cadZipFileAbsPath);
            List<CadItem> buildingPipeData = Collections.singletonList(buildingData);

            // 4. 合并出地管和建筑管道数据
            List<CadItem> mergedData = mergeData(outboundPipeData, buildingPipeData);

            // 5. 合并补充文件数据
            if (StringUtils.hasText(supplementFileUrl)) {
                supplementFile = downloadToTempFileEnhance(supplementFileUrl);
                List<CadItem> supplementData = excelToCadItems(supplementFile);
                mergedData = mergeData(supplementData, mergedData);
            }

            // 6. 合并工程量确认单
            if (StringUtils.hasText(confirmFileUrl)) {
                confirmFile = downloadToTempFileEnhance(confirmFileUrl);
                // todo
            }

            // 7. 返回结果
            String jsonString = JSON.toJSONString(mergedData);
            log.info("完成CAD图纸识别，返回结果: {}", jsonString);
            return jsonString;
        } catch (Exception e) {
            return "处理失败: " + e.getMessage();
        } finally {
            // 清理临时文件
            deleteTempFile(cadZipFile);
            deleteTempFile(supplementFile);
            deleteTempFile(confirmFile);
            log.info("删除CAD临时文件");
        }
    }

    private File downloadToTempFileEnhance(String url) {
        if (!StringUtils.hasText(url)) {
            return null;
        }

        String fileName = url.substring(url.lastIndexOf("/"));
        File dest;
        try {
            dest = Paths.get(uploadPath, "files", fileName).toFile();
            if (dest.exists()) {
                return dest;
            } else {
                return downloadToTempFile(url);
            }
        } catch (Exception e) {
            log.error("下载文件失败", e);
            return null;
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

    private List<CadItem> excelToCadItems(File excelFile) throws IOException {
        if (excelFile == null) {
            return new ArrayList<>();
        }

        Workbook workbook = WorkbookFactory.create(excelFile);
        Sheet sheet = workbook.getSheetAt(0);
        List<CadItem> result = new ArrayList<>();

        for (Row row : sheet) {
            if (row.getRowNum() == 0) {
                continue;
            }

            CadItem item = new CadItem();
            item.setType(getCellStringValue(row.getCell(0)));
            item.setSpec(getCellStringValue(row.getCell(1)));
            item.setAlias(getCellStringValue(row.getCell(2)));
            item.setData(convertCellToBigDecimal(row.getCell(3)));
            item.setUnit(getCellStringValue(row.getCell(4)));

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

    public List<CadItem> mergeData(List<CadItem> dataA, List<CadItem> dataB) {
        // 用于存储合并后的数据，key 是 alias + "|" + spec
        Map<String, CadItem> merged = new HashMap<>();

        // 将 dataA 和 dataB 合并处理
        Stream.concat(dataA.stream(), dataB.stream()).forEach(item -> {
            String key = item.getAlias() + "|" + item.getSpec();
            CadItem existing = merged.get(key);

            if (existing != null) {
                // 如果已存在相同 key 的 CadItem，累加 data 值
                BigDecimal sum = existing.getData().add(item.getData());
                existing.setData(sum);
            } else {
                // 如果不存在，直接添加到 merged 中
                CadItem newItem = new CadItem();
                newItem.setAlias(item.getAlias());
                newItem.setUnit(item.getUnit());
                newItem.setType(item.getType());
                newItem.setData(item.getData());
                newItem.setSpec(item.getSpec());
                merged.put(key, newItem);
            }
        });

        // 将 Map 转换为 List<CadItem> 返回
        return new ArrayList<>(merged.values());
    }

    private void deleteTempFile(File file) {
        if (file != null && file.exists()) {
            try {
                log.info("删除临时文件: {}", file.toPath());
                Files.delete(file.toPath());
            } catch (IOException e) {
                log.error("删除临时文件失败: " + file.getAbsolutePath());
            }
        }
    }
}