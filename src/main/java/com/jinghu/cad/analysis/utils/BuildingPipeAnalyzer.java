package com.jinghu.cad.analysis.utils;


import com.jinghu.cad.analysis.pojo.CadItem;
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
 * 楼栋管道分析
 */
@Slf4j
public class BuildingPipeAnalyzer {

    // 层正则
    private static final Pattern FLOOR_PATTERN = Pattern.compile(
            "(?<!\\S)([一二三四五六七八九十零百千万]+层|[1-9]\\d*F)(?!\\S)",
            Pattern.CASE_INSENSITIVE);

    // 管子正则
    private static final Pattern THICKNESS_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)"); // 匹配整数或小数
    private static final Pattern PIPE_PATTERN = Pattern.compile(
            "^" +                                  // 锚定字符串开始
                    "(D(?:N?\\d+(?:\\.\\d+)?))" +         // 组1: 直径（如 D48.3）
                    "[-x×]" +                             // 分隔符（如 x）
                    "(?:" + THICKNESS_PATTERN.pattern() + "[-x×])?" + // 组2（可选）: 厚度（如 4）
                    "(\\d+(?:\\.\\d+)?)" +                // 组3: 长度（如 1.2）
                    "[mM]" +                              // 后缀 m/M
                    "$",                                  // 锚定字符串结束
            Pattern.CASE_INSENSITIVE
    );

    private final List<CadItem> pipeInfos = new ArrayList<>();
    private int floorCounts = 0;

    public List<CadItem> executeAnalysis(String filePath) {
        try {
            if (filePath.startsWith("http://") || filePath.startsWith("https://")) {
                processRemoteZip(filePath);
            } else if (filePath.endsWith(".zip")) {
                processLocalZip(filePath);
            } else {
                processDXFFile(filePath);
            }

            return this.generateSummary();
        } catch (Exception e) {
            log.error("出地管文件分析失败", e);
            return Collections.emptyList();
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
            Path tempDir = Files.createTempDirectory("cad_outbound_pipe");
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
                        processTextEntity((DXFText) entity);
                    }
                    if (entity instanceof DXFMText) {
                        processTextEntity((DXFMText) entity);
                    }
                }
            }
        }
    }

    private void processTextEntity(DXFText text) {
        try {
            String currentText = text.getText().trim().replaceAll("\\s+", "");
            Matcher pipeMatcher = PIPE_PATTERN.matcher(currentText);
            Matcher floorMatcher = FLOOR_PATTERN.matcher(currentText);

            if (pipeMatcher.find()) {
                String spec = pipeMatcher.group(1).toUpperCase();
                String groupAlias = pipeMatcher.group(2);
                String alias = spec;
                if (StringUtils.hasText(groupAlias)) {
                    alias = spec + "*" + groupAlias;
                }
                BigDecimal length = new BigDecimal(pipeMatcher.group(3));

                CadItem item = new CadItem();
                item.setAlias(alias);
                item.setUnit("m");
                item.setType("管道");
                item.setData(length);
                item.setSpec(spec);
                item.setNominalSpec(spec);
                pipeInfos.add(item);
            } else if (floorMatcher.find()) {
                floorCounts++;
            }
        } catch (Exception e) {
            log.error("文本解析异常: " + text.getText() + " - " + e.getMessage());
        }
    }

    private List<CadItem> generateSummary() {
        // 按 alias 和 spec 共同分组
        Map<String, List<CadItem>> grouped = pipeInfos.stream()
                .collect(Collectors.groupingBy(tag -> tag.getAlias() + "|" + tag.getSpec()));

        List<CadItem> result = new ArrayList<>();
        for (Map.Entry<String, List<CadItem>> entry : grouped.entrySet()) {
            List<CadItem> items = entry.getValue();
            String[] keys = entry.getKey().split("\\|");

            CadItem item = new CadItem();
            item.setAlias(keys[0]);
            item.setSpec(keys[1]);
            item.setNominalSpec(keys[1]);
            item.setType(items.get(0).getType());
            item.setUnit(items.get(0).getUnit());
            item.setData(items.stream()
                    .map(CadItem::getData)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
            result.add(item);
        }

        CadItem floorItem = new CadItem();
        floorItem.setAlias("层数");
        floorItem.setUnit("层");
        floorItem.setType("楼层数");
        floorItem.setData(BigDecimal.valueOf(floorCounts));
        floorItem.setSpec("层");
        floorItem.setNominalSpec("层");
        result.add(floorItem);

        return result;
    }

    public static void main(String[] args) {
        BuildingPipeAnalyzer analyzer = new BuildingPipeAnalyzer();
        List<CadItem> result = analyzer.executeAnalysis("d:\\cad_file2.zip");
        System.out.println(result);

//        String text = " 二十层 "
//                + "100F "
//                + "、二十层"
//                + "三层楼"
//                + "零0层"
//                + "01F"
//                + "100F。"
//                + "这是一层"
//                + "独立词100F";
//        String regex = "(?<!\\S)([一二三四五六七八九十零百千万]+层|[1-9]\\d*F)(?!\\S)";
//        Matcher matcher = Pattern.compile(regex).matcher(text);
//        while (matcher.find()) {
//            System.out.println("匹配到: " + matcher.group());
//        }
    }
}