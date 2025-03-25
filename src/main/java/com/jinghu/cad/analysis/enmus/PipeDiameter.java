package com.jinghu.cad.analysis.enmus;

/**
 * @author liming
 * @version 1
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
    DN15(15, "DN15", 21.3, "D21.3", 18, "D18"),
    DN20(20, "DN20", 26.9, "D26.9", 25, "D25"),
    DN25(25, "DN25", 33.7, "D33.7", 32, "D32"),
    DN32(32, "DN32", 42.4, "D42.4", 38, "D38"),
    DN40(40, "DN40", 48.3, "D48.3", 45, "D45"),
    DN50(50, "DN50", 60.3, "D60.3", 57, "D57"),
    DN65(65, "DN65", 76.1, "D76.1", 76, "D76"),
    DN80(80, "DN80", 88.9, "D88.9", 89, "D89"),
    DN100(100, "DN100", 114.3, "D114.3", 108, "D108"),
    DN125(125, "DN125", 139.7, "D139.7", 133, "D133"),
    DN150(150, "DN150", 168.3, "D168.3", 159, "D159"),
    DN200(200, "DN200", 219.1, "D219.1", 219, "D219"),
    DN250(250, "DN250", 273, "D273", 273, "D273"),
    DN300(300, "DN300", 323.9, "D323.9", 325, "D325"),
    DN350(350, "DN350", 355.6, "D355.6", 377, "D377"),
    DN400(400, "DN400", 406.4, "D406.4", 426, "D426"),
    DN450(450, "DN450", 457, "D457", 478, "D478"),
    DN500(500, "DN500", 508, "D508", 529, "D529"),
    DN600(600, "DN600", 610, "D610", 630, "D630");

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