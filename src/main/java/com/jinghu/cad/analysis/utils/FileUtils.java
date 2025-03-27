package com.jinghu.cad.analysis.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author liming
 * @version 1.0
 * @description FileUtils
 * @date 2025/3/26 10:47
 */
@Slf4j
public class FileUtils {

    public static String removeFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf(".");
        if (lastDotIndex > 0) { // 确保文件名不以 "." 开头
            return fileName.substring(0, lastDotIndex);
        }
        return fileName; // 没有后缀时返回原文件名
    }

    // 获取文件扩展名
    public static String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf(".");
        if (lastDotIndex > 0) { // 确保文件名不以 "." 开头
            return fileName.substring(lastDotIndex);
        }
        return ".tmp";
    }

    public static void openFile(String filePath) {
        File file = new File(filePath);
        if (file.exists()) {
            try {
                Desktop.getDesktop().open(file);
                System.out.println("文件已打开：" + filePath);
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("无法打开文件：" + filePath);
            }
        } else {
            System.out.println("文件不存在：" + filePath);
        }
    }

    public static String getUrl(File file) {
        String port = SpringUtils.getEnv("server.port", "8800");
        return "http://" + IpUtils.getHostIp().get(0) + ":" + port + "/files/" + file.getName();
    }

    public static File downloadToTempFileEnhance(String url) {
        if (!StringUtils.hasText(url)) {
            return null;
        }

        String fileName = url.substring(url.lastIndexOf("/"));
        File dest;
        try {
            dest = Paths.get(SpringUtils.getEnv("upload.path", "D:/upload"), "files", fileName).toFile();
            if (dest.exists()) {
                log.info("文件 {} 已存在跳过下载", dest.getAbsolutePath());
                return dest;
            } else {
                return downloadToTempFile(url);
            }
        } catch (Exception e) {
            log.error("下载文件失败", e);
            return null;
        }
    }

    private static File downloadToTempFile(String url) throws IOException {
        if (!StringUtils.hasText(url)) {
            return null;
        }
        String fileName = url.substring(url.lastIndexOf("/"));
        FileUtils.downloadFile(url, Paths.get(SpringUtils.getEnv("upload.path", "D:/upload"), "files", fileName));
        return Paths.get(SpringUtils.getEnv("upload.path", "D:/upload"), "files", fileName).toFile();
    }

    public static void downloadFile(String urlString, Path destination) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        // 设置超时
        connection.setConnectTimeout(5000); // 连接超时 5秒
        connection.setReadTimeout(5000);    // 读取超时 5秒

        // 设置请求方法
        connection.setRequestMethod("GET");

        // 检查 HTTP 响应码
        int responseCode = connection.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new IOException("Server returned HTTP code: " + responseCode);
        }

        // 获取文件大小
        int fileSize = connection.getContentLength();
        log.info("File size: " + fileSize + " bytes");

        try (InputStream in = connection.getInputStream();
             FileOutputStream out = new FileOutputStream(destination.toFile())) {

            byte[] buffer = new byte[4096];
            int bytesRead;
            long totalBytesRead = 0;

            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
                totalBytesRead += bytesRead;

                // 打印下载进度
                if (fileSize > 0) {
                    int progress = (int) (totalBytesRead * 100 / fileSize);
                    System.out.print("\rDownloading: " + progress + "%");
                }
            }
        } catch (IOException e) {
            log.error("Error downloading file", e);
            throw e;
        } finally {
            connection.disconnect();
        }

        log.info("Download completed: {}", destination);
    }

    public static void deleteFile(File file) {
        if (file != null && file.exists()) {
            if (file.delete()) {
                log.info("文件删除成功：{}", file.getAbsolutePath());
            } else {
                log.error("文件删除失败：{}", file.getAbsolutePath());
            }
        } else {
            log.info("文件不存在");
        }
    }
}
