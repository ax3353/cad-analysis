package com.jinghu.cad.analysis.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * cad图纸识别数据项
 */
@Data
public class CadItem {

    /**
     * 名称
     */
    private String name;

    /**
     * 别名
     */
    private String alias;

    /**
     * 单位
     */
    private String unit;

    /**
     * 类型 管道 法兰 楼层数
     */
    private String type;

    /**
     * 数据
     */
    private BigDecimal data;

    /**
     * 原规格
     */
    private String spec;

    /**
     * 公制原规格
     */
    private String nominalSpec;

    /**
     * 是否合并
     */
    private Boolean isMerge = false;
}