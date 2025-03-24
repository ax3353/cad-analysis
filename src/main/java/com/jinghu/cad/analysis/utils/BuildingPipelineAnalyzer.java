package com.jinghu.cad.analysis.utils;


import lombok.extern.slf4j.Slf4j;
import org.kabeja.dxf.DXFDocument;
import org.kabeja.dxf.DXFEntity;
import org.kabeja.dxf.DXFLayer;
import org.kabeja.dxf.DXFText;
import org.kabeja.parser.Parser;
import org.kabeja.parser.ParserBuilder;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * 楼栋管道分析
 */
@Slf4j
public class BuildingPipelineAnalyzer {

    // 正则表达式和关键字配置
    private final String regex = "(?<![^\\p{P}\\s])([一二三四五六七八九十零百千万]+层|[1-9]\\d*F)(?![^\\p{P}\\s])";
    private final Pattern layerPattern = Pattern.compile(regex);
    private final List<String> keywords = Arrays.asList("高位挂表", "低位挂表");

    // 解析 DXF 文件
    private Map.Entry<Integer, Map<String, Integer>> analyzeDxfTexts(String dxfPath) {
        Map<String, Integer> textCounts = new HashMap<>();
        keywords.forEach(k -> textCounts.put(k, 0));
        int layerCount = 0;

        try {
            Parser parser = ParserBuilder.createDefaultParser();
            try (FileInputStream fis = new FileInputStream(dxfPath)) {
                parser.parse(fis, "UTF-8");
                DXFDocument doc = parser.getDocument();

                // 遍历所有图层
                Iterator<DXFLayer> layerIter = doc.getDXFLayerIterator();
                while (layerIter.hasNext()) {
                    DXFLayer layer = layerIter.next();

                    Iterator entityTypeIterator = layer.getDXFEntityTypeIterator();
                    while (entityTypeIterator.hasNext()) {
                        String entityType = (String) entityTypeIterator.next();
                        if ("TEXT".equalsIgnoreCase(entityType)) {
                            List<DXFEntity> entities = layer.getDXFEntities(entityType);
                            for (DXFEntity entity : entities) {
                                if (entity instanceof DXFText) {
                                    DXFText text = (DXFText) entity;
                                    String content = text.getText().trim().replaceAll("\\s+", "");

                                    // 匹配层数
                                    if (layerPattern.matcher(content).matches()) {
                                        layerCount++;
                                    }

                                    // 统计关键字
                                    keywords.forEach(k -> {
                                        if (content.contains(k)) {
                                            textCounts.put(k, textCounts.get(k) + 1);
                                        }
                                    });
                                }
                            }
                        }
                    }

                }
            }
        } catch (Exception e) {
            System.err.println("DXF解析失败: " + e.getMessage());
        }

        return new AbstractMap.SimpleEntry<>(layerCount > 0 ? layerCount : null, textCounts);
    }

    // 处理单个 DXF 文件（保留原有逻辑）
    private double processDxfFile(String dxfFile) {
        Map.Entry<Integer, Map<String, Integer>> result = analyzeDxfTexts(dxfFile);
        Integer totalHouseholds = result.getKey();
        Map<String, Integer> textCounts = result.getValue();

        if (totalHouseholds == null) {
            log.info("文件 {} 未找到楼层信息", dxfFile);
            return 0;
        }

        log.info("【文件】: {}", dxfFile);
        log.info("【总户数】: {}", totalHouseholds);

        int low = textCounts.getOrDefault("低位挂表", 0);
        int high = textCounts.getOrDefault("高位挂表", 0);
        log.info("原始低位挂表: {}, 原始高位挂表: {}", low, high);

        String category = determineCategory(low, high);
        Map.Entry<Integer, Integer> clocks = calculateClocks(category, totalHouseholds, low, high);

        double highClockMeter = 3.0d;
        double lowClockMeter = 3.0d;
        double totalLength = (clocks.getValue() * lowClockMeter) + (clocks.getKey() * highClockMeter);
        log.info("【总长度】: {}米\n", totalLength);
        return totalLength;
    }

