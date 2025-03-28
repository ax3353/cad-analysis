package com.jinghu.cad.analysis.service.impl;

import com.jinghu.cad.analysis.analyzer.BuildingPipeAnalyzer;
import com.jinghu.cad.analysis.analyzer.ConfirmFileAnalyzer;
import com.jinghu.cad.analysis.analyzer.OutboundPipeAnalyzer;
import com.jinghu.cad.analysis.delegate.MergeDelegate;
import com.jinghu.cad.analysis.dto.CadItem;
import com.jinghu.cad.analysis.excel.ConfirmFileDataList;
import com.jinghu.cad.analysis.excel.MergeResultDataList;
import com.jinghu.cad.analysis.service.IReportService;
import com.jinghu.cad.analysis.utils.FileUtils;
import com.jinghu.cad.analysis.vo.resp.ReportResp;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;

/**
 * @author liming
 * @version 1.0
 * @description ReportServiceImpl
 * @date 2025/3/28 16:35
 */
@Service
public class ReportServiceImpl implements IReportService {

    @Value("${file.upload-path}")
    private String uploadPath;

    @Override
    public ReportResp doReport(String buildingPipeFileAbsPath, String outboundPipeFileAbsPath, String confirmFileAbsPath) {
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
}
