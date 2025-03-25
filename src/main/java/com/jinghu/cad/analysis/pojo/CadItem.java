package com.jinghu.cad.analysis.pojo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * cad图纸识别数据项
 */
@Data
public class CadItem {

    private String alias;

    private String unit;

    private String type;

    private BigDecimal data;

    private String spec;

    @Override
    public String toString() {
        return "Item{" +
                "alias='" + alias + '\'' +
                ", unit='" + unit + '\'' +
                ", type='" + type + '\'' +
                ", data=" + data +
                ", spec='" + spec + '\'' +
                '}';
    }
}