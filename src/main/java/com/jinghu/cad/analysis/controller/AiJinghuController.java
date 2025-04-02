package com.jinghu.cad.analysis.controller;

import com.jinghu.cad.analysis.excel.ExportUtils;
import com.jinghu.cad.analysis.service.IAiJinghuService;
import com.jinghu.cad.analysis.utils.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * @author liming
 * @version 1.0
 * @description AiJinghuController
 * @date 2025/3/28 19:34
 */
@RestController
public class AiJinghuController {

    @Resource
    private IAiJinghuService iAiJinghuService;

    @Value("${file.upload-path}")
    private String uploadPath;

    @PostMapping("/difySqlQuery")
    public List<Map<Object,Object>> difySqlQuery(String querySql) throws Exception {
        querySql=querySql.replace("```sql","");
        querySql=querySql.replace("```","");
        List<Map<Object, Object>> maps = iAiJinghuService.difySqlQuery(querySql);
        return maps;
    }


    @PostMapping("/excelExport")
    public String difySqlQuery(String querySql,String fileName){
        fileName = fileName + System.currentTimeMillis()+ ".xlsx";
        querySql=querySql.replace("```sql","");
        querySql=querySql.replace("```","");
        List<Map<Object, Object>> maps = iAiJinghuService.difySqlQuery(querySql);
        String profix="/excel/";
        File outputDirectory = new File(uploadPath+profix);
        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs(); // 创建目录，如果不存在
        }
        // Windows 路径
        String filePath = outputDirectory.getAbsolutePath();
        filePath = filePath + "\\" + fileName;
        ExportUtils.exportToExcel(maps, filePath);

        String urlByName = FileUtils.getUrlByName(profix+fileName);
        return urlByName;
    }


}
