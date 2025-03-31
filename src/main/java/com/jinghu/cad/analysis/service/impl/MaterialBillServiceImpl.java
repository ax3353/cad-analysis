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
    private static final Pattern VALVE_REGEX = Pattern.compile("(钢制法兰球阀|焊接连接闸阀|止回阀|钢制针形截止阀).*(D[^_]+).*");
    private static final Pattern METAL_HOSE_REGEX = Pattern.compile("(工业金属软管).*(D[^_]+).*");
    //PE100燃气管_SDR17_dn200_11.9mm_黑色管材_橙色色条_GB 15558.1_直管
    private static final Pattern PIPE_PE_REGEX = Pattern.compile("^(PE\\d+燃气管)_([^_]+)_([^_]+).*");
    //调压箱_RX100_0.1-0.4Mpa_2-30Kpa_100Nm3/h_DN50_DN50_天然气_不锈钢
    private static final Pattern REGULATOR_BOX_REGEX = Pattern.compile("^(调压箱)_([^_]+).*");
    //PE球阀_PE100_dn40_SDR11_手轮、手柄、扳手_无放散_标准高度
    private static final Pattern PE_VALVE_REGEX = Pattern.compile("^PE球阀_(PE\\d+)_([^_]+)_([^_]+).*");
    //示踪线_铜包钢_2.5mm² 双线_500m/卷
    private static final Pattern TRACER_LINE_REGEX = Pattern.compile("示踪线_铜包钢_([^_]+)\\s.*");
    //钢纤维混凝土井盖_圆形_Ф200_B125_C30_12
    private static final Pattern MANHOLE_COVER_REGEX = Pattern.compile("^([^_]+井盖).*([^_]+形)_(\\D{1})(\\d+).*");
