package com.jinghu.cad.analysis.analyzer;


import com.alibaba.fastjson.JSON;
import com.jinghu.cad.analysis.enmus.PipeDiameter;
import com.jinghu.cad.analysis.enmus.TypeEnums;
import com.jinghu.cad.analysis.dto.CadItem;
import com.jinghu.cad.analysis.utils.FileUtils;
import lombok.extern.slf4j.Slf4j;
import org.kabeja.dxf.*;
import org.kabeja.parser.Parser;
import org.kabeja.parser.ParserBuilder;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.math.BigDecimal;
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
                    "(DN?\\d+(?:\\.\\d+)?)" +         // 组1: 直径（如 D48.3）
                    "[-x×]" +                             // 分隔符（如 x）
                    "(?:" + THICKNESS_PATTERN.pattern() + "[-x×])?" + // 组2（可选）: 厚度（如 4）
                    "(\\d+(?:\\.\\d+)?)" +                // 组3: 长度（如 1.2）
                    "[mM]" +                              // 后缀 m/M
                    "$",                                  // 锚定字符串结束
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern PAI_SHAPED_BEND_PATTERN = Pattern.compile(
            "^π型弯",                                  // 锚定字符串结束
            Pattern.CASE_INSENSITIVE
    );

    private final List<CadItem> pipeInfos = new ArrayList<>();
    private int floorCounts = 0;
    private int paiCounts = 0;

    public List<CadItem> executeAnalysis(String filePath) {
        try {
            if (filePath.startsWith("http://") || filePath.startsWith("https://")) {
                processRemoteFile(filePath);
            } else {
                processLocalFile(new File(filePath));
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
    private void processRemoteFile(String urlString) throws Exception {
        File file = FileUtils.downloadToTempFileEnhance(urlString);
        assert file != null;
        processLocalFile(file);
    }

    /**
     * 本地文件，如：D:\test.zip
     */
    private void processLocalFile(File file) throws Exception {
        processDXFFile(file);
    }

    /**
     * 本地DXF文件，如：D:\test.dxf
     */
    private void processDXFFile(File file) {
        try {
            Parser parser = ParserBuilder.createDefaultParser();
            try (FileInputStream fis = new FileInputStream(file)) {
                parser.parse(fis, "UTF-8");
                DXFDocument doc = parser.getDocument();
                processDXFDocument(doc);
            }
        } catch (Exception e) {
            log.error("DXF处理失败: {} - {}", file, e.getMessage());
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
            Matcher paishapedBendMatcher = PAI_SHAPED_BEND_PATTERN.matcher(currentText);
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
                item.setName("楼");
                item.setAlias(alias);
                item.setUnit("m");
                item.setType(TypeEnums.PIPE.getType());
                item.setData(length);
                item.setSpec(spec);
                item.setNominalSpec(PipeDiameter.getPipeDiameter(spec).getNominalDiameterAlias());
                pipeInfos.add(item);
            } else if (paishapedBendMatcher.find()) {
                paiCounts++;
            } else if (floorMatcher.find()) {
                floorCounts++;
            }
        } catch (Exception e) {
            log.error("文本解析异常: " + text.getText() + " - " + e.getMessage());
        }
    }

    private List<CadItem> generateSummary() {
        // 按公制分组统计
        Map<String, Map<String, List<CadItem>>> groupedData = pipeInfos.stream()
                .collect(Collectors.groupingBy(
                        CadItem::getType,
                        Collectors.groupingBy(CadItem::getNominalSpec)
                ));
        List<CadItem> result = new ArrayList<>();
        for (Map.Entry<String, Map<String, List<CadItem>>> entry : groupedData.entrySet()) {
            String key = entry.getKey();
            Map<String, List<CadItem>> grouped1 = entry.getValue();
            for (Map.Entry<String, List<CadItem>> entry1 : grouped1.entrySet()) {
                String key1 = entry1.getKey();
                List<CadItem> items1 = entry1.getValue();
                CadItem item = new CadItem();
                item.setName(key);
                item.setAlias(items1.stream().map(CadItem::getAlias).distinct().collect(Collectors.joining(",")));
                item.setSpec(items1.stream().map(CadItem::getSpec).distinct().collect(Collectors.joining(",")));
                item.setNominalSpec(key1);
                item.setType(items1.get(0).getType());
                item.setUnit(items1.get(0).getUnit());
                item.setData(items1.stream()
                        .map(CadItem::getData)
                        .reduce(BigDecimal.ZERO, BigDecimal::add));
                result.add(item);
            }
        }

        CadItem floorItem = new CadItem();
        floorItem.setName("户数");
        floorItem.setAlias("f、F、层");
        floorItem.setUnit("户");
        floorItem.setType(TypeEnums.FLOOR.getType());
        floorItem.setData(BigDecimal.valueOf(floorCounts));
        floorItem.setSpec("f、F、层");
        floorItem.setNominalSpec("");
        result.add(floorItem);

        CadItem paiItem = new CadItem();
        paiItem.setName("π型弯");
        paiItem.setAlias("-");
        paiItem.setUnit("个");
        paiItem.setType(TypeEnums.PAI_SHAPED_BEND.getType());
        paiItem.setData(BigDecimal.valueOf(paiCounts));
        paiItem.setSpec("-");
        paiItem.setNominalSpec("-");
        result.add(paiItem);

        return result;
    }

    public static void main(String[] args) {
        BuildingPipeAnalyzer analyzer = new BuildingPipeAnalyzer();
        List<CadItem> result = analyzer.executeAnalysis("C:\\Users\\Liming\\Desktop\\cad_file2.zip");
        System.out.println(JSON.toJSONString(result));

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