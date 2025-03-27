package com.jinghu.cad.analysis.vo.req;

import lombok.Data;

@Data
public class ReportRequest {

    /**
     * cad楼栋文件
     */
    private String buildingPipeUrl;

    /**
     * cad出地管文件
     */
    private String outboundPipeUrl;

    /**
     * 工程量确认单文件
     */
    private String confirmFileUrl;
}