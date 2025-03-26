package com.jinghu.cad.analysis.utils;

/**
 * @author liming
 * @version 1.0
 * @description FileUtils
 * @date 2025/3/26 10:47
 */
public class FileUtils {
    public static String removeFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf(".");
        if (lastDotIndex > 0) { // 确保文件名不以 "." 开头
            return fileName.substring(0, lastDotIndex);
        }
        return fileName; // 没有后缀时返回原文件名
    }
}
