package com.jinghu.cad.analysis.enmus;

/**
 * @author liming
 * @version 1.0
 * @description PipeDiameter
 * @date 2025/3/25 10:45
 */

/**
 * 钢管直径标准枚举
 * 包含公称直径和两种外径系列(A系列/B系列)
 */
public enum PipeDiameter {
    DN15(15, 21.3, 18.0),
    DN20(20, 26.9, 25.0),
    DN25(25, 33.7, 32.0),
    DN32(32, 42.4, 38.0),
    DN40(40, 48.3, 45.0),
    DN50(50, 60.3, 57.0),
    DN65(65, 76.1, 76.0),
    DN80(80, 88.9, 89.0),
    DN100(100, 114.3, 108.0),
    DN125(125, 139.7, 133.0),
    DN150(150, 168.3, 159.0),
    DN200(200, 219.1, 219.0),
    DN250(250, 273.0, 273.0),
    DN300(300, 323.9, 325.0),
    DN350(350, 355.6, 377.0),
    DN400(400, 406.4, 426.0),
    DN450(450, 457.0, 478.0),
    DN500(500, 508.0, 529.0),
    DN600(600, 610.0, 630.0);

    private final int nominalDiameter;        // 公称直径(mm)
    private final double seriesAOuterDiameter; // A系列外径(美标大外径管)(mm)
    private final double seriesBOuterDiameter; // B系列外径(欧标小外径管)(mm)

    PipeDiameter(int nominalDiameter, double seriesAOuterDiameter, double seriesBOuterDiameter) {
        this.nominalDiameter = nominalDiameter;
        this.seriesAOuterDiameter = seriesAOuterDiameter;
        this.seriesBOuterDiameter = seriesBOuterDiameter;
    }

    // 获取公称直径(mm)
    public int getNominalDiameter() {
        return nominalDiameter;
    }

    // 获取A系列外径(mm)(美标大外径管)
    public double getSeriesAOuterDiameter() {
        return seriesAOuterDiameter;
    }

    // 获取B系列外径(mm)(欧标小外径管)
    public double getSeriesBOuterDiameter() {
        return seriesBOuterDiameter;
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