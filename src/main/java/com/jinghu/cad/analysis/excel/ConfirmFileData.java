package com.jinghu.cad.analysis.excel;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * @author liming
 * @version 1.0
 * @description ConfirmFileData
 * @date 2025/3/26 12:34
 */
@Getter
@Setter
@EqualsAndHashCode
public class ConfirmFileData {
    @ExcelProperty("序号")
    private String number;
    @ExcelProperty("工程项目名称")
    private String name;
    @ExcelProperty("规格型号")
    private String workSpec;
    @ExcelProperty("单位")
    private String unit;
    @ExcelProperty("预算工程量")
    private String budgetCount;
    @ExcelProperty("本月完成量")
    private String thisMonthCount;
    @ExcelProperty("累计完成量")
    private String cumulativeCount;
    @ExcelProperty("备注")
    private String remark;

    @ExcelIgnore
    private String type;
    /**
     * 公制原规格
     */
    @ExcelIgnore
    private String nominalSpec;
}
