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
 * 出地管和法兰分析
 */
@Slf4j
public class OutboundPipeAnalyzer {

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

    private static final Pattern FLANGE_BALL_VALVE_PATTERN = Pattern.compile(
            "(法兰球阀)" +                      // 目标配件类型
                    "(DN\\d+(?:\\.\\d+)?)" +        // 配件规格
                    "(?!.*\\d+\\s*[m米])",          // 排除含长度单位的情况
            Pattern.CASE_INSENSITIVE);
    private static final Pattern METAL_BALL_VALVE_PATTERN = Pattern.compile(
            "(金属球阀)" +                      // 目标配件类型
                    "(DN\\d+(?:\\.\\d+)?)" +        // 配件规格
                    "(?!.*\\d+\\s*[m米])",          // 排除含长度单位的情况
            Pattern.CASE_INSENSITIVE);
    private static final Pattern METAL_HOSE_PATTERN = Pattern.compile(
            "(金属软管)" +                      // 目标配件类型
                    "(DN\\d+(?:\\.\\d+)?)" +        // 配件规格
                    "(?!.*\\d+\\s*[m米])",          // 排除含长度单位的情况
            Pattern.CASE_INSENSITIVE);
    private static final Pattern FLANGE_COVER_PATTERN = Pattern.compile(
            "(法兰盖)" +                      // 目标配件类型
                    "(DN\\d+(?:\\.\\d+)?)" +        // 配件规格
                    "(?!.*\\d+\\s*[m米])",          // 排除含长度单位的情况
            Pattern.CASE_INSENSITIVE);


    private final List<CadItem> cadItems = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        OutboundPipeAnalyzer analyzer = new OutboundPipeAnalyzer();
        List<CadItem> result = analyzer.executeAnalysis("C:\\Users\\Liming\\Desktop\\cad_file3.zip");
        System.out.println(JSON.toJSONString(result));
    }

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
            log.error("DXF处理失败: " + file + " - " + e.getMessage());
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
            Matcher flangeBallValveMatcher = FLANGE_BALL_VALVE_PATTERN.matcher(currentText);
            Matcher metalBallValveMatcher = METAL_BALL_VALVE_PATTERN.matcher(currentText);
            Matcher metalHoseMatcher = METAL_HOSE_PATTERN.matcher(currentText);
            Matcher flangeCoverMatcher = FLANGE_COVER_PATTERN.matcher(currentText);

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
                item.setType(TypeEnums.PIPE.getType());
                item.setData(length);
                item.setSpec(spec);
                item.setNominalSpec(PipeDiameter.getPipeDiameterStr(spec));
                cadItems.add(item);
            } else if (flangeBallValveMatcher.find()) {
                CadItem item = new CadItem();
                item.setAlias(flangeBallValveMatcher.group(1));
                item.setUnit("个");
                item.setType(TypeEnums.FERRULE_BALL_VALVE.getType());
                item.setData(new BigDecimal(1));
                item.setSpec(flangeBallValveMatcher.group(2).toUpperCase());
                item.setNominalSpec(item.getSpec());
                cadItems.add(item);
            } else if (metalBallValveMatcher.find()) {
                CadItem item = new CadItem();
                item.setAlias(metalBallValveMatcher.group(1));
                item.setUnit("个");
                item.setType(TypeEnums.METAL_BALL_VALVE.getType());
                item.setData(new BigDecimal(1));
                item.setSpec(metalBallValveMatcher.group(2).toUpperCase());
                item.setNominalSpec(item.getSpec());
            } else if (metalHoseMatcher.find()) {
                CadItem item = new CadItem();
                item.setAlias(flangeBallValveMatcher.group(1));
                item.setUnit("个");
                item.setType(TypeEnums.METAL_HOSE.getType());
                item.setData(new BigDecimal(1));
                item.setSpec(flangeBallValveMatcher.group(2).toUpperCase());
                item.setNominalSpec(item.getSpec());
                cadItems.add(item);
            } else if (flangeCoverMatcher.find()) {
                CadItem item = new CadItem();
                item.setAlias(flangeBallValveMatcher.group(1));
                item.setUnit("个");
                item.setType(TypeEnums.FERRULE_COVER.getType());
                item.setData(new BigDecimal(1));
                item.setSpec(flangeBallValveMatcher.group(2).toUpperCase());
                item.setNominalSpec(item.getSpec());
                cadItems.add(item);
            }
        } catch (Exception e) {
            log.error("文本解析异常: {} - {}", text.getText(), e.getMessage());
        }
    }

    private List<CadItem> generateSummary() {

        // 按 alias 和 spec 共同分组
        Map<String, Map<String, List<CadItem>>> groupedData = cadItems.stream()
                .collect(Collectors.groupingBy(
                        CadItem::getType,
                        Collectors.groupingBy(CadItem::getNominalSpec)
                ));
        List<CadItem> result = new ArrayList<>();
        for (Map.Entry<String, Map<String, List<CadItem>>> entry : groupedData.entrySet()) {
            Map<String, List<CadItem>> grouped1 = entry.getValue();
            for (Map.Entry<String, List<CadItem>> entry1 : grouped1.entrySet()) {
                String key1 = entry1.getKey();
                List<CadItem> items1 = entry1.getValue();
                CadItem item = new CadItem();
                item.setName("");
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
        return result;
    }
}