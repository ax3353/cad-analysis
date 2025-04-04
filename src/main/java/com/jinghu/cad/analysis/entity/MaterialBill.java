package com.jinghu.cad.analysis.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

/**
 * <p>
 * 物料清单
 * </p>
 *
 * @author liming
 * @since 2025-03-27
 */
@Getter
@Setter
@ToString
@TableName("material_bill")
public class MaterialBill implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 物料编码
     */
    private String materialCode;
    /**
     * 物料类型
     */
    private String materialType;
    /**
     * 物料扩展1
     */
    private String materialExt1;
    /**
     * 物料扩展2
     */
    private String materialExt2;
    /**
     * 物料名称
     */
    private String materialName;

    /**
     * 规格型号
     */
    private String materialSpec;

    /**
     * 公制型号
     */
    private String materialNominalSpec;
}
