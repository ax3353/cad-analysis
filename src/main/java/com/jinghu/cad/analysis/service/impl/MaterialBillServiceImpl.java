package com.jinghu.cad.analysis.service.impl;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jinghu.cad.analysis.enmus.PipeDiameter;
import com.jinghu.cad.analysis.entity.MaterialBill;
import com.jinghu.cad.analysis.excel.MaterialBill1Data;
import com.jinghu.cad.analysis.mapper.MaterialBillMapper;
import com.jinghu.cad.analysis.service.IMaterialBillService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * <p>
 * 物料清单 服务实现类
 * </p>
 *
 * @author liming
 * @since 2025-03-27
 */
@Service
public class MaterialBillServiceImpl extends ServiceImpl<MaterialBillMapper, MaterialBill> implements IMaterialBillService {

    private void calc(MaterialBill bill) {
        String[] split = bill.getMaterialName().split("_");
        if (split.length > 1) {
            bill.setMaterialExt1(split[0]);
        }
        if (split.length > 2) {
            //有可能是材质
            String string1 = split[1];
            if (string1.startsWith("D") || string1.startsWith("d")) {
                bill.setMaterialSpec(string1);
            } else {
                if (split.length > 3) {
                    bill.setMaterialExt2(string1);
                    String string2 = split[2];
                    if (string2.startsWith("D") || string2.startsWith("d")) {
                        bill.setMaterialSpec(string2);
                    }
                }
            }
        }
        if (StringUtils.isNotBlank(bill.getMaterialSpec())) {
            bill.setMaterialNominalSpec(PipeDiameter.getPipeDiameterStr(bill.getMaterialSpec()));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void upload(MultipartFile file) throws IOException {
        List<MaterialBill1Data> list = EasyExcel.read(file.getInputStream()).head(MaterialBill1Data.class).sheet("库存红线设置 (2024年12月)").doReadSync();
        this.remove(null);
        this.saveBatch(list.stream().map(item -> {
            MaterialBill materialBill = new MaterialBill();
            materialBill.setMaterialCode(item.getMaterialCode().replace("\"", "").trim());
            materialBill.setMaterialName(item.getMaterialSpec());
            materialBill.setMaterialType(item.getMaterialType());
            calc(materialBill);
            return materialBill;
        }).collect(Collectors.toList()));
    }

    private static final Pattern PE_REGEX = Pattern.compile("(PE\\d+)(管材)?(SDR\\d+)(dn\\d+)");

    @Override
    public String getMaterialCode(String name) {
        if (name.contains(" ")) {
            String[] split = name.split(" ");
            if (split.length > 1) {
                String materialName = split[0];
                String materialSpec = split[1];
                if (materialName.equals("焊接钢管")) {
                    materialName = "镀锌钢管";
                }
                if (materialName.equals("热镀锌钢管")) {
                    materialName = "镀锌钢管";
                }
                if (StringUtils.isNotBlank(materialSpec)) {
                    materialSpec = PipeDiameter.getPipeDiameterStr(materialSpec);
                }
                List<MaterialBill> list = this.lambdaQuery().eq(MaterialBill::getMaterialType, materialName).eq(MaterialBill::getMaterialNominalSpec, materialSpec).list();
                return list.stream().map(MaterialBill::getMaterialCode).collect(Collectors.joining(","));
            }
            return -1 + "";
        } else {
            Matcher matcher = PE_REGEX.matcher(name);
            if (matcher.find()) {
                String group1 = matcher.group(1);
                String group2 = matcher.group(2);
                String group3 = matcher.group(3);
                String group4 = matcher.group(4);
                String materialType = "PE";
                if (StringUtils.isNotBlank(group2)) {
                    materialType = materialType + group2;
                }
                List<MaterialBill> list = this.lambdaQuery().eq(MaterialBill::getMaterialType, materialType).likeRight(MaterialBill::getMaterialExt1, group1).eq(MaterialBill::getMaterialExt2, group3).eq(MaterialBill::getMaterialSpec, group4).list();
                return list.stream().map(MaterialBill::getMaterialCode).collect(Collectors.joining(","));
            }
            return -1 + "";
        }
    }

}
