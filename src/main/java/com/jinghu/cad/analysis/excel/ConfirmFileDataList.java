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
    List<ConfirmFileData> part1 = new ArrayList<>();
    List<ConfirmFileData> part2 = new ArrayList<>();
    List<ConfirmFileData> part3 = new ArrayList<>();
}