//    public static void main(String[] args) {
//        String a = "复合材料井盖_复合材料_圆形_Ф700_A15_7";
//        Matcher matcher = MANHOLE_COVER_REGEX.matcher(a);
//        while (matcher.find()) {
//            System.out.println(matcher.group(1));
//            System.out.println(matcher.group(2));
//            System.out.println(matcher.group(3));
//            System.out.println(matcher.group(3));
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

        Matcher valveMatcher = VALVE_REGEX.matcher(materialName);
        if (valveMatcher.find()) {
            bill.setMaterialExt1(valveMatcher.group(1));
            bill.setMaterialSpec(valveMatcher.group(2));
            bill.setMaterialNominalSpec(PipeDiameter.getPipeDiameterStr(bill.getMaterialSpec()));
            return;
        }

        Matcher metalHoseMatcher = METAL_HOSE_REGEX.matcher(materialName);
        if (metalHoseMatcher.find()) {
            bill.setMaterialExt1(metalHoseMatcher.group(1));
            bill.setMaterialSpec(metalHoseMatcher.group(2));
            bill.setMaterialNominalSpec(PipeDiameter.getPipeDiameterStr(bill.getMaterialSpec()));
        }

        Matcher pipePEMatcher = PIPE_PE_REGEX.matcher(materialName);
        if (pipePEMatcher.find()) {
            String materialExt1 = pipePEMatcher.group(1);
            materialExt1 = materialExt1.replace("燃气管", "");
            bill.setMaterialExt1(materialExt1);
            bill.setMaterialExt2(pipePEMatcher.group(2));
            bill.setMaterialSpec(pipePEMatcher.group(3));
            bill.setMaterialNominalSpec(PipeDiameter.getPipeDiameterStr(bill.getMaterialSpec()));
        }

        Matcher regulatorBoxMatcher = REGULATOR_BOX_REGEX.matcher(materialName);
        if (regulatorBoxMatcher.find()) {
            bill.setMaterialExt1(regulatorBoxMatcher.group(1));
            bill.setMaterialSpec(regulatorBoxMatcher.group(2));
            bill.setMaterialNominalSpec(PipeDiameter.getPipeDiameterStr(bill.getMaterialSpec()));
        }

        Matcher peValveMatcher = PE_VALVE_REGEX.matcher(materialName);
        if (peValveMatcher.find()) {
            bill.setMaterialExt1(peValveMatcher.group(1));//PE100 //dn40 //SDR11
            bill.setMaterialExt2(peValveMatcher.group(3));
            bill.setMaterialSpec(peValveMatcher.group(2));
            bill.setMaterialNominalSpec(PipeDiameter.getPipeDiameterStr(bill.getMaterialSpec()));
        }

        Matcher tracerLineMatcher = TRACER_LINE_REGEX.matcher(materialName);
        if (tracerLineMatcher.find()) {
            bill.setMaterialSpec(tracerLineMatcher.group(1));
            bill.setMaterialNominalSpec(PipeDiameter.getPipeDiameterStr(bill.getMaterialSpec()));
        }

        Matcher manholeCoverMatcher = MANHOLE_COVER_REGEX.matcher(materialName);
        if (manholeCoverMatcher.find()) {
            bill.setMaterialExt1(manholeCoverMatcher.group(1));
            bill.setMaterialExt2(manholeCoverMatcher.group(2));
            bill.setMaterialSpec(manholeCoverMatcher.group(3) + manholeCoverMatcher.group(4));
            bill.setMaterialNominalSpec(manholeCoverMatcher.group(4));
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
    //PE100 管材 SDR11 dn40
    private static final Pattern PIPE_PE_REGEX_1 = Pattern.compile("(PE\\d+)\\s*(管材)\\s*(SDR\\d+)\\s*(dn\\d+(?:\\.\\d+)?)");
    //调压箱 RX200
    private static final Pattern REGULATOR_BOX_REGEX_1 = Pattern.compile("^(调压箱)\\s*(RX\\d+)");
    //PE球阀 dn63
    private static final Pattern PE_VALVE_REGEX_1 = Pattern.compile("^(PE球阀)\\s*(dn\\d+(?:\\.\\d+)?)");
    //示踪线 2.5mm²
    private static final Pattern TRACER_LINE_REGEX_1 = Pattern.compile("^(示踪线)\\s*(\\S+)");
    //钢纤维混凝土井盖_圆形 Ф320
    private static final Pattern MANHOLE_COVER_REGEX_1 = Pattern.compile("^([^_]+井盖)_([^_]+形)\\s+(\\D{1})(\\d+)");

    public static void main(String[] args) {
        String a = "钢纤维混凝土井盖_圆形 Ф320";
        Matcher matcher = MANHOLE_COVER_REGEX_1.matcher(a);
        while (matcher.find()) {
            System.out.println(matcher.group(1));
            System.out.println(matcher.group(2));
            System.out.println(matcher.group(3));
            System.out.println(matcher.group(4));
        }
    }

    @Override
    public String getMaterialCode(String name) {
        try {
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
            matcher = PIPE_PE_REGEX_1.matcher(name);
            if (matcher.find()) {
                String materialExt1 = matcher.group(1);
                String materialName = matcher.group(2);
                if ("管材".equals(materialName)) {
                    materialName = "PE管材";
                }
                String materialExt2 = matcher.group(3);
                String materialSpec = matcher.group(4);
                String materialNominalSpec = PipeDiameter.getPipeDiameterStr(materialSpec);
                List<MaterialBill> list = this.lambdaQuery().eq(MaterialBill::getMaterialType, materialName).eq(MaterialBill::getMaterialExt1, materialExt1).eq(MaterialBill::getMaterialExt2, materialExt2).eq(MaterialBill::getMaterialNominalSpec, materialNominalSpec).list();
                return list.stream().map(MaterialBill::getMaterialCode).collect(Collectors.joining(","));
            }
            matcher = REGULATOR_BOX_REGEX_1.matcher(name);
            if (matcher.find()) {
                String materialName = matcher.group(1);
                String materialSpec = matcher.group(2);
                List<MaterialBill> list = this.lambdaQuery().eq(MaterialBill::getMaterialExt1, materialName).eq(MaterialBill::getMaterialNominalSpec, materialSpec).list();
                return list.stream().map(MaterialBill::getMaterialCode).collect(Collectors.joining(","));
            }
            matcher = PE_VALVE_REGEX_1.matcher(name);
            if (matcher.find()) {
                String materialName = matcher.group(1);
                String materialSpec = matcher.group(2);
                List<MaterialBill> list = this.lambdaQuery().eq(MaterialBill::getMaterialType, materialName).eq(MaterialBill::getMaterialNominalSpec, materialSpec).list();
                return list.stream().map(MaterialBill::getMaterialCode).collect(Collectors.joining(","));
            }
            matcher = TRACER_LINE_REGEX_1.matcher(name);
            if (matcher.find()) {
                String materialType = matcher.group(1);
                String materialSpec = matcher.group(2);
                List<MaterialBill> list = this.lambdaQuery().eq(MaterialBill::getMaterialType, materialType).eq(MaterialBill::getMaterialNominalSpec, materialSpec).list();
                return list.stream().map(MaterialBill::getMaterialCode).collect(Collectors.joining(","));
            }
            matcher = MANHOLE_COVER_REGEX_1.matcher(name);
            if (matcher.find()) {
                String materialExt1 = matcher.group(1);
                String materialSpec = matcher.group(4);
                List<MaterialBill> list = this.lambdaQuery().eq(MaterialBill::getMaterialExt1, materialExt1).eq(MaterialBill::getMaterialNominalSpec, materialSpec).list();
                return list.stream().map(MaterialBill::getMaterialCode).collect(Collectors.joining(","));
            }
        } catch (Exception e) {
            log.error("获取材料编码失败", e);
        }
        return -1 + "";
    }

}
