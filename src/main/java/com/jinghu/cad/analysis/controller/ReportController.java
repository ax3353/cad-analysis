package com.jinghu.cad.analysis.controller;

import com.jinghu.cad.analysis.analyzer.BuildingPipeAnalyzer;
import com.jinghu.cad.analysis.analyzer.ConfirmFileAnalyzer;
import com.jinghu.cad.analysis.analyzer.OutboundPipeAnalyzer;
import com.jinghu.cad.analysis.delegate.MergeDelegate;
import com.jinghu.cad.analysis.dto.CadItem;
import com.jinghu.cad.analysis.excel.ConfirmFileDataList;
import com.jinghu.cad.analysis.excel.MergeResultDataList;
import com.jinghu.cad.analysis.service.IReportService;
import com.jinghu.cad.analysis.utils.FileUtils;
import com.jinghu.cad.analysis.vo.req.ReportRequest;
import com.jinghu.cad.analysis.vo.resp.R;
import com.jinghu.cad.analysis.vo.resp.ReportResp;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.io.File;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/")
public class ReportController {

    @Resource
    private IReportService iReportService;

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

            ReportResp resp = iReportService.doReport(buildingPipeFileAbsPath, outboundPipeFileAbsPath, confirmFileAbsPath);

            log.info("完成CAD图纸识别，返回结果: {}", resp);
            return R.success(resp);
        } catch (Exception e) {
            log.error("CAD图纸智能分析失败", e);
            return R.error("CAD图纸智能分析失败：" + e.getMessage());
        } finally {
            // 清理临时文件
//            FileUtils.deleteFile(buildingPipeFile);
//            FileUtils.deleteFile(outboundPipeFile);
//            FileUtils.deleteFile(confirmFile);
            log.info("删除CAD临时文件");
        }
    }

}