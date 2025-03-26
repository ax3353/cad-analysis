package com.jinghu.cad.analysis.analyzer;

import com.alibaba.excel.EasyExcel;
import com.alibaba.fastjson.JSON;
import com.jinghu.cad.analysis.enmus.PipeDiameter;
import com.jinghu.cad.analysis.enmus.TypeEnums;
import com.jinghu.cad.analysis.excel.ConfirmFileData;
import com.jinghu.cad.analysis.excel.ConfirmFileDataList;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * @author liming
 * @version 1.0
 * @description ConfirmFileAnalyzer
 * @date 2025/3/26 12:21
 */
@Slf4j
public class ConfirmFileAnalyzer {

    private final ConfirmFileDataList pipeInfos = new ConfirmFileDataList();

    public ConfirmFileDataList executeAnalysis(String filePath) {
        try {
            if (filePath.startsWith("http://") || filePath.startsWith("https://")) {
                processRemote(filePath);
            } else if (filePath.endsWith(".xlsx")) {
                processLocal(filePath);
            }
            return this.generateSummary();
        } catch (Exception e) {
            log.error("工程量确认单文件分析失败", e);
            return new ConfirmFileDataList();
        }
    }

    /**
     * 远程文件，如：http://www.qq.com/test.zip
     */
    private void processRemote(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

//        try (InputStream is = connection.getInputStream()) {
//            Path tempDir = Files.createTempDirectory("confirm_file");
//            Path tempZip = Files.createTempFile("temp", ".xlsx");
//            Files.copy(is, tempZip, StandardCopyOption.REPLACE_EXISTING);
//            processFiles(tempDir);
//        }
    }

    /**
     * 本地文件，如：D:\test.xlsx
     */
    private void processLocal(String xlsxPath) throws Exception {
        processFiles(xlsxPath);
    }


    /**
     * 本地DXF文件，如：D:\test.xlsx
     */
    private void processFiles(String xlsxPath) {
        List<ConfirmFileData> part1 = new ArrayList<>();
        List<ConfirmFileData> part2 = new ArrayList<>();
        List<ConfirmFileData> part3 = new ArrayList<>();
        List<ConfirmFileData> current = null;
        try {
            File file = new File(xlsxPath);
            List<ConfirmFileData> list = EasyExcel.read(file).head(ConfirmFileData.class).sheet().doReadSync();
            for (ConfirmFileData confirmFileData : list) {
                boolean isAdd = true;
                if (confirmFileData.getName().contains("户内部分")) {
                    current = part1;
                    isAdd = false;
                }
                if (confirmFileData.getName().contains("庭院低压部分（调压箱后管道）")) {
                    current = part2;
                    isAdd = false;
                }
                if (confirmFileData.getName().contains("庭院中压部分（调压箱/调压柜前管道）")) {
                    current = part3;
                    isAdd = false;
                }
                assert current != null;
                if (isAdd) {
                    boolean isMatch = false;
                    if (confirmFileData.getName().contains("管") && confirmFileData.getUnit().equals("米")) {
                        confirmFileData.setType(TypeEnums.PIPE.getType());
                        confirmFileData.setNominalSpec(PipeDiameter.getPipeDiameter(confirmFileData.getWorkSpec()).getNominalDiameterAlias());
                        isMatch = true;
                    }
                    if (confirmFileData.getName().contains("法兰球阀")) {
                        confirmFileData.setType(TypeEnums.FERRULE.getType());
                        isMatch = true;
                    }
                    if (!isMatch) {
                        confirmFileData.setType(TypeEnums.UNKNOWN.getType());
                    }
                    current.add(confirmFileData);
                }
            }
            pipeInfos.getPart1().addAll(part1);
            pipeInfos.getPart2().addAll(part2);
            pipeInfos.getPart3().addAll(part3);
        } catch (Exception e) {
            log.error("XLSX处理失败: {} - {}", xlsxPath, e.getMessage());
        }
    }

    private ConfirmFileDataList generateSummary() {
        return pipeInfos;
    }

    public static void main(String[] args) {
        ConfirmFileAnalyzer analyzer = new ConfirmFileAnalyzer();
        ConfirmFileDataList result = analyzer.executeAnalysis("C:\\Users\\Liming\\Desktop\\江湾明珠4#-6#楼燃气管道工程-工程量确认单.xlsx");
        System.out.println(JSON.toJSONString(result));
    }

}
