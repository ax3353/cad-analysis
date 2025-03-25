package com.jinghu.cad.analysis.utils;


import com.jinghu.cad.analysis.pojo.CadItem;
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
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 楼栋管道分析
 */
@Slf4j
public class BuildingPipeAnalyzer {

    // 正则表达式和关键字配置
    private final String regex = "(?<!\\S)([一二三四五六七八九十零百千万]+层|[1-9]\\d*F)(?!\\S)";
    private final Pattern layerPattern = Pattern.compile(regex);
    private final List<String> keywords = Arrays.asList("高位挂表", "低位挂表");

    /**
     * 计算D48.3管子的总长度
     */
    public CadItem executeAnalysis(String zipPath) {
        try {
            Path tempDir = Files.createTempDirectory("building_pipe");
            ZipFileUtils.unzip(zipPath, tempDir.toString());

            List<String> dxfFiles = Files.walk(tempDir).filter(p -> p.toString().endsWith(".dxf")
                            && p.getFileName().toString().startsWith("楼栋"))
                    .map(Path::toString).collect(Collectors.toList());
            if (dxfFiles.isEmpty()) {
                log.info("未找到DXF文件");
                return createResult(BigDecimal.ZERO);
            }

            double totalLength = dxfFiles.stream().mapToDouble(this::processDxfFile).sum();

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

            return createResult(BigDecimal.valueOf(totalLength));
        } catch (IOException e) {
            System.err.println("ZIP处理失败: " + e.getMessage());
            return createResult(BigDecimal.ZERO);
        }
    }

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

    private CadItem createResult(BigDecimal data) {
        CadItem item = new CadItem();
        item.setAlias("D48.3*4");
        item.setType("管道");
        item.setSpec("D48.3");
        item.setNominalSpec("DN40");
        item.setData(data);
        item.setUnit("m");
        return item;
    }

    public static void main(String[] args) {
        BuildingPipeAnalyzer analyzer = new BuildingPipeAnalyzer();
        CadItem result = analyzer.executeAnalysis("d:\\cad_file2.zip");
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