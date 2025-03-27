package com.jinghu.cad.analysis.controller;

import com.jinghu.cad.analysis.analyzer.BuildingPipeAnalyzer;
import com.jinghu.cad.analysis.analyzer.ConfirmFileAnalyzer;
import com.jinghu.cad.analysis.analyzer.OutboundPipeAnalyzer;
import com.jinghu.cad.analysis.delegate.MergeDelegate;
import com.jinghu.cad.analysis.excel.ConfirmFileDataList;
import com.jinghu.cad.analysis.excel.MergeResultDataList;
import com.jinghu.cad.analysis.dto.CadItem;
import com.jinghu.cad.analysis.vo.req.ReportRequest;
import com.jinghu.cad.analysis.vo.resp.R;
import com.jinghu.cad.analysis.vo.resp.ReportResp;
import com.jinghu.cad.analysis.utils.FileUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/")
public class ReportController {

    @Value("${file.upload-path}")
    private String uploadPath;

    @PostMapping("/report")
    public R<?> report(@RequestBody ReportRequest request) {
        String buildingPipeUrl = request.getBuildingPipeUrl();
        String outboundPipeUrl = request.getOutboundPipeUrl();
        String confirmFileUrl = request.getConfirmFileUrl();

        log.info("开始分析CAD图纸, 楼栋文件地址: {}", buildingPipeUrl);
        log.info("开始分析CAD图纸, 出地管文件地址: {}", outboundPipeUrl);
        log.info("开始分析CAD图纸, 工程量确认单文件地址: {}", confirmFileUrl);

        // 下载文件
        File buildingPipeFile = null;
        File outboundPipeFile = null;
        File confirmFile = null;
        try {
            // 1. 下载 CAD 文件
            buildingPipeFile = FileUtils.downloadToTempFileEnhance(buildingPipeUrl);
            outboundPipeFile = FileUtils.downloadToTempFileEnhance(outboundPipeUrl);
            confirmFile = FileUtils.downloadToTempFileEnhance(confirmFileUrl);
            if (buildingPipeFile == null || outboundPipeFile == null || confirmFile == null) {
                return R.error("CAD 文件下载失败");
            }

            String buildingPipeFileAbsPath = buildingPipeFile.getAbsolutePath();
            String outboundPipeFileAbsPath = outboundPipeFile.getAbsolutePath();
            String confirmFileAbsPath = confirmFile.getAbsolutePath();

            ReportResp resp = doReport(buildingPipeFileAbsPath, outboundPipeFileAbsPath, confirmFileAbsPath);

            log.info("完成CAD图纸识别，返回结果: {}", resp);
            return R.success(resp);
        } catch (Exception e) {
            log.error("CAD图纸智能分析失败", e);
            return R.error("CAD图纸智能分析失败：" + e.getMessage());
        } finally {
            // 清理临时文件
            FileUtils.deleteFile(buildingPipeFile);
            FileUtils.deleteFile(outboundPipeFile);
            FileUtils.deleteFile(confirmFile);
            log.info("删除CAD临时文件");
        }
    }

    private ReportResp doReport(String buildingPipeFileAbsPath, String outboundPipeFileAbsPath, String confirmFileAbsPath) {
        // 2. 解析出地管数据
        List<CadItem> outboundPipeData = new OutboundPipeAnalyzer().executeAnalysis(outboundPipeFileAbsPath);

        // 3. 解析建筑管道数据
        List<CadItem> buildingPipeData = new BuildingPipeAnalyzer().executeAnalysis(buildingPipeFileAbsPath);

        // 7. 合并工程量确认单
        ConfirmFileDataList confirmFileDataList = new ConfirmFileAnalyzer().executeAnalysis(confirmFileAbsPath);
        MergeResultDataList merge = MergeDelegate.merge(buildingPipeData, outboundPipeData, confirmFileDataList);
        File file = MergeDelegate.generateExcel(uploadPath, merge);
        ReportResp reportResp = new ReportResp();
        reportResp.setReportFileUrl(FileUtils.getUrl(file));
        reportResp.setReportDetail(merge.adapter());
        return reportResp;
    }

    public static void main(String[] args) {
        ReportController controller = new ReportController();
        controller.uploadPath = "D:/upload/";
        String buildingPipeFileAbsPath = "C:\\Users\\Liming\\Desktop\\1津都雅苑-户数(1).dxf";
        String outboundPipeFileAbsPath = "C:\\Users\\Liming\\Desktop\\1津都雅苑-出地管道.dxf";
        String confirmFileAbsPath = "C:\\Users\\Liming\\Desktop\\完工确认单(1).xlsx";
        controller.doReport(buildingPipeFileAbsPath, outboundPipeFileAbsPath, confirmFileAbsPath);
    }
}