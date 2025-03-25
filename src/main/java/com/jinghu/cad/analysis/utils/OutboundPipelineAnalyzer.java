package com.jinghu.cad.analysis.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kabeja.dxf.*;
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

/**
 * 出地管和法兰分析
 */
@Slf4j
public class OutboundPipelineAnalyzer {

    private static final Pattern THICKNESS_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)"); // 匹配整数或小数
    private static final Pattern PIPE_PATTERN = Pattern.compile(
            "(D(?:N?\\d+(?:\\.\\d+)?))" + // 匹配直径（如 D32, DN80, D21.3）
                    "[-x×]" + // 分隔符：-、x 或 ×
                    "(?:" + THICKNESS_PATTERN.pattern() + "[-×])?" + // 可选的厚度和分隔符
                    "(\\d+(?:\\.\\d+)?)[mM]", // 匹配长度（如 0.2m）
            Pattern.CASE_INSENSITIVE
    );

    private static final String REGEX =
            "(法兰球阀|金属软管|法兰盖)" + // 目标配件类型
                    "(DN\\d+(?:\\.\\d+)?)" + // 配件规格
                    "(?!.*\\d+\\s*[m米])"; // 排除含长度单位的情况
    private static final Pattern FLANGE_PATTERN = Pattern.compile(REGEX, Pattern.CASE_INSENSITIVE);

    private final List<TagData> pipeInfos = new ArrayList<>();
    private final List<TagData> flangeInfos = new ArrayList<>();
    private final ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

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
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 远程文件，如：http://www.qq.com/test.zip
     */
    private void processRemoteZip(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        try (InputStream is = connection.getInputStream()) {
            Path tempDir = Files.createTempDirectory("cad_outbound_pipeline");
            Path tempZip = Files.createTempFile("temp", ".zip");
            Files.copy(is, tempZip, StandardCopyOption.REPLACE_EXISTING);
            ZipFileUtils.unzip(tempZip.toString(), tempDir.toString());
            Files.deleteIfExists(tempZip);
            processDXFFiles(tempDir);
        }
    }

    /**
     * 本地文件，如：D:\test.zip
     */
    private void processLocalZip(String zipPath) throws Exception {
        Path tempDir = Files.createTempDirectory("cad_outbound_pipeline");
        ZipFileUtils.unzip(zipPath, tempDir.toString());
        processDXFFiles(tempDir);
    }

    /**
     * 本地DXF文件，如：D:\test.dxf
     */
    private void processDXFFile(String dxfPath) {
        try {
            Parser parser = ParserBuilder.createDefaultParser();
            try (FileInputStream fis = new FileInputStream(dxfPath)) {
                parser.parse(fis, "UTF-8");
                DXFDocument doc = parser.getDocument();
                processDXFDocument(doc);
            }
        } catch (Exception e) {
            log.error("DXF处理失败: " + dxfPath + " - " + e.getMessage());
        }
    }

    private void processDXFFiles(Path tempDir) throws IOException {
        Files.walk(tempDir).filter(p -> p.toString().endsWith(".dxf")
                        && p.getFileName().toString().startsWith("出地管"))
                .forEach(p -> processDXFFile(p.toString()));

        // 删除临时文件
        if (Files.exists(tempDir)) {
            try {
                Files.walk(tempDir)
                        // 逆序遍历，先删文件再删目录
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
                log.info("成功删除临时目录: {}", tempDir);
            } catch (IOException e) {
                log.warn("删除临时目录: {} 失败", tempDir, e);
            }
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
            Matcher pipeMatcher = PIPE_PATTERN.matcher(currentText);
            Matcher flangeMatcher = FLANGE_PATTERN.matcher(currentText);

            if (pipeMatcher.find()) {
                String spec = pipeMatcher.group(1).toUpperCase();
                String groupAlias = pipeMatcher.group(2);
                String alias = spec;
                if (StringUtils.hasText(groupAlias)) {
                    alias = spec + "*" + groupAlias;
                }
                BigDecimal length = new BigDecimal(pipeMatcher.group(3));
                pipeInfos.add(new TagData(
                        text.getID(),
                        layerName,
                        "管道",
                        spec,
                        alias,
                        currentText,
                        length,
                        "m"));
            } else if (flangeMatcher.find()) {
                flangeInfos.add(new TagData(
                        text.getID(),
                        layerName,
                        "法兰",
                        flangeMatcher.group(1),
                        flangeMatcher.group(2).toUpperCase(),
                        currentText,
                        new BigDecimal(1),
                        "个"
                ));
            }
        } catch (Exception e) {
            log.error("文本解析异常: " + text.getText() + " - " + e.getMessage());
        }
    }

    public String generateSummaryJson() throws JsonProcessingException {
        List<TagData> dataList = new ArrayList<>();
        dataList.addAll(pipeInfos);
        dataList.addAll(flangeInfos);

        // 按 alias 和 spec 共同分组
        Map<String, List<TagData>> grouped = dataList.stream()
                .collect(Collectors.groupingBy(tag -> tag.getAlias() + "|" + tag.getSpec()));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, List<TagData>> entry : grouped.entrySet()) {
            List<TagData> items = entry.getValue();
            String[] keys = entry.getKey().split("\\|");

            Map<String, Object> map = new LinkedHashMap<>();
            map.put("alias", keys[0]); // alias
            map.put("spec", keys[1]);  // spec
            map.put("type", items.get(0).getType());
            map.put("unit", items.get(0).getUnit());
            map.put("data", items.stream()
                    .map(TagData::getData)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
            result.add(map);
        }
        return objectMapper.writeValueAsString(result);
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TagData {
        private String cadId;
        private String cadLayer;
        private String type;
        private String spec;
        private String alias;
        private String text;
        private BigDecimal data;
        private String unit;
    }

    public static void main(String[] args) throws Exception {
        // 支持 HTTP/ZIP/DXF
        OutboundPipelineAnalyzer extractor = new OutboundPipelineAnalyzer();
//        String path = "D:\\1津都雅苑-出地管道.dxf";
//        String path = "D:\\cad_file2.zip";
        String path = "http://ddns.limlim.cn:9000/ai/file/cad/cad_file2.zip";

        if (extractor.executeAnalysis(path)) {
            log.info("汇总数据: " + extractor.generateSummaryJson());
        } else {
            log.info("分析失败");
        }
    }
}