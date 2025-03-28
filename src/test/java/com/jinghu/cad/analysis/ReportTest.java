package com.jinghu.cad.analysis;

import com.jinghu.cad.analysis.service.IReportService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

/**
 * @author liming
 * @version 1.0
 * @description ReportTest
 * @date 2025/3/28 16:29
 */
@SpringBootTest
public class ReportTest {

    @Resource
    private IReportService iReportService;

    @Test
    public void doReport() throws Exception {
//        String buildingPipeFileAbsPath = "C:\\Users\\Liming\\Desktop\\识别文件\\津都雅苑1\\津都雅苑-户数.dxf";
//        String outboundPipeFileAbsPath = "C:\\Users\\Liming\\Desktop\\识别文件\\津都雅苑1\\津都雅苑-出地管道.dxf";
//        String confirmFileAbsPath = "C:\\Users\\Liming\\Desktop\\识别文件\\津都雅苑1\\津都雅苑-工程量确认单.xlsx";

        String buildingPipeFileAbsPath = "C:\\Users\\Liming\\Desktop\\识别文件\\建友爱路生活区1\\建友爱路生活区-户数.dxf";
        String outboundPipeFileAbsPath = "C:\\Users\\Liming\\Desktop\\识别文件\\建友爱路生活区1\\建友爱路生活区-出地管道.dxf";
        String confirmFileAbsPath = "C:\\Users\\Liming\\Desktop\\识别文件\\建友爱路生活区1\\建友爱路生活区-工程量确认单.xlsx";

//        String buildingPipeFileAbsPath = "C:\\Users\\Liming\\Desktop\\识别文件\\江湾明珠1\\江湾明珠-户数.dxf";
//        String outboundPipeFileAbsPath = "C:\\Users\\Liming\\Desktop\\识别文件\\江湾明珠1\\江湾明珠-出地管道.dxf";
//        String confirmFileAbsPath = "C:\\Users\\Liming\\Desktop\\识别文件\\江湾明珠1\\江湾明珠-工程量确认单.xlsx";
        iReportService.doReport(buildingPipeFileAbsPath, outboundPipeFileAbsPath, confirmFileAbsPath);
    }
}
