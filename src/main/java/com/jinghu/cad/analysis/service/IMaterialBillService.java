package com.jinghu.cad.analysis.service;

import com.jinghu.cad.analysis.entity.MaterialBill;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * <p>
 * 物料清单 服务类
 * </p>
 *
 * @author liming
 * @since 2025-03-27
 */
public interface IMaterialBillService extends IService<MaterialBill> {

    void upload(MultipartFile file) throws IOException;

    String getMaterialCode(String name);
}
