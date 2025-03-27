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


    //钢制弯头
    //钢制三通
    //钢制异径管
    //金属法兰
    //特殊钢制管件
    //钢制弯管
    //工业金属软管
    //PE100燃气管
    //电熔套筒
    //电熔变径
    //热熔变径
    //电熔三通
    //热熔三通
    //电熔弯头
    //热熔弯头
    private static final Pattern PIPE_REGEX = Pattern.compile("(热镀锌钢管|无缝钢管|喷塑钢管|螺旋钢管)_([^_]+)_(D[^_]+).*");
    private static final Pattern FERRULE_REGEX = Pattern.compile("(钢制法兰球阀|焊接连接闸阀|止回阀|钢制针形截止阀).*(D[^_]+).*");

    public static void main(String[] args) {
        String a = "钢制法兰球阀_手轮、手柄、扳手_法兰连接_浮动式_氟塑料_PN16_WCB_DN15_无放散";
        Matcher matcher = FERRULE_REGEX.matcher(a);
        while (matcher.find()) {
            System.out.println(matcher.group(1));
            System.out.println(matcher.group(2));
        }
    }

    /**
     * 暂定：防腐漆(防腐漆_喷塑钢管补口_底漆环氧粉末+面漆聚酯粉末)
     *
     * @param bill
     */
    private void calc(MaterialBill bill) {
        Matcher pipeMatcher = PIPE_REGEX.matcher(bill.getMaterialName());
        if (pipeMatcher.find()) {
            bill.setMaterialExt1(pipeMatcher.group(1));
            bill.setMaterialExt2(pipeMatcher.group(2));
            bill.setMaterialSpec(pipeMatcher.group(3));
            bill.setMaterialNominalSpec(PipeDiameter.getPipeDiameterStr(bill.getMaterialSpec()));
        } else {
            Matcher ferruleMatcher = FERRULE_REGEX.matcher(bill.getMaterialName());
            if (ferruleMatcher.find()) {
                bill.setMaterialExt1(ferruleMatcher.group(1));
                bill.setMaterialSpec(ferruleMatcher.group(2)); // 修正错误
                bill.setMaterialNominalSpec(PipeDiameter.getPipeDiameterStr(bill.getMaterialSpec()));
            }
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
