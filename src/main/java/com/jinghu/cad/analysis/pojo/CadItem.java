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

    /**
     * 原规格
     */
    private String spec;

    /**
     * 公制原规格
     */
    private String nominalSpec;
}