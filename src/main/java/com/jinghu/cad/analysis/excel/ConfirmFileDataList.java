package com.jinghu.cad.analysis.excel;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @author liming
 * @version 1.0
 * @description ConfirmFileDataList
 * @date 2025/3/26 12:52
 */
@Data
public class ConfirmFileDataList {
    public static String part1Name = "户内部分";
    public static String part2Name = "庭院低压部分（调压箱后管道）";
    public static String part3Name = "庭院中压部分（调压箱/调压柜前管道）";
    private List<ConfirmFileData> part1 = new ArrayList<>();
    private List<ConfirmFileData> part2 = new ArrayList<>();
    private List<ConfirmFileData> part3 = new ArrayList<>();
}
