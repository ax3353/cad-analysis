package com.jinghu.cad.analysis.task;

/**
 * @author liming
 * @version 1.0
 * @description DeleteTemporaryFileTask
 * @date 2025/4/2 9:27
 */

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Objects;

@Component
public class DeleteTemporaryFileTask {

    @Value("${file.upload-path}")
    private String uploadPath;

    @Scheduled(fixedRate = 3600000) // 每小时执行一次
    public void deleteTemporaryFiles() {
        File tempDir = new File(uploadPath);
        if (tempDir.exists() && tempDir.isDirectory()) {
            for (File file : Objects.requireNonNull(tempDir.listFiles())) {
                if (file.isFile() && isOldFile(file)) {
                    file.delete();
                }
            }
        }
    }

    private boolean isOldFile(File file) {
        long lastModified = file.lastModified();
        long threshold = System.currentTimeMillis() - (24 * 60 * 60 * 1000); // 24小时之前的文件
        return lastModified < threshold;
    }
}
