package com.jinghu.cad.analysis.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * @author liming
 * @version 1.0
 * @description MaterialBill1Data 库存红线设置 (2024年12月)
 * @date 2025/3/27 12:14
 */
@Getter
@Setter
@EqualsAndHashCode
public class MaterialBill1Data {
    //序号	物资属性	集采/自采	物资类别	新编码	新规格型号	单位	最新价格	红线量	备注
    @ExcelProperty("序号")
    private String number;
    @ExcelProperty("物资属性")
    private String materialAttribute;
    @ExcelProperty("集采/自采")
    private String collection;
    @ExcelProperty("物资类别")
    private String materialType;
    @ExcelProperty("新编码")
    private String materialCode;
    @ExcelProperty("新规格型号")
    private String materialSpec;
    @ExcelProperty("单位")
    private String unit;
    @ExcelProperty("最新价格")
    private String latestPrice;
    @ExcelProperty("红线量")
    private String redLine;
    @ExcelProperty("备注")
    private String remark;
}
