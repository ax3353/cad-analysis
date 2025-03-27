package com.jinghu.cad.analysis.controller;

import com.alibaba.excel.EasyExcel;
import com.jinghu.cad.analysis.enmus.PipeDiameter;
import com.jinghu.cad.analysis.entity.MaterialBill;
import com.jinghu.cad.analysis.excel.MaterialBill1Data;
import com.jinghu.cad.analysis.service.IMaterialBillService;
import com.jinghu.cad.analysis.vo.resp.R;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 物料清单 前端控制器
 * </p>
 *
 * @author liming
 * @since 2025-03-27
 */
@Controller
@RequestMapping("/materialBill")
public class MaterialBillController {

    @Resource
    private IMaterialBillService iMaterialBillService;

    @RequestMapping("/upload")
    public R<?> upload(MultipartFile file) throws IOException {
        iMaterialBillService.upload(file);
        return R.success("upload success");
    }
}
