package com.jinghu.cad.analysis.excel;

import com.alibaba.excel.annotation.ExcelProperty;

/**
 * @author liming
 * @version 1.0
 * @description MaterialBill5Data 核销单价
 * @date 2025/3/27 14:32
 */
public class MaterialBill5Data {
    @ExcelProperty("物料编码")
    private String materialCode;
    @ExcelProperty("物料名称")
    private String materialName;
    @ExcelProperty("规格型号")
    private String materialSpec;
    @ExcelProperty("单位")
    private String materialUnit;
    @ExcelProperty("核销单价")
    private String materialPrice;
}

