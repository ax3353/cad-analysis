package com.jinghu.cad.analysis.entity;

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

    private Integer id;

    /**
     * 物料类型
     */
    private String materialType;

    /**
     * 物料编码
     */
    private String materialCode;

    /**
     * 物流规格
     */
    private String materialSpec;
}