    private String determineCategory(int low, int high) {
        if (low == 0 || high == 0) {
            return "A";
        } else if (low == high) {
            return "B";
        } else {
            return "C";
        }
    }

    private Map.Entry<Integer, Integer> calculateClocks(String category, int total, int low, int high) {
        switch (category) {
            case "A":
                if (low == 0) {
                    log.info("户数转高位挂表={}, 户数转低位挂表=0", total);
                    return new AbstractMap.SimpleEntry<>(total, 0);
                } else {
                    log.info("户数转低位挂表={}, 户数转高位挂表=0", total);
                    return new AbstractMap.SimpleEntry<>(0, total);
                }
            case "B":
                int half = total / 2;
                log.info("户数转低位挂表={}, 户数转高位挂表={}", half, half);
                return new AbstractMap.SimpleEntry<>(half, half);
            default:
                log.info("户型分散，无法分配");
                return new AbstractMap.SimpleEntry<>(0, 0);
        }
    }

    // 处理 ZIP 文件（优化解压逻辑）
    public Map<String, String> calcTotalLength(String zipPath) {
        try {
            Path tempDir = Files.createTempDirectory("dxf_analyzer");
            unzip(zipPath, tempDir.toString());

            List<String> dxfFiles = findDxfFiles(tempDir);
            if (dxfFiles.isEmpty()) {
                log.info("未找到DXF文件");
                return createResult("0");
            }

            double totalLength = dxfFiles.stream().mapToDouble(this::processDxfFile).sum();

            // 删除临时文件
            if (Files.exists(tempDir)) {
                try {
                    Files.walk(tempDir)
                            .map(Path::toFile)
                            .sorted((o1, o2) -> -o1.compareTo(o2))
                            .forEach(File::delete);
                    log.info("删除临时目录: {}", tempDir.getFileName());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            return createResult(String.valueOf(totalLength));
        } catch (IOException e) {
            System.err.println("ZIP处理失败: " + e.getMessage());
            return createResult("0");
        }
    }

    // 解压 ZIP（优化临时文件清理）
    private void unzip(String zipPath, String outputDir) throws IOException {
        try (ZipFile zipFile = new ZipFile(zipPath)) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                Path entryPath = Paths.get(outputDir, entry.getName());
                Files.createDirectories(entryPath.getParent());
                if (!entry.isDirectory()) {
                    try (InputStream is = zipFile.getInputStream(entry)) {
                        Files.copy(is, entryPath, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
        }
    }

    // 查找 DXF 文件（保留过滤条件）
    private List<String> findDxfFiles(Path dir) throws IOException {
        try (Stream<Path> paths = Files.walk(dir)) {
            return paths.filter(p -> p.toString().endsWith(".dxf") &&
                            !p.getFileName().toString().startsWith("Drawing0"))
                    .map(Path::toString)
                    .collect(Collectors.toList());
        }
    }

    private Map<String, String> createResult(String data) {
        Map<String, String> result = new HashMap<>();
        result.put("type", "管道");
        result.put("spec", "D48.3");
        result.put("alias", "D48.3*4");
        result.put("data", data);
        result.put("unit", "m");
        return result;
    }

    public static void main(String[] args) {
//        BuildingPipelineAnalyzer analyzer = new BuildingPipelineAnalyzer();
//        Map<String, String> result = analyzer.calcTotalLength("d:\\cad_file2.zip");
//        System.out.println(result);

        String text = "盘管仅二层有";
        String regex = "(?<![^\\p{P}\\s])([一二三四五六七八九十零百千万]+层|[1-9]\\d*F)(?![^\\p{P}\\s])";
        Matcher matcher = Pattern.compile(regex).matcher(text);
        while (matcher.find()) {
            System.out.println("匹配到: " + matcher.group());
        }
    }
}