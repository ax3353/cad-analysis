package com.jinghu.cad.analysis.service;

import com.jinghu.cad.analysis.vo.resp.ReportResp;

public interface IReportService {
    ReportResp doReport(String buildingPipeFileAbsPath, String outboundPipeFileAbsPath, String confirmFileAbsPath);
}
