package com.jinghu.cad.analysis.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * @author liming
 * @version 1.0
 * @description ComplexHeadData
 * @date 2025/3/26 11:36
 */
@Getter
@Setter
@EqualsAndHashCode
public class ComplexHeadData {
    @ExcelProperty({"CAD图纸智能分析结果", "基础信息", "序号"})
    private String number;

    @ExcelProperty({"CAD图纸智能分析结果", "基础信息", "名称"})
    private String name;

    @ExcelProperty({"CAD图纸智能分析结果", "基础信息", "规格型号（公称）"})
    private String nominalSpec;

    @ExcelProperty({"CAD图纸智能分析结果", "基础信息", "单位"})
    private String unit;

    @ExcelProperty({"CAD图纸智能分析结果", "CAD自动识别", "规格型号（CAD标注）"})
    private String cadSpec;

    @ExcelProperty({"CAD图纸智能分析结果", "CAD自动识别", "CAD图纸数量"})
    private String cadCount;

    @ExcelProperty({"CAD图纸智能分析结果", "工程量确认单", "工程量名称"})
    private String workName;

    @ExcelProperty({"CAD图纸智能分析结果", "工程量确认单", "规格型号"})
    private String workSpec;

    @ExcelProperty({"CAD图纸智能分析结果", "工程量确认单", "工程量数量"})
    private String workCount;

    @ExcelProperty({"CAD图纸智能分析结果", "分析结果", "对比差异"})
    private String diffCount;

    @ExcelProperty({"CAD图纸智能分析结果", "分析结果", "对比分析"})
    private String diffPercentage;
}