package com.jinghu.cad.analysis.req;

import lombok.Data;

@Data
public class ReportRequest {

    /**
     * cad文件
     */
    private String cad_files_url;

    /**
     * 补充文件
     */
    private String supplement_file_url;

    /**
     * 核算单文件
     */
    private String confirm_file_url;
}