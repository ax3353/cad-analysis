package com.jinghu.cad.analysis.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kabeja.dxf.*;
import org.kabeja.dxf.helpers.Point;
import org.kabeja.parser.Parser;
import org.kabeja.parser.ParserBuilder;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * 出地管道分析
 */
@Slf4j
public class OutboundPipelineAnalyzer {

    private static final Pattern THICKNESS_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)"); // 匹配整数或小数
//    private static final Pattern PIPE_PATTERN = Pattern.compile(
//            "(D21\\.3|D33\\.7|D42\\.4|D48\\.3|D60\\.3|D88\\.9|D76|DN80|D114|D159|dn63|dn90|dn110|dn160)x" +
//                    THICKNESS_PATTERN.pattern() + // 匹配厚度（如 4.0）
//                    "[-×]" + // 匹配分隔符（- 或 ×）
//                    "(\\d+(?:\\.\\d+)?)[mM]", // 匹配长度（如 23m）
//            Pattern.CASE_INSENSITIVE
//    );

    private static final Pattern PIPE_PATTERN = Pattern.compile(
            "(D(?:N?\\d+(?:\\.\\d+)?))" + // 匹配直径（如 D32, DN80, D21.3）
                    "[-x×]" + // 分隔符：-、x 或 ×
                    "(?:" + THICKNESS_PATTERN.pattern() + "[-×])?" + // 可选的厚度和分隔符
                    "(\\d+(?:\\.\\d+)?)[mM]", // 匹配长度（如 0.2m）
            Pattern.CASE_INSENSITIVE
    );

    private final List<PipeInfo> pipeInfos = new ArrayList<>();
    private final ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    private final List<String> errorLog = new ArrayList<>();

    public boolean executeAnalysis(String filePath) {
        try {
            if (filePath.startsWith("http://") || filePath.startsWith("https://")) {
                processRemoteZip(filePath);
            } else if (filePath.endsWith(".zip")) {
                processLocalZip(filePath);
            } else {
                processDXFFile(filePath);
            }
            return true;
        } catch (Exception e) {
            errorLog.add("文件解析异常: " + e.getMessage());
            return false;
        }
    }

    private void processRemoteZip(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        try (InputStream is = connection.getInputStream()) {
            Path tempDir = Files.createTempDirectory("cad_extractor");
            unzip(is, tempDir);
            processExtractedFiles(tempDir);
        }
    }

    private void processLocalZip(String zipPath) throws Exception {
        Path tempDir = Files.createTempDirectory("cad_extractor");
        try (ZipFile zipFile = new ZipFile(zipPath)) {
            unzip(zipFile, tempDir);
            processExtractedFiles(tempDir);
        }
    }

    private void unzip(InputStream is, Path outputDir) throws IOException {
        Path tempZip = Files.createTempFile("temp", ".zip");
        Files.copy(is, tempZip, StandardCopyOption.REPLACE_EXISTING);
        try (ZipFile zipFile = new ZipFile(tempZip.toFile())) {
            unzip(zipFile, outputDir);
        }
        Files.deleteIfExists(tempZip);
    }

    private void unzip(ZipFile zipFile, Path outputDir) throws IOException {
        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            Path entryPath = outputDir.resolve(entry.getName());
            if (entry.isDirectory()) {
                Files.createDirectories(entryPath);
            } else {
                Files.createDirectories(entryPath.getParent());
                try (InputStream is = zipFile.getInputStream(entry)) {
                    Files.copy(is, entryPath, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private void processExtractedFiles(Path tmpDir) throws IOException {
        Files.walk(tmpDir)
                .filter(p -> p.getFileName().toString().equals("Drawing0.dxf"))
                .forEach(p -> processDXFFile(p.toString()));

        // 删除临时文件
        if (Files.exists(tmpDir)) {
            try {
                Files.walk(tmpDir)
                        .map(Path::toFile)
                        .sorted((o1, o2) -> -o1.compareTo(o2))
                        .forEach(File::delete);
                log.info("删除临时目录: {}", tmpDir.getFileName());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void processDXFFile(String dxfPath) {
        try {
            Parser parser = ParserBuilder.createDefaultParser();
            try (FileInputStream fis = new FileInputStream(dxfPath)) {
                parser.parse(fis, "UTF-8");
                DXFDocument doc = parser.getDocument();
                processDXFDocument(doc);
            }
        } catch (Exception e) {
            errorLog.add("DXF处理失败: " + dxfPath + " - " + e.getMessage());
        }
    }

    private void processDXFDocument(DXFDocument doc) {
        Iterator layerIterator = doc.getDXFLayerIterator();
        while (layerIterator.hasNext()) {
            DXFLayer layer = (DXFLayer) layerIterator.next();
            processLayerEntities(layer);
        }
    }

    private void processLayerEntities(DXFLayer layer) {
        Iterator entityTypeIterator = layer.getDXFEntityTypeIterator();
        while (entityTypeIterator.hasNext()) {
            String entityType = (String) entityTypeIterator.next();
            if ("TEXT".equalsIgnoreCase(entityType) || "MTEXT".equalsIgnoreCase(entityType)) {
                List<DXFEntity> entities = layer.getDXFEntities(entityType);
                for (DXFEntity entity : entities) {
                    if (entity instanceof DXFText) {
                        processTextEntity((DXFText) entity, layer.getName());
                    }
                    if (entity instanceof DXFMText) {
                        processTextEntity((DXFMText) entity, layer.getName());
                    }
                }
            }
        }
    }

    private void processTextEntity(DXFText text, String layerName) {
        try {
            String currentText = text.getText().trim().replaceAll("\\s+", "");
            Matcher matcher = PIPE_PATTERN.matcher(currentText);
            if (matcher.find()) {
                String spec = matcher.group(1).toUpperCase();
                String groupAlias = matcher.group(2);
                String alias = spec;
                if (StringUtils.hasText(groupAlias)) {
                    alias = spec + "*" + groupAlias;
                }
                BigDecimal length = new BigDecimal(matcher.group(3));

                Point insertPoint = text.getInsertPoint();
                String position = String.format("(%.2f,%.2f)", insertPoint.getX(), insertPoint.getY());

                pipeInfos.add(new PipeInfo(
                        text.getID(),
                        layerName,
                        "管道",
                        spec,
                        alias,
                        currentText,
                        position,
                        length,
                        "m"
                ));
            }
        } catch (Exception e) {
            errorLog.add("文本解析异常: " + text.getText() + " - " + e.getMessage());
        }
    }

    public String generateSummaryJson() throws JsonProcessingException {
        Map<String, List<PipeInfo>> grouped = pipeInfos.stream()
                .collect(Collectors.groupingBy(PipeInfo::getAlias));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, List<PipeInfo>> entry : grouped.entrySet()) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("alias", entry.getKey());
            map.put("spec", entry.getValue().get(0).getSpec());
            map.put("type", entry.getValue().get(0).getType());
            map.put("unit", entry.getValue().get(0).getUnit());
            map.put("data", entry.getValue().stream()
                    .map(PipeInfo::getData)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
            result.add(map);
        }

        return objectMapper.writeValueAsString(result);
    }

    public String getErrorLog() {
        return errorLog.isEmpty() ? "无错误记录" : String.join("\n", errorLog);
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PipeInfo {
        private String cadId;
        private String cadLayer;
        private String type;
        private String spec;
        private String alias;
        private String text;
        private String position;
        private BigDecimal data;
        private String unit;
    }

    public static void main(String[] args) throws Exception {
        boolean b = PIPE_PATTERN.matcher("D32-0.2m").find();
        System.out.println(b);
        // 支持 HTTP/ZIP/DXF
        OutboundPipelineAnalyzer extractor = new OutboundPipelineAnalyzer();
        String path = "D:\\1津都雅苑-出地管道.dxf";
//        String path = "D:\\cad_file2.zip";
//        String path = "http://ddns.limlim.cn:9000/ai/file/cad/cad_file.zip";

        if (extractor.executeAnalysis(path)) {
            log.info("汇总数据: " + extractor.generateSummaryJson());
        } else {
            log.info("分析失败");
        }

        log.info("错误日志: " + extractor.getErrorLog());
    }
}