package com.jinghu.cad.analysis.vo.resp;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author liming
 * @version 1.0
 * @description UploadResp
 * @date 2025/3/24 11:15
 */
@Data
@AllArgsConstructor
public class UploadResp {
    private String fileName;  // 原始文件名
    private String filePath;  // 存储路径
    private long fileSize;    // 文件大小（字节）
    private String fileUrl;   // 访问 URL
}
