package com.jinghu.cad.analysis.enmus;

/**
 * @author liming
 * @version 1
 * @description PipeDiameter
 * @date 2025/3/25 10:45
 */

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 钢管直径标准枚举
 * 包含公称直径和两种外径系列(A系列/B系列)
 */
@Getter
public enum PipeDiameter {
    UNKNOWN(0, "UNKNOWN", 0, "UNKNOWN", "UNKNOWN", 0, "UNKNOWN"),
    DN15(15, "DN15", 21.3, "D21.3", "D21", 18, "D18"),
    DN20(20, "DN20", 26.9, "D26.9", "D26", 25, "D25"),
    DN25(25, "DN25", 33.7, "D33.7", "D33", 32, "D32"),
    DN32(32, "DN32", 42.4, "D42.4", "D42", 38, "D38"),
    DN40(40, "DN40", 48.3, "D48.3", "D48", 45, "D45"),
    DN50(50, "DN50", 60.3, "D60.3", "D60", 57, "D57"),
    DN65(65, "DN65", 76.1, "D76.1", "D76", 76, "D76"),
    DN80(80, "DN80", 88.9, "D88.9", "D88", 89, "D89"),
    DN100(100, "DN100", 114.3, "D114.3", "D114", 108, "D108"),
    DN125(125, "DN125", 139.7, "D139.7", "D139", 133, "D133"),
    DN150(150, "DN150", 168.3, "D168.3", "D168", 159, "D159"),
    DN200(200, "DN200", 219.1, "D219.1", "D219", 219, "D219"),
    DN250(250, "DN250", 273, "D273", "D273", 273, "D273"),
    DN300(300, "DN300", 323.9, "D323.9", "D323", 325, "D325"),
    DN350(350, "DN350", 355.6, "D355.6", "D355", 377, "D377"),
    DN400(400, "DN400", 406.4, "D406.4", "D406", 426, "D426"),
    DN450(450, "DN450", 457, "D457", "D457", 478, "D478"),
    DN500(500, "DN500", 508, "D508", "D508", 529, "D529"),
    DN600(600, "DN600", 610, "D610", "D610", 630, "D630");

    // 获取公称直径(mm)
    private final int nominalDiameter;        // 公称直径(mm)
    private final String nominalDiameterAlias;
    // 获取A系列外径(mm)(美标大外径管)
    private final double seriesAOuterDiameter; // A系列外径(美标大外径管)(mm)
    private final String seriesAOuterDiameterAlias;//包含小数点
    private final String seriesAOuterDiameterAlias1;//去掉小数点
    // 获取B系列外径(mm)(欧标小外径管)
    private final double seriesBOuterDiameter; // B系列外径(欧标小外径管)(mm)
    private final String seriesBOuterDiameterAlias;

    PipeDiameter(int nominalDiameter, String nominalDiameterAlias, double seriesAOuterDiameter, String seriesAOuterDiameterAlias, String seriesAOuterDiameterAlias1, double seriesBOuterDiameter, String seriesBOuterDiameterAlias) {
        this.nominalDiameter = nominalDiameter;
        this.nominalDiameterAlias = nominalDiameterAlias;
        this.seriesAOuterDiameter = seriesAOuterDiameter;
        this.seriesAOuterDiameterAlias = seriesAOuterDiameterAlias;
        this.seriesAOuterDiameterAlias1 = seriesAOuterDiameterAlias1;
        this.seriesBOuterDiameter = seriesBOuterDiameter;
        this.seriesBOuterDiameterAlias = seriesBOuterDiameterAlias;
    }

    @Override
    public String toString() {
        return "DN" + nominalDiameter +
                " (A系列: Ø" + seriesAOuterDiameter + "mm, " +
                "B系列: Ø" + seriesBOuterDiameter + "mm)";
    }

    public static void main(String[] args) {
        PipeDiameter pipe = PipeDiameter.DN100;

        System.out.println(pipe);
        System.out.println("公称直径: " + pipe.getNominalDiameter() + "mm");
        System.out.println("A系列外径(美标): " + pipe.getSeriesAOuterDiameter() + "mm");
        System.out.println("B系列外径(欧标): " + pipe.getSeriesBOuterDiameter() + "mm");
    }

    public static final List<String> nominalDiameterAliasList = Arrays.stream(PipeDiameter.values()).map(PipeDiameter::getNominalDiameterAlias).collect(Collectors.toList());
    public static final List<String> seriesAOuterDiameterAliasList = Arrays.stream(PipeDiameter.values()).map(PipeDiameter::getSeriesAOuterDiameterAlias).collect(Collectors.toList());
    public static final List<String> seriesAOuterDiameterAlias1List = Arrays.stream(PipeDiameter.values()).map(PipeDiameter::getSeriesAOuterDiameterAlias1).collect(Collectors.toList());
    public static final List<String> seriesBOuterDiameterAliasList = Arrays.stream(PipeDiameter.values()).map(PipeDiameter::getSeriesBOuterDiameterAlias).collect(Collectors.toList());

    /**
     * 非标转公制
     *
     * @param spec 待识别规格
     * @return
     */
    public static PipeDiameter getPipeDiameter(String spec) {
        if (StringUtils.isBlank(spec)) {
            return UNKNOWN;
        }
        if (nominalDiameterAliasList.contains(spec)) {
            return Arrays.stream(PipeDiameter.values()).filter(pipeDiameter -> pipeDiameter.getNominalDiameterAlias().equals(spec)).findFirst().orElse(null);
        }
        if (seriesAOuterDiameterAliasList.contains(spec)) {
            return Arrays.stream(PipeDiameter.values()).filter(pipeDiameter -> pipeDiameter.getSeriesAOuterDiameterAlias().equals(spec)).findFirst().orElse(null);
        }
        if (seriesBOuterDiameterAliasList.contains(spec)) {
            return Arrays.stream(PipeDiameter.values()).filter(pipeDiameter -> pipeDiameter.getSeriesBOuterDiameterAlias().equals(spec)).findFirst().orElse(null);
        }
        // 还有没写小数点的
        if (seriesAOuterDiameterAlias1List.contains(spec)) {
            return Arrays.stream(PipeDiameter.values()).filter(pipeDiameter -> pipeDiameter.getSeriesAOuterDiameterAlias1().equals(spec)).findFirst().orElse(null);
        }
        return UNKNOWN;
    }
}