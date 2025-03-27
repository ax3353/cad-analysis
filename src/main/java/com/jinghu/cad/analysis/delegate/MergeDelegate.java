package com.jinghu.cad.analysis.delegate;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.write.style.column.LongestMatchColumnWidthStyleStrategy;
import com.jinghu.cad.analysis.dto.CadItem;
import com.jinghu.cad.analysis.excel.ConfirmFileData;
import com.jinghu.cad.analysis.excel.ConfirmFileDataList;
import com.jinghu.cad.analysis.excel.MergeResultData;
import com.jinghu.cad.analysis.excel.MergeResultDataList;
import com.jinghu.cad.analysis.service.IMaterialBillService;
import com.jinghu.cad.analysis.utils.SpringUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author liming
 * @version 1.0
 * @description MergeDelegate 合并逻辑的相关委托
 * @date 2025/3/26 13:17
 */
@Data
@Slf4j
public class MergeDelegate {

    private static IMaterialBillService iMaterialBillService = SpringUtils.getBean(IMaterialBillService.class);

    public static MergeResultDataList merge(List<CadItem> buildingPipeData, List<CadItem> outboundPipeData, ConfirmFileDataList confirmFileDataList) {
        List<MergeResultData> partResult1 = new ArrayList<>();
        List<MergeResultData> partResult2 = new ArrayList<>();
        List<MergeResultData> partResult3 = new ArrayList<>();
        // 楼栋户数 对应不上 直接左右拼接，工程量在上，识别在下
        List<ConfirmFileData> part1 = confirmFileDataList.getPart1();
        AtomicInteger index = new AtomicInteger(1);
        if (!CollectionUtils.isEmpty(part1)) {
            partResult1.addAll(part1.stream().map(confirmFileData -> {
                MergeResultData mergeResultData = new MergeResultData();
                mergeResultData.setNumber(String.valueOf(index.getAndIncrement()));
                mergeResultData.setDataOrigin("工程量确认单");
                mergeResultData.setName(confirmFileData.getName());
                mergeResultData.setNominalSpec("");
                mergeResultData.setUnit(confirmFileData.getUnit());
                mergeResultData.setCadSpec("-");
                mergeResultData.setCadCount("-");
                mergeResultData.setWorkName(confirmFileData.getName());
                mergeResultData.setWorkSpec("-");
                mergeResultData.setWorkCount(confirmFileData.getThisMonthCount());
                mergeResultData.setDiffCount("-");
                mergeResultData.setDiffPercentage("-");
                return mergeResultData;
            }).collect(Collectors.toList()));
        }
        if (!CollectionUtils.isEmpty(buildingPipeData)) {
            partResult1.addAll(buildingPipeData.stream().map(cadItem -> {
                MergeResultData mergeResultData = new MergeResultData();
                mergeResultData.setNumber(String.valueOf(index.getAndIncrement()));
                mergeResultData.setDataOrigin("CAD识别");
                mergeResultData.setName(cadItem.getName());
                mergeResultData.setNominalSpec("");
                mergeResultData.setUnit(cadItem.getUnit());
                mergeResultData.setCadSpec(cadItem.getSpec());
                mergeResultData.setCadCount(String.valueOf(cadItem.getData()));
                mergeResultData.setWorkName("-");
                mergeResultData.setWorkSpec("-");
                mergeResultData.setWorkCount("-");
                mergeResultData.setDiffCount("-");
                mergeResultData.setDiffPercentage("-");
                return mergeResultData;
            }).collect(Collectors.toList()));
        }

        List<ConfirmFileData> part2 = confirmFileDataList.getPart2();
        Map<String, Map<String, CadItem>> groupedData = outboundPipeData.stream()
                .collect(Collectors.groupingBy(
                        CadItem::getType,
                        Collectors.toMap(CadItem::getNominalSpec, Function.identity())
                ));
        AtomicInteger index2 = new AtomicInteger(1);
        if (!CollectionUtils.isEmpty(part2)) {
            for (ConfirmFileData confirmFileData : part2) {
                MergeResultData mergeResultData = new MergeResultData();
                mergeResultData.setNumber(String.valueOf(index2.getAndIncrement()));
                mergeResultData.setDataOrigin("工程量确认单");
                mergeResultData.setName(confirmFileData.getName());
                mergeResultData.setMaterialCode(iMaterialBillService.getMaterialCode(mergeResultData.getName()));
                mergeResultData.setNominalSpec(confirmFileData.getNominalSpec());
                mergeResultData.setUnit(confirmFileData.getUnit());

                Map<String, CadItem> typeMap = groupedData.get(confirmFileData.getType());
                if (typeMap != null) {
                    CadItem cadItems = typeMap.get(confirmFileData.getNominalSpec());
                    if (cadItems != null && !cadItems.getIsMerge()) {
                        cadItems.setIsMerge(true);
                        mergeResultData.setCadSpec(cadItems.getSpec());
                        mergeResultData.setCadCount(String.valueOf(cadItems.getData()));
                    }
                }
                mergeResultData.setWorkName(confirmFileData.getName());
                mergeResultData.setWorkSpec(confirmFileData.getWorkSpec());
                mergeResultData.setWorkCount(confirmFileData.getThisMonthCount());

                if (StringUtils.isNotBlank(mergeResultData.getCadCount()) && StringUtils.isNotBlank(mergeResultData.getWorkCount())) {
                    BigDecimal cadCount = new BigDecimal(mergeResultData.getCadCount());
                    BigDecimal workCount = new BigDecimal(mergeResultData.getWorkCount());
                    mergeResultData.setDiffCount(String.valueOf(workCount.subtract(cadCount)));
                    mergeResultData.setDiffPercentage(String.format("%.2f%%",
                            workCount.subtract(cadCount) // (workCount - cadCount)
                                    .multiply(BigDecimal.valueOf(100)) // 乘以 100
                                    .divide(cadCount, 2, RoundingMode.HALF_UP) // 除以 cadCount，并保留两位小数
                    ));

                }
                partResult2.add(mergeResultData);
            }
        }

        List<CadItem> notMergeData2 = outboundPipeData.stream().filter(cadItem -> !cadItem.getIsMerge()).collect(Collectors.toList());

        if (!CollectionUtils.isEmpty(notMergeData2)) {
            for (CadItem cadItem : notMergeData2) {
                MergeResultData mergeResultData = new MergeResultData();
                mergeResultData.setNumber(String.valueOf(index2.getAndIncrement()));
                mergeResultData.setDataOrigin("CAD识别");
                mergeResultData.setName(cadItem.getName());
                mergeResultData.setNominalSpec(cadItem.getNominalSpec());
                mergeResultData.setUnit(cadItem.getUnit());
                mergeResultData.setCadSpec(cadItem.getSpec());
                mergeResultData.setCadCount(String.valueOf(cadItem.getData()));
                mergeResultData.setWorkName("-");
                mergeResultData.setWorkSpec("-");
                mergeResultData.setWorkCount("-");
                mergeResultData.setDiffCount("-");
                mergeResultData.setDiffPercentage("-");
                partResult2.add(mergeResultData);
            }
        }

        List<ConfirmFileData> part3 = confirmFileDataList.getPart3();
        if (!CollectionUtils.isEmpty(part3)) {
            for (ConfirmFileData confirmFileData : part3) {
                MergeResultData mergeResultData = new MergeResultData();
                mergeResultData.setNumber(String.valueOf(index2.getAndIncrement()));
                mergeResultData.setDataOrigin("工程量确认单");
                mergeResultData.setName(confirmFileData.getName());
                mergeResultData.setMaterialCode(iMaterialBillService.getMaterialCode(mergeResultData.getName()));
                mergeResultData.setNominalSpec(confirmFileData.getNominalSpec());
                mergeResultData.setUnit(confirmFileData.getUnit());

                mergeResultData.setCadSpec(null);
                mergeResultData.setCadCount(null);

                mergeResultData.setWorkName(confirmFileData.getName());
                mergeResultData.setWorkSpec(confirmFileData.getWorkSpec());
                mergeResultData.setWorkCount(confirmFileData.getThisMonthCount());

                if (StringUtils.isNotBlank(mergeResultData.getCadCount()) && StringUtils.isNotBlank(mergeResultData.getWorkCount())) {
                    BigDecimal cadCount = new BigDecimal(mergeResultData.getCadCount());
                    BigDecimal workCount = new BigDecimal(mergeResultData.getWorkCount());
                    mergeResultData.setDiffCount(String.valueOf(workCount.subtract(cadCount)));
                    mergeResultData.setDiffPercentage(String.format("%.2f%%",
                            workCount.subtract(cadCount) // (workCount - cadCount)
                                    .multiply(BigDecimal.valueOf(100)) // 乘以 100
                                    .divide(cadCount, 2, RoundingMode.HALF_UP) // 除以 cadCount，并保留两位小数
                    ));

                }
                partResult3.add(mergeResultData);
            }
        }

        MergeResultDataList resultData = new MergeResultDataList();
        resultData.getPart1().addAll(partResult1);
        resultData.getPart2().addAll(partResult2);
        resultData.getPart3().addAll(partResult3);
        return resultData;
    }

    public static File generateExcel(String uploadPath, MergeResultDataList mergeResultDataList) {
        String timestamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        String fileName = timestamp + "_" + UUID.randomUUID() + "_" + "CAD图纸智能分析结果.xlsx";
        File dest = Paths.get(uploadPath, "files", fileName).toFile();

        List<MergeResultData> resultData = new ArrayList<>();
        resultData.add(new MergeResultData("一", ConfirmFileDataList.part1Name));
        resultData.addAll(mergeResultDataList.getPart1());
        resultData.add(new MergeResultData("二", ConfirmFileDataList.part2Name));
        resultData.addAll(mergeResultDataList.getPart2());
        resultData.add(new MergeResultData("三", ConfirmFileDataList.part3Name));
        resultData.addAll(mergeResultDataList.getPart3());

        EasyExcel.write(dest, MergeResultData.class).registerWriteHandler(new LongestMatchColumnWidthStyleStrategy()).sheet("CAD图纸智能分析结果").doWrite(resultData);
//        FileUtils.openFile(dest.getAbsolutePath());
        log.info("生成excel文件：{}", dest.getAbsolutePath());
        return dest;
    }
}
