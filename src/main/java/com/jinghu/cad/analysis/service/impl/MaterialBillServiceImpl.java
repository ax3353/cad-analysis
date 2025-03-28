package com.jinghu.cad.analysis.service.impl;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jinghu.cad.analysis.enmus.PipeDiameter;
import com.jinghu.cad.analysis.entity.MaterialBill;
import com.jinghu.cad.analysis.excel.MaterialBill1Data;
import com.jinghu.cad.analysis.mapper.MaterialBillMapper;
import com.jinghu.cad.analysis.service.IMaterialBillService;
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
    /**
     * 库存红线设置 (2024年12月) 正则
     */
    private static final Pattern PIPE_REGEX = Pattern.compile("(热镀锌钢管|无缝钢管|喷塑钢管|螺旋钢管)_([^_]+)_(D[^_]+).*");
    private static final Pattern FERRULE_REGEX = Pattern.compile("(钢制法兰球阀|焊接连接闸阀|止回阀|钢制针形截止阀).*(D[^_]+).*");
    private static final Pattern METAL_HOSE_REGEX = Pattern.compile("(工业金属软管).*(D[^_]+).*");

//    public static void main(String[] args) {
//        String a = "工业金属软管_DN25_PN10_500_法兰_公制";
//        Matcher matcher = METAL_HOSE_REGEX.matcher(a);
//        while (matcher.find()) {
//            System.out.println(matcher.group(1));
//            System.out.println(matcher.group(2));
//        }
//    }

    /**
     * 暂定：防腐漆(防腐漆_喷塑钢管补口_底漆环氧粉末+面漆聚酯粉末)
     *
     * @param bill
     */
    private void calc(MaterialBill bill) {
        String materialName = bill.getMaterialName();

        Matcher pipeMatcher = PIPE_REGEX.matcher(materialName);
        if (pipeMatcher.find()) {
            bill.setMaterialExt1(pipeMatcher.group(1));
            bill.setMaterialExt2(pipeMatcher.group(2));
            bill.setMaterialSpec(pipeMatcher.group(3));
            bill.setMaterialNominalSpec(PipeDiameter.getPipeDiameterStr(bill.getMaterialSpec()));
            return;
        }

        Matcher ferruleMatcher = FERRULE_REGEX.matcher(materialName);
        if (ferruleMatcher.find()) {
            bill.setMaterialExt1(ferruleMatcher.group(1));
            bill.setMaterialSpec(ferruleMatcher.group(2));
            bill.setMaterialNominalSpec(PipeDiameter.getPipeDiameterStr(bill.getMaterialSpec()));
            return;
        }

        Matcher metalHoseMatcher = METAL_HOSE_REGEX.matcher(materialName);
        if (metalHoseMatcher.find()) {
            bill.setMaterialExt1(metalHoseMatcher.group(1));
            bill.setMaterialSpec(metalHoseMatcher.group(2));
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

    /**
     * 工程量确认单正则
     */
    private static final Pattern PIPE_REGEX_1 = Pattern.compile("(喷塑直缝钢管|喷塑钢管|镀锌钢管).*(DN?\\d+(?:\\.\\d+)?).*");
    private static final Pattern PIPE_PE_REGEX_1 = Pattern.compile("(PE\\d+)");

    public static void main(String[] args) {
        String a = "喷塑钢管 DN65 (3.6米以内)\u007F【标高3.6~10m】";
        Matcher matcher = PIPE_REGEX_1.matcher(a);
        while (matcher.find()) {
            System.out.println(matcher.group(1));
            System.out.println(matcher.group(2));
        }
    }

    @Override
    public String getMaterialCode(String name) {
        Matcher matcher = PIPE_REGEX_1.matcher(name);
        if (matcher.find()) {
            String materialName = matcher.group(1);
            if ("喷塑钢管".equals(materialName)) {
                materialName = "喷塑直缝钢管";
            }
            String materialSpec = matcher.group(2);
            String materialNominalSpec = PipeDiameter.getPipeDiameterStr(materialSpec);
            List<MaterialBill> list = this.lambdaQuery().eq(MaterialBill::getMaterialType, materialName).eq(MaterialBill::getMaterialNominalSpec, materialNominalSpec).list();
            return list.stream().map(MaterialBill::getMaterialCode).collect(Collectors.joining(","));
        }
        return -1 + "";
    }

}
