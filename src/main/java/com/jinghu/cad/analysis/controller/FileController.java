package com.jinghu.cad.analysis.controller;

import com.jinghu.cad.analysis.vo.resp.R;
import com.jinghu.cad.analysis.vo.resp.UploadResp;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

/**
 * @author liming
 * @version 1.0
 * @description FileController
 * @date 2025/3/24 11:25
 */
@RestController
@CrossOrigin(origins = "*")
public class FileController {

    @Value("${file.upload-path}")
    private String uploadPath;

    // 上传文件接口
    @PostMapping("/upload")
    public R<UploadResp> upload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return R.error("上传文件不能为空");
        }

        // 获取原始文件名
        String originalFileName = file.getOriginalFilename();

        // 生成唯一的文件名
        String timestamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        String fileName = timestamp + "_" + UUID.randomUUID() + "_" + originalFileName;
        String uniqueFileNameUrl = "/files/" + fileName;

        // 构建目标文件路径
        File dest = Paths.get(uploadPath, "files", fileName).toFile();

        // 确保文件目录存在，如果不存在则创建
        if (!dest.getParentFile().exists()) {
            dest.getParentFile().mkdirs(); // 创建目录
        }

        try {
            // 保存文件到目标路径
            file.transferTo(dest);
            System.out.println("文件上传成功，文件存储路径：" + dest.getAbsolutePath());
            System.out.println("文件访问路径（URL）：" + "http://localhost:8800" + uniqueFileNameUrl);

            // 构建文件响应对象，返回可访问的下载链接
            String fileUrl = uniqueFileNameUrl; // 指向静态资源的 URL
            UploadResp resp = new UploadResp(
                    fileName,
                    dest.getAbsolutePath(),
                    file.getSize(),
                    fileUrl
            );

            return R.success("文件上传成功", resp);
        } catch (IOException e) {
            e.printStackTrace();
            return R.error("文件上传失败：" + e.getMessage());
        }
    }

    // 删除文件接口
    @PostMapping("/delete")
    public R<String> delete(@RequestParam("fileName") String fileName) {
        // 构建文件路径
        File file = Paths.get(uploadPath, "files", fileName).toFile();

        // 检查文件是否存在
        if (!file.exists()) {
            return R.error("文件不存在");
        }

        // 删除文件
        if (file.delete()) {
            return R.success("文件删除成功");
        } else {
            return R.error("文件删除失败");
        }
    }


}
