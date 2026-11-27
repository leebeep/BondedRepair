package com.ruoyi.bonded.domain;

import java.math.BigDecimal;
import java.util.Date;
import lombok.Data;

/**
 * 物料库存余额 br_material_stock（物料为唯一维度，唯一库存事实）
 */
@Data
public class BrMaterialStock
{
    private Long materialId;
    private String markType;
    private BigDecimal qty;
    private Date lastBizTime;
    private Date updateTime;

    /** 关联物料基本信息（联查 br_material，非本表字段） */
    private String materialCd;
    private String version;
    private String materialName;
    private String gModel;
    private String codeTs;
    private String calcUnit;
    private String isBonded;
    private String declareStatus;
}
