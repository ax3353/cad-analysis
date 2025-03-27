package com.jinghu.cad.analysis.vo.resp;

import com.jinghu.cad.analysis.excel.MergeResultData;
import lombok.Data;

import java.util.List;

/**
 * @author liming
 * @version 1.0
 * @description ReportResp
 * @date 2025/3/26 14:54
 */
@Data
public class ReportResp {
    private String reportFileUrl;
    private List<MergeResultData> reportDetail;
}
