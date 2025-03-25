package com.jinghu.cad.analysis.enmus;

/**
 * @author liming
 * @version 1.0
 * @description PipeDiameter
 * @date 2025/3/25 10:45
 */

import lombok.Getter;

/**
 * 钢管直径标准枚举
 * 包含公称直径和两种外径系列(A系列/B系列)
 */
@Getter
public enum PipeDiameter {
    DN15(15, "DN15", 21.3, "D21.3", 18.0, "D18.0"),
    DN20(20, "DN20", 26.9, "D26.9", 25.0, "D25.0"),
    DN25(25, "DN25", 33.7, "D33.7", 32.0, "D32.0"),
    DN32(32, "DN32", 42.4, "D42.4", 38.0, "D38.0"),
    DN40(40, "DN40", 48.3, "D48.3", 45.0, "D45.0"),
    DN50(50, "DN50", 60.3, "D60.3", 57.0, "D57.0"),
    DN65(65, "DN65", 76.1, "D76.1", 76.0, "D76.0"),
    DN80(80, "DN80", 88.9, "D88.9", 89.0, "D89.0"),
    DN100(100, "DN100", 114.3, "D114.3", 108.0, "D108.0"),
    DN125(125, "DN125", 139.7, "D139.7", 133.0, "D133.0"),
    DN150(150, "DN150", 168.3, "D168.3", 159.0, "D159.0"),
    DN200(200, "DN200", 219.1, "D219.1", 219.0, "D219.0"),
    DN250(250, "DN250", 273.0, "D273.0", 273.0, "D273.0"),
    DN300(300, "DN300", 323.9, "D323.9", 325.0, "D325.0"),
    DN350(350, "DN350", 355.6, "D355.6", 377.0, "D377.0"),
    DN400(400, "DN400", 406.4, "D406.4", 426.0, "D426.0"),
    DN450(450, "DN450", 457.0, "D457.0", 478.0, "D478.0"),
    DN500(500, "DN500", 508.0, "D508.0", 529.0, "D529.0"),
    DN600(600, "DN600", 610.0, "D610.0", 630.0, "D630.0");

    // 获取公称直径(mm)
    private final int nominalDiameter;        // 公称直径(mm)
    private final String nominalDiameterAlias;
    // 获取A系列外径(mm)(美标大外径管)
    private final double seriesAOuterDiameter; // A系列外径(美标大外径管)(mm)
    private final String seriesAOuterDiameterAlias;
    // 获取B系列外径(mm)(欧标小外径管)
    private final double seriesBOuterDiameter; // B系列外径(欧标小外径管)(mm)
    private final String seriesBOuterDiameterAlias;

    PipeDiameter(int nominalDiameter,String nominalDiameterAlias, double seriesAOuterDiameter, String  seriesAOuterDiameterAlias, double seriesBOuterDiameter, String seriesBOuterDiameterAlias) {
        this.nominalDiameter = nominalDiameter;
        this.nominalDiameterAlias = nominalDiameterAlias;
        this.seriesAOuterDiameter = seriesAOuterDiameter;
        this.seriesAOuterDiameterAlias = seriesAOuterDiameterAlias;
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
}