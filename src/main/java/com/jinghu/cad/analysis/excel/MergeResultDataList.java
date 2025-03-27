package com.jinghu.cad.analysis.excel;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @author liming
 * @version 1.0
 * @description MergeResultDataList
 * @date 2025/3/26 13:35
 */
@Data
public class MergeResultDataList {
    public static String part1Name = ConfirmFileDataList.part1Name;
    public static String part2Name = ConfirmFileDataList.part2Name;
    public static String part3Name = ConfirmFileDataList.part3Name;
    private List<MergeResultData> part1 = new ArrayList<>();
    private List<MergeResultData> part2 = new ArrayList<>();
    private List<MergeResultData> part3 = new ArrayList<>();

    public List<MergeResultData> adapter() {
        getPart1().forEach(mergeResultData -> {
            mergeResultData.setPartType(part1Name);
        });
        getPart2().forEach(mergeResultData -> {
            mergeResultData.setPartType(part2Name);
        });
        getPart3().forEach(mergeResultData -> {
            mergeResultData.setPartType(part3Name);
        });
        List<MergeResultData> result = new ArrayList<>();
        result.addAll(getPart1());
        result.addAll(getPart2());
        result.addAll(getPart3());
        return result;
    }
}
