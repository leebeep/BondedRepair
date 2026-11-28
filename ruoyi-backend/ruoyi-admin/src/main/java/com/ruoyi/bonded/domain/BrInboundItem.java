package com.ruoyi.bonded.domain;

import java.math.BigDecimal;
import lombok.Data;

/**
 * 入库单明细 br_inbound_item
 */
@Data
public class BrInboundItem
{
    private Long id;
    private Long inboundId;
    private Long materialId;
    /** 物料编号(关联查询) */
    private String materialCd;
    /** 物料名称(关联查询) */
    private String materialName;
    /** 计量单位(关联查询，展示用) */
    private String calcUnit;
    /** 法定计量单位1(关联查询，展示用) */
    private String unit1;
    /** 法定计量单位2(关联查询，展示用) */
    private String unit2;
    /** 冗余展示字段：来自物料档案（单据页面拼接六类标记名称用） */
    private String markType;
    private String isBonded;
    private BigDecimal storageCnt;
    private BigDecimal price;
    private String currency;
    private BigDecimal unit1Num;
    private BigDecimal unit2Num;
    private String brandType;
    private String tradeModeCd;
    private String arrivingCountryCode;
    private String endCountryCode;
    private String specificationsModels;
    /** g-入库清单 i-工单收料 h-出库清单 空-国内采购 */
    private String goodsSourceType;
    private String goodsSourceNo;
    private String busiDesc;
    private String busiFormNum;
}
