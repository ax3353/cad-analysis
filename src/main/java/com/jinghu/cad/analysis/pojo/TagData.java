package com.jinghu.cad.analysis.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * cad图纸标注数据
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TagData {

    private String cadId;

    private String cadLayer;

    private String type;

    private String spec;

    private String alias;

    private String text;

    private BigDecimal data;

    private String unit;
}